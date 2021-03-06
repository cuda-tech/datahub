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
package tech.cuda.woden.common.service.dao

import me.liuwj.ktorm.schema.*
import tech.cuda.woden.annotation.mysql.*
import tech.cuda.woden.common.service.po.FilePO
import tech.cuda.woden.common.service.po.dtype.FileType

/**
 * @author Jensen Qi <jinxiu.qi@alu.hit.edu.cn>
 * @since 0.1.0
 */
@STORE_IN_MYSQL
internal object FileDAO : Table<FilePO>("file") {
    @BIGINT
    @UNSIGNED
    @AUTO_INCREMENT
    @PRIMARY_KEY
    @NOT_NULL
    @COMMENT("文件 ID")
    val id = int("id").primaryKey().bindTo { it.id }


    @INT
    @UNSIGNED
    @NOT_NULL
    @COMMENT("项目组 ID")
    val teamId = int("team_id").bindTo { it.teamId }

    @INT
    @UNSIGNED
    @NOT_NULL
    @COMMENT("创建者 ID")
    val ownerId = int("owner_id").bindTo { it.ownerId }

    @VARCHAR(128)
    @COMMENT("文件名")
    val name = varchar("name").bindTo { it.name }

    @VARCHAR(32)
    @COMMENT("文件类型")
    val type = enum("type", typeRef<FileType>()).bindTo { it.type }

    @BIGINT
    @UNSIGNED
    @COMMENT("父节点 ID")
    val parentId = int("parent_id").bindTo { it.parentId }

    @TEXT
    @COMMENT("文件内容")
    val content = text("content").bindTo { it.content }

    @BOOL
    @NOT_NULL
    @COMMENT("逻辑删除")
    val isRemove = boolean("is_remove").bindTo { it.isRemove }

    @DATETIME
    @NOT_NULL
    @COMMENT("创建时间")
    val createTime = datetime("create_time").bindTo { it.createTime }

    @DATETIME
    @NOT_NULL
    @COMMENT("更新时间")
    val updateTime = datetime("update_time").bindTo { it.updateTime }
}
