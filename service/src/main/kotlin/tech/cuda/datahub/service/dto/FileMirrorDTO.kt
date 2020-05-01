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
package tech.cuda.datahub.service.dto

import tech.cuda.datahub.annotation.pojo.DTO
import tech.cuda.datahub.service.po.FileMirrorPO
import java.time.LocalDateTime

/**
 * @author Jensen Qi <jinxiu.qi@alu.hit.edu.cn>
 * @since 1.0.0
 */
@DTO(FileMirrorPO::class)
data class FileMirrorDTO(
    val id: Int,
    val fileId: Int,
    val content: String,
    val message: String,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime
)
