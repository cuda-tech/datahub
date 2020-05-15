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
package tech.cuda.datahub.webserver.controller

import tech.cuda.datahub.webserver.Response
import tech.cuda.datahub.webserver.ResponseData
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.web.bind.annotation.*
import tech.cuda.datahub.i18n.I18N
import tech.cuda.datahub.service.GroupService
import tech.cuda.datahub.service.exception.NotFoundException
import java.lang.Exception

/**
 * @author Jensen Qi <jinxiu.qi@alu.hit.edu.cn>
 * @since 1.0.0
 */
@RestController
@EnableAutoConfiguration
@RequestMapping("/api/group")
class GroupController {

    /**
     * @api {get} /api/group 获取项目组列表
     * @apiDescription 获取项目组列表，支持分页和模糊查询
     * @apiGroup Group
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {Number} [page = 1] 分页ID
     * @apiParam {Number} [pageSize = 9999] 分页大小
     * @apiParam {String} like 项目组名模糊匹配，多个词用空格分隔，null 字符串会被忽略
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"count":32,"groups":[{"id":37,"name":"rjktnfdu","isRemove":false,"createTime":"2006-10-07 09:46:27","updateTime":"2007-07-29 16:10:54"},{"id":38,"name":"fpjsyxun","isRemove":false,"createTime":"2044-10-25 01:02:29","updateTime":"2046-03-05 16:13:40"}]}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @GetMapping
    fun listing(@RequestParam(required = false, defaultValue = "1") page: Int,
                @RequestParam(required = false, defaultValue = "9999") pageSize: Int,
                @RequestParam(required = false) like: String?): Map<String, Any> {
        val (groups, count) = GroupService.listing(page, pageSize, like)
        return Response.Success.data("groups" to groups, "count" to count)
    }

    /**
     * @api {get} /api/group/{id} 查找项目组
     * @apiDescription 查找指定 ID 的项目组信息，查找已删除的或不存在的项目组会返回失败
     * @apiGroup Group
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"group":{"id":1,"name":"root","isRemove":false,"createTime":"2050-01-10 23:42:07","updateTime":"2050-10-27 21:44:49"}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"group 7 not found"}
     */
    @GetMapping("{id}")
    fun find(@PathVariable id: Int): ResponseData {
        val group = GroupService.findById(id)
        return if (group == null) {
            Response.Failed.WithError("${I18N.group} $id ${I18N.notExistsOrHasBeenRemove}")
        } else {
            Response.Success.data("group" to group)
        }
    }

    /**
     * @api {post} /api/group 创建项目组
     * @apiDescription 创建项目组，并创建项目组的根节点，返回创建后的项目组信息
     * @apiGroup Group
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {String} name 项目组名称
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"group":{"name":"xxxx","isRemove":false,"createTime":"2020-03-07 23:23:54","updateTime":"2020-03-07 23:23:54","id":40}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @PostMapping
    fun create(@RequestParam(required = true) name: String): ResponseData {
        return try {
            val group = GroupService.create(name)
            Response.Success.data("group" to group)
        } catch (e: Exception) {
            Response.Failed.WithError(e.message ?: "服务异常")
        }
    }

    /**
     * @api {put} /api/group/{id} 更新项目组信息
     * @apiDescription 更新指定 ID 的项目组信息，并返回更新后的数据
     * @apiGroup Group
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiParam {String} [name = null] 项目组名称，如果不提供则不更新
     * @apiSuccessExample 请求成功
     * {"status":"success","data":{"group":{"id":38,"name":"fpjsy","isRemove":false,"createTime":"2044-10-25 01:02:29","updateTime":"2020-03-07 23:25:36"}}}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"group 7 not found"}
     */
    @PutMapping("{id}")
    fun update(@PathVariable id: Int, @RequestParam(required = false) name: String?) = try {
        Response.Success.data("group" to GroupService.update(id, name))
    } catch (e: NotFoundException) {
        Response.Failed.WithError(e.message ?: "系统异常")
    }

    /**
     * @api {delete} /api/group/{id} 删除项目组
     * @apiDescription 删除指定 ID 的项目组
     * @apiGroup Group
     * @apiVersion 0.1.0
     * @apiHeader {String} token 用户授权 token
     * @apiSuccessExample 请求成功
     * {"status":"success","message":"group 38 has been removed"}
     * @apiSuccessExample 请求失败
     * {"status":"failed","error":"错误信息"}
     */
    @DeleteMapping("{id}")
    fun remove(@PathVariable id: Int) = try {
        GroupService.remove(id)
        Response.Success.message("项目组 $id 已被删除")
    } catch (e: NotFoundException) {
        Response.Failed.WithError(e.message ?: "系统异常")
    }

}