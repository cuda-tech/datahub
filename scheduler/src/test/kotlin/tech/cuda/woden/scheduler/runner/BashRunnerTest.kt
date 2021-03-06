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
package tech.cuda.woden.scheduler.runner

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/**
 * @author Jensen Qi <jinxiu.qi@alu.hit.edu.cn>
 * @since 0.1.0
 */
class BashRunnerTest : AnnotationSpec() {

    @Test
    fun testSuccess() = EnvSetter.autoConvertPathFromWindows2WSL {
        val job = BashRunner(code = """
            echo "hello"
            sleep 3
            echo "world"
        """.trimIndent())
        job.status shouldBe RunnerStatus.NOT_START
        job.start()
        do {
            Thread.sleep(100)
        } while (job.status == RunnerStatus.NOT_START)
        var hasBufferOutput = false
        while (job.status == RunnerStatus.RUNNING) {
            if (job.output != "") {
                job.output shouldBe "hello\n"
                hasBufferOutput = true
                break
            }
            Thread.sleep(123)
        }
        hasBufferOutput shouldBe true
        job.join()
        job.status shouldBe RunnerStatus.SUCCESS
        job.output shouldBe "hello\nworld\n"
    }

    @Test
    fun testWrongStatement() = EnvSetter.autoConvertPathFromWindows2WSL {
        val job = BashRunner("command_not_exists")
        job.status shouldBe RunnerStatus.NOT_START
        job.startAndJoin()
        job.status shouldBe RunnerStatus.FAILED
        job.output shouldContain "line 1: command_not_exists: command not found"
    }

    @Test
    fun testKill() = EnvSetter.autoConvertPathFromWindows2WSL {
        val job = BashRunner("""
            echo hello
            sleep 10
            echo world
        """.trimIndent())
        job.status shouldBe RunnerStatus.NOT_START
        job.start()
        do {
            Thread.sleep(1000)
        } while (job.status == RunnerStatus.NOT_START || !job.output.contains("hello"))
        job.kill()
        job.join()
        job.status shouldBe RunnerStatus.KILLED
        job.output shouldContain "hello\n"
        job.output shouldNotContain "world"
    }

    @Test
    fun userDefineArgument() = EnvSetter.autoConvertPathFromWindows2WSL {
        val job = BashRunner(
            code = """
                echo ${'$'}1
                echo ${'$'}2
            """.trimIndent(),
            arguments = listOf("first", "second")
        )
        job.status shouldBe RunnerStatus.NOT_START
        job.startAndJoin()
        job.status shouldBe RunnerStatus.SUCCESS
        job.output shouldBe "first\nsecond\n"
    }

    @Test
    fun userDefineKvArgument() = EnvSetter.autoConvertPathFromWindows2WSL {
        val job = BashRunner(
            code = """
                while [[ ${'$'}# -gt 0 ]]
                do
                    key="${'$'}1"
                    case ${'$'}key in
                        -f|--first)
                        FIRST="${'$'}2"
                        shift
                        shift
                        ;;

                        -s|--second)
                        SECOND="${'$'}2"
                        shift
                        shift
                        ;;
                    esac
                done
                echo "first = ${'$'}{FIRST}"
                echo "second = ${'$'}{SECOND}"
            """.trimIndent(),
            kvArguments = mapOf("-f" to "1", "--second" to "2")
        )
        job.status shouldBe RunnerStatus.NOT_START
        job.startAndJoin()
        job.status shouldBe RunnerStatus.SUCCESS
        job.output shouldBe "first = 1\nsecond = 2\n"
    }

}