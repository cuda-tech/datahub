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
package tech.cuda.woden.scheduler.tracker

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import tech.cuda.woden.scheduler.TestWithDistribution
import tech.cuda.woden.scheduler.util.MachineUtil
import tech.cuda.woden.service.toLocalDateTime

/**
 * @author Jensen Qi <jinxiu.qi@alu.hit.edu.cn>
 * @since 0.1.0
 */
class MachineTrackerTest : TestWithDistribution("machines") {

    @Test
    fun testStart() {
        mockkObject(MachineUtil)
        every { MachineUtil.systemInfo } returns MachineUtil.SystemInfo("17.212.169.100", "9E-EE-49-FA-00-F4", "nknvleif", false)
        val machineTracker = MachineTracker(afterStarted = {
            it.id shouldBe 3
            it.ip shouldBe "17.212.169.100"
            it.mac shouldBe "9E-EE-49-FA-00-F4"
            it.hostname shouldBe "nknvleif"
            it.cpuLoad shouldBeGreaterThan 0
            it.memLoad shouldBeGreaterThan 0
            it.diskUsage shouldBeGreaterThan 0
            it.updateTime shouldNotBe "2036-03-31 18:40:59".toLocalDateTime()
        })
        machineTracker.start()
        machineTracker.cancelAndAwait()
        unmockkObject(MachineUtil)
    }

    @Test
    fun testStartWhenIpAndHostChange() {
        mockkObject(MachineUtil)
        every { MachineUtil.systemInfo } returns MachineUtil.SystemInfo("192.168.1.1", "9E-EE-49-FA-00-F4", "HOSTNAME", false)
        val machineTracker = MachineTracker(afterStarted = {
            it.id shouldBe 3
            it.ip shouldBe "192.168.1.1"
            it.mac shouldBe "9E-EE-49-FA-00-F4"
            it.hostname shouldBe "HOSTNAME"
            it.cpuLoad shouldBeGreaterThan 0
            it.memLoad shouldBeGreaterThan 0
            it.diskUsage shouldBeGreaterThan 0
            it.updateTime shouldNotBe "2036-03-31 18:40:59".toLocalDateTime()
        })
        machineTracker.start()
        machineTracker.cancelAndAwait()
        unmockkObject(MachineUtil)
    }

    @Test
    fun testStartWhenNotRegister() {
        mockkObject(MachineUtil)
        every { MachineUtil.systemInfo } returns MachineUtil.SystemInfo("192.168.1.1", "01-23-45-67-89-AB", "HOSTNAME", false)
        val machineTracker = MachineTracker(afterStarted = {
            it.id shouldBe 247
            it.ip shouldBe "192.168.1.1"
            it.mac shouldBe "01-23-45-67-89-AB"
            it.hostname shouldBe "HOSTNAME"
            it.cpuLoad shouldBeGreaterThan 0
            it.memLoad shouldBeGreaterThan 0
            it.diskUsage shouldBeGreaterThan 0
        })
        machineTracker.start()
        machineTracker.cancelAndAwait()
        unmockkObject(MachineUtil)
    }

}