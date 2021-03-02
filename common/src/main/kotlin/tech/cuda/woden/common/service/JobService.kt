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
package tech.cuda.woden.common.service

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.global.add
import me.liuwj.ktorm.global.global
import tech.cuda.woden.common.i18n.I18N
import tech.cuda.woden.common.service.dao.JobDAO
import tech.cuda.woden.common.service.dao.TaskDAO
import tech.cuda.woden.common.service.dto.JobDTO
import tech.cuda.woden.common.service.dto.toJobDTO
import tech.cuda.woden.common.service.dto.TaskDTO
import tech.cuda.woden.common.service.exception.DirtyDataException
import tech.cuda.woden.common.service.exception.NotFoundException
import tech.cuda.woden.common.service.exception.OperationNotAllowException
import tech.cuda.woden.common.service.mysql.function.toDate
import tech.cuda.woden.common.service.po.JobPO
import tech.cuda.woden.common.service.po.TaskPO
import tech.cuda.woden.common.service.po.dtype.JobStatus
import tech.cuda.woden.common.service.po.dtype.SchedulePeriod
import tech.cuda.woden.common.utils.*
import java.time.LocalDateTime

/**
 * @author Jensen Qi <jinxiu.qi@alu.hit.edu.cn>
 * @since 0.1.0
 */
object JobService : Service(JobDAO) {

    /**
     * 通过[id]查找实例
     * 如果找不到或已被删除，则返回 null
     */
    fun findById(id: Int) = find<JobPO>(JobDAO.id eq id and (JobDAO.isRemove eq false))?.toJobDTO()

    /**
     * 分页查询作业信息，结果按创建时间倒序返回
     * 如果提供了[taskId]，则只返回该任务的作业
     * 如果提供了[machineId]，则只返回该机器执行的作业
     * 如果提供了[hour]，则只返回在[hour]执行的作业
     * 如果提供了[status]，则只返回对应状态的作业
     * 如果提供了[after]，则只返回创建日期晚于或等于它的记录
     * 如果提供了[before]，则只返回创建日期早于或等于它的记录
     */
    fun listing(
        pageId: Int,
        pageSize: Int,
        taskId: Int? = null,
        machineId: Int? = null,
        status: JobStatus? = null,
        hour: Int? = null,
        after: LocalDateTime? = null,
        before: LocalDateTime? = null
    ): Pair<List<JobDTO>, Int> {
        val conditions = mutableListOf(JobDAO.isRemove eq false)
        taskId?.let { conditions.add(JobDAO.taskId eq taskId) }
        machineId?.let { conditions.add(JobDAO.machineId eq machineId) }
        status?.let { conditions.add(JobDAO.status eq status) }
        hour?.let { conditions.add(JobDAO.hour eq hour) }
        after?.let { conditions.add(JobDAO.createTime.toDate() greaterEq after.toLocalDate()) }
        before?.let { conditions.add(JobDAO.createTime.toDate() lessEq before.toLocalDate()) }

        val (jobs, count) = batch<JobPO>(
            pageId = pageId,
            pageSize = pageSize,
            filter = conditions.reduce { a, b -> a and b },
            orderBy = JobDAO.createTime.desc()
        )
        return jobs.map { it.toJobDTO() } to count
    }

    /**
     * 根据[task]的信息生成当天的调度作业，并返回生成的作业列表
     * 如果任务当天应该调度，并且是小时级任务，则返回 24 个作业，否则返回一个作业
     * 如果任务当天不应该调度，则返回空列表
     * 如果[task]已失效，则抛出 OperationNotAllowException
     * 如果[task]调度时间格式非法，则抛出 OperationNotAllowException
     */
    fun create(task: TaskDTO): List<JobDTO> = Database.global.useTransaction {
        if (!task.isValid) {
            throw OperationNotAllowException(I18N.task, task.id, I18N.invalid)
        }
        if (!task.format.isValid(task.period)) {
            throw OperationNotAllowException(I18N.task, task.id, I18N.scheduleFormat, I18N.illegal)
        }
        // 只有当天需要调度的任务才会生成作业, 如果当天的作业已生成，则跳过创建，直接返回
        if (task.format.shouldSchedule(task.period)) {
            val now = LocalDateTime.now()
            val (existsJobs, existsCount) = listing(1, 25, taskId = task.id, after = now, before = now)
            if (task.period != SchedulePeriod.HOUR) { // 非小时任务只会生成一个作业
                val job = JobPO {
                    taskId = task.id
                    machineId = null
                    status = JobStatus.INIT
                    hour = task.format.hour!! // 非小时 hour 一定不为 null
                    minute = task.format.minute
                    runCount = 0
                    isRemove = false
                    createTime = now
                    updateTime = now
                }
                return when (existsCount) {
                    0 -> JobDAO.add(job).run { listOf(job.toJobDTO()) }
                    1 -> existsJobs
                    else -> throw DirtyDataException(I18N.task, task.id, task.period, I18N.job, existsJobs.map { it.id }.joinToString(", "))
                }
            } else { // 小时任务会生成 24 个作业
                return when (existsCount) {
                    0 -> (0..23).map { hr ->
                        val job = JobPO {
                            taskId = task.id
                            machineId = null
                            status = JobStatus.INIT
                            hour = hr
                            minute = task.format.minute
                            runCount = 0
                            isRemove = false
                            createTime = now
                            updateTime = now
                        }
                        JobDAO.add(job)
                        job.toJobDTO()
                    }
                    24 -> existsJobs
                    else -> throw DirtyDataException(I18N.task, task.id, task.period, I18N.job, existsJobs.map { it.id }.joinToString(", "))
                }
            }
        }
        return listOf() // 如果任务当天不应该调度，则直接返回空列表
    }

