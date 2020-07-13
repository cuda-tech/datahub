/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.cuda.datahub.scheduler.tracker

import kotlinx.coroutines.*
import org.apache.log4j.Logger
import tech.cuda.datahub.scheduler.listener.ClockListener
import tech.cuda.datahub.scheduler.listener.TrackerLifeCycleListener
import java.lang.AssertionError
import java.time.LocalDateTime

/**
 * Tracker 抽象类
 * @author Jensen Qi <jinxiu.qi@alu.hit.edu.cn>
 * @since 1.0.0
 */
abstract class Tracker : TrackerLifeCycleListener, ClockListener {
    protected val logger: Logger = Logger.getLogger(this::class.java)
    private val className: String get() = this.javaClass.simpleName
    private val heartbeat = 500L
    private lateinit var job: Deferred<Unit>
    private lateinit var datetimeSnapshot: LocalDateTime

    private fun dealWithHourChange(current: LocalDateTime) {
        if (current.hour != datetimeSnapshot.hour) {
            onHourChange()
            logger.info("hour change")
        }
    }

    private fun dealWithDateChange(current: LocalDateTime) {
        if (current.toLocalDate().isAfter(datetimeSnapshot.toLocalDate())) {
            onDateChange()
            logger.info("day change")
        }
    }

    private fun dealWithHeartBeat() = onHeartBeat()

    /**
     * 分数表达式
     */
    protected infix fun Int.over(v: Int) = this to v

    /**
     * 按 [batchSize] 分批执行，避免大型 SQL 查询
     * 其中 [block] 块需要返回当前批次执行的数量，以及总共需要执行的数量，e.g current over total
     * 后续可优化为并发执行
     */
    protected fun batchExecute(batchSize: Int = 100, block: (batch: Int, batchSize: Int) -> Pair<Int, Int>) {
        var batch = 0
        do {
            batch++
            val (currentBatchProcessed, total) = block(batch, batchSize)
        } while ((batch - 1) * batchSize + currentBatchProcessed < total)
    }

    fun start() {
        if (this::job.isInitialized) {
            logger.error("try to start $className duplicate")
            return
        }
        this.job = GlobalScope.async {
            try {
                onStarted()
                datetimeSnapshot = LocalDateTime.now()
                while (true) {
                    val current = LocalDateTime.now()
                    dealWithDateChange(current)
                    dealWithHourChange(current)
                    dealWithHeartBeat()
                    delay(heartbeat)
                    datetimeSnapshot = current
                    logger.debug("$className alive")
                }
            } catch (exception: Throwable) {
                when (exception) {
                    // 不捕获由协程取消引起的 CancellationException 和单测引起的 AssertionError
                    is CancellationException, is AssertionError -> throw exception
                    else -> {
                        logger.error(exception.message)
                        exception.printStackTrace()
                    }
                }
            } finally {
                onDestroyed()
            }
        }
        logger.info("$className started")
    }

    fun await() {
        if (this::job.isInitialized) {
            runBlocking {
                job.await()
            }
        } else {
            logger.error("try to join a not started $className")
        }
    }

    fun cancelAndAwait() {
        if (this::job.isInitialized) {
            try {
                runBlocking {
                    delay(1000)
                    job.cancel()
                    job.await()
                }
            } catch (e: CancellationException) {
                logger.info("cancel $className")
            }
        } else {
            logger.error("try to cancel a not started $className")
        }
    }

}