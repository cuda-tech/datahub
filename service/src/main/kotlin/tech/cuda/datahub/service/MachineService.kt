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
package tech.cuda.datahub.service

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.and
import me.liuwj.ktorm.dsl.asc
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.add
import tech.cuda.datahub.i18n.I18N
import tech.cuda.datahub.service.dao.MachineDAO
import tech.cuda.datahub.service.dto.MachineDTO
import tech.cuda.datahub.service.dto.toMachineDTO
import tech.cuda.datahub.service.exception.DuplicateException
import tech.cuda.datahub.service.exception.NotFoundException
import tech.cuda.datahub.service.po.MachinePO
import java.time.LocalDateTime

/**
 * @author Jensen Qi <jinxiu.qi@alu.hit.edu.cn>
 * @since 1.0.0
 */
object MachineService : Service(MachineDAO) {

    /**
     * 分页查询服务器信息
     * 如果提供了[pattern]，则对 hostname 进行模糊查询
     */
    fun listing(page: Int, pageSize: Int, pattern: String? = null): Pair<List<MachineDTO>, Int> {
        val (machines, count) = batch<MachinePO>(
            pageId = page,
            pageSize = pageSize,
            filter = MachineDAO.isRemove eq false,
            like = MachineDAO.hostname.match(pattern),
            orderBy = MachineDAO.id.asc()
        )
        return machines.map { it.toMachineDTO() } to count
    }

    /**
     * 通过[id]查找服务器信息
     * 如果找不到或已被删除，则返回 null
     */
    fun findById(id: Int) = find<MachinePO>(MachineDAO.id eq id and (MachineDAO.isRemove eq false))?.toMachineDTO()

    /**
     * 查找 hostname 为[name]的服务器
     * 如果找不到或已被删除，则返回 null
     */
    fun findByHostname(name: String) = find<MachinePO>(MachineDAO.hostname eq name and (MachineDAO.isRemove eq false))?.toMachineDTO()

    /**
     * 通过[ip]查找服务器信息
     * 如果找不到或已被删除，则返回 null
     */
    fun findByIP(ip: String) = find<MachinePO>(MachineDAO.isRemove eq false and (MachineDAO.ip eq ip))?.toMachineDTO()

    /**
     * 创建服务器
     * 如果提供的[ip]已存在，则抛出 DuplicateException
     * 服务器的 hostname, mac, cpu/内存/磁盘 由 Tracker 自行获取，因此不需要提供
     */
    fun create(ip: String): MachineDTO = Database.global.useTransaction {
        findByIP(ip)?.let { throw DuplicateException(I18N.ipAddress, ip, I18N.existsAlready) }
        val machine = MachinePO {
            this.ip = ip
            this.isRemove = false
            this.createTime = LocalDateTime.now()
            this.updateTime = LocalDateTime.now()
            this.hostname = "" // 以下字段由 MachineTracker 自动更新
            this.mac = ""
            this.cpuLoad = 0
            this.memLoad = 0
            this.diskUsage = 0
        }
        MachineDAO.add(machine)
        return machine.toMachineDTO()
    }

    /**
     * 更新服务器信息
     * 如果给定的服务器[id]不存在或已被删除，则抛出 NotFoundException
     * 如果试图更新[ip], 且[ip]已存在，则抛出 DuplicateException
     */
    fun update(
        id: Int,
        ip: String? = null,
        hostname: String? = null,
        mac: String? = null,
        cpuLoad: Int? = null,
        memLoad: Int? = null,
        diskUsage: Int? = null
    ): MachineDTO = Database.global.useTransaction {
        val machine = find<MachinePO>(MachineDAO.id eq id and (MachineDAO.isRemove eq false))
            ?: throw NotFoundException(I18N.machine, id, I18N.notExistsOrHasBeenRemove)
        ip?.let {
            findByIP(ip)?.let { throw DuplicateException(I18N.ipAddress, ip, I18N.existsAlready) }
            machine.ip = ip
        }
        hostname?.let { machine.hostname = hostname }
        mac?.let { machine.mac = mac }
        cpuLoad?.let { machine.cpuLoad = cpuLoad }
        memLoad?.let { machine.memLoad = memLoad }
        diskUsage?.let { machine.diskUsage = diskUsage }
        anyNotNull(ip, hostname, mac, cpuLoad, memLoad, diskUsage)?.let {
            machine.updateTime = LocalDateTime.now()
            machine.flushChanges()
        }
        return machine.toMachineDTO()
    }

    /**
     * 删除服务器[id]
     * 如果指定的服务器[id]不存在或已被删除，则抛出 NotFoundException
     */
    fun remove(id: Int) = Database.global.useTransaction {
        val machine = find<MachinePO>(MachineDAO.id eq id and (MachineDAO.isRemove eq false))
            ?: throw NotFoundException(I18N.machine, id, I18N.notExistsOrHasBeenRemove)
        machine.isRemove = true
        machine.updateTime = LocalDateTime.now()
        machine.flushChanges()
    }

}