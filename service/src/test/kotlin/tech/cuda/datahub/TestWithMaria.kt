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
package tech.cuda.datahub

import ch.vorburger.mariadb4j.DB
import ch.vorburger.mariadb4j.DBConfigurationBuilder
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import me.liuwj.ktorm.schema.Table
import tech.cuda.datahub.config.DatabaseConfig
import tech.cuda.datahub.service.Database

/**
 * 基于 maria 数据库的测试套件，所有测试用例执行前启动 maria 数据库
 * 并在每个测试用例创建时重新导入[tables]所依赖的数据表
 * @author Jensen Qi <jinxiu.qi@alu.hit.edu.cn>
 * @since 1.0.0
 */
open class TestWithMaria(body: StringSpec.() -> Unit = {}, private vararg val tables: Table<*> = arrayOf()) : StringSpec(body) {


    override fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        val db = DB.newEmbeddedDB(DBConfigurationBuilder.newBuilder().also {
            it.port = 0
            it.baseDir = System.getProperty("java.io.tmpdir") +  this.javaClass.simpleName
        }.build()).also { it.start() }
        Database.connect(DatabaseConfig(port = db.configuration.port))
//        Database.connect(DatabaseConfig(port = 3306))
    }

    override fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        Database.rebuild()
        tables.forEach {
            Database.mock(it)
        }
    }
}