    /**
     * 更新指定[id]的作业信息
     * 如果指定[id]的作业不存在或已被删除，则抛出 NotFoundException
     * 如果试图更新[machineId]，并且该机器不存在或已被删除，则抛出 NotFoundException
     */
    fun update(id: Int, status: JobStatus? = null, machineId: Int? = null, runCount: Int? = null): JobDTO {
        val job = find<JobPO>(JobDAO.id eq id and (JobDAO.isRemove eq false))
            ?: throw NotFoundException(I18N.job, id, I18N.notExistsOrHasBeenRemove)
        status?.let {
            // todo: 作业状态可达性判断
            job.status = status
        }
        machineId?.let {
            MachineService.findById(machineId)
                ?: throw NotFoundException(I18N.machine, machineId, I18N.notExistsOrHasBeenRemove)
            // todo: 机器存活性判断
            job.machineId = machineId
        }
        runCount?.let {
            job.runCount = runCount
        }
        anyNotNull(status, machineId, runCount)?.let {
            job.updateTime = LocalDateTime.now()
            job.flushChanges()
        }
        return job.toJobDTO()
    }

    /**
     * 统计任务在[start]和[end]这段区间内，期望执行时间为[hour]的作业中，成功执行的次数
     */
    private fun TaskDTO.successCount(start: LocalDateTime? = null, end: LocalDateTime? = null, hour: Int? = null): Int {
        return listing(1, 1000, this.id, status = JobStatus.SUCCESS, hour = hour, after = start, before = end).second
    }

    /**
     * 检查作业[job]的上游任务是否执行成功或被跳过，根据上游任务的调度周期采用不同的判据：
     * 上游为 ONCE 调度：因为只会调度一次，所以成功/跳过后则认为下游 Ready
     * 上游为 HOUR 调度：如果子任务是非小时级任务，则需要上游昨天的 24 个作业均成功/跳过后才认为下游 Ready
     *                  如果子任务是小时级任务，则上游对应的 hr 作业成功/跳过时，认为子任务 hr 的作业 Ready
     * 上游为 DAY 调度：上游当天作业执行成功/跳过则认为子下游 Ready
     * 上游为 WEEK 调度： 因为一周只会调度一次，所以自然周内成功/跳过后则认为下游 Ready
     * 上游为 MONTH 调度：因为一月只会调度一次，所以自然月内成功/跳过后则认为下游 Ready
     * 上游为 YEAR 调度：因为一年只会调度一次，所以自然年内成功/跳过后则认为下游 Ready
     * todo: 暂时不支持偏移
     */
    fun isReady(job: JobDTO): Boolean {
        if (job.status == JobStatus.RUNNING) {
            return false
        }
        if (job.status == JobStatus.READY) {
            return true
        }
        val task = TaskService.findById(job.taskId)
            ?: throw NotFoundException(I18N.job, job.id, I18N.task, job.taskId, I18N.notExistsOrHasBeenRemove)
        for (parent in TaskService.listingParent(task)) {
            val success = when (parent.period) {
                SchedulePeriod.ONCE -> parent.successCount(end = job.createTime) == 1
                SchedulePeriod.HOUR -> if (task.period == SchedulePeriod.HOUR) {
                    parent.successCount(job.createTime, job.createTime, job.hour) == 1
                } else {
                    parent.successCount(job.createTime.yesterday, job.createTime.yesterday) == 24
                }
                SchedulePeriod.DAY -> parent.successCount(job.createTime, job.createTime) == 1
                SchedulePeriod.WEEK -> parent.successCount(job.createTime.monday, job.createTime) == 1
                SchedulePeriod.MONTH -> parent.successCount(job.createTime.monthStartDay, job.createTime) == 1
                SchedulePeriod.YEAR -> parent.successCount(job.createTime.newYearDay, job.createTime) == 1
            }
            if (!success) {
                return false
            }
        }
        return true
    }

    /**
     * 检查作业[id]是否可以重启
     * 如果作业状态不为 failed，则直接返回 false
     * 如果作业归属的任务已删除或已失效，则直接返回 false
     */
    fun canRetry(id: Int): Boolean {
        val job = findById(id) ?: throw NotFoundException()
        if (job.status != JobStatus.FAILED) {
            return false
        }
        val task = TaskService.findById(job.taskId) ?: return false
        return job.runCount - 1 < task.retries // 第一次执行不算重试，因此 runCount 需要减 1
    }

    /**
     * 清理 ID 为[id]或者归属任务 ID 为[taskId]的作业，并清理归属于这些作业的实例
     * 如果[id]和[taskId]都没有指定，则抛出异常
     */
    fun remove(id: Int? = null, taskId: Int? = null) = Database.global.useTransaction {
        if (Checker.allNull(id, taskId)) {
            throw OperationNotAllowException()
        }
        val conditions = mutableListOf(JobDAO.isRemove eq false)
        id?.let { conditions.add(JobDAO.id eq id) }
        taskId?.let { conditions.add(JobDAO.taskId eq taskId) }

        val (jobs, count) = batch<JobPO>(filter = conditions.reduce { a, b -> a and b })

        val now = LocalDateTime.now()
        jobs.forEach {
            it.isRemove = true
            it.updateTime = now
            InstanceService.remove(jobId = it.id)
            it.flushChanges()
        }
    }
}