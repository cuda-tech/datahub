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

import com.google.common.base.Charsets
import com.google.common.io.Resources
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.global.connectGlobally
import me.liuwj.ktorm.schema.Table
import org.apache.log4j.Logger
import org.reflections.Reflections
import tech.cuda.woden.common.service.exception.OperationNotAllowException
import java.io.File
import javax.sql.DataSource


/**
 * @author Jensen Qi
 * @since 0.1.0
 */
object Database {
    private val logger = Logger.getLogger(this.javaClass)
    private const val lightBlue = "\u001B[1;94m"
    private const val end = "\u001B[m"
    private lateinit var db: Database

    /**
     * 通过反射获取 dao 下的所有类
     */
    private val models = Reflections("tech.cuda.woden.common.service.dao").getSubTypesOf(Table::class.java).map {
        it.getField("INSTANCE").get(it) as Table<*>
    }


    fun connect(dataSource: DataSource) {
        db = Database.connectGlobally(dataSource)
        build()
    }

    private fun checkConnected(block: () -> Unit) {
        if (!this::db.isInitialized || !this::db.isInitialized) {
            throw OperationNotAllowException("database disconnected")
        } else {
            block()
        }
    }

    /**
     * 创建 dao 下定义的所有表
     */
    private fun build() = checkConnected {
        db.useConnection { conn ->
            val dbName = conn.catalog
            logger.info("create database $dbName")
            conn.prepareStatement("create database if not exists $dbName default character set = 'utf8'").use { it.execute() }
            logger.info("database $dbName have been created")
            conn.catalog = dbName
            models.forEach { table ->
                val ddl = Class.forName("${table::class.qualifiedName}DDLKt")
                        .getMethod("getDDL", table::class.java)
                        .invoke(null, table) as String // kotlin 的扩展属性本质上是静态方法
                logger.info("create table for class ${table.javaClass.name}:\n" + lightBlue + ddl + end)
                conn.prepareStatement(ddl).use { it.execute() }
                logger.info("table $dbName.${table.tableName} have been created")
            }
        }
    }

    /**
     * 从删库到跑路
     */
    private fun clean() = checkConnected {
        db.useConnection { conn ->
            val dbName = conn.catalog
            models.forEach { table ->
                logger.info("drop table for class ${table.javaClass.name}")
                conn.prepareStatement("drop table if exists $dbName.${table.tableName}").use { it.execute() }
                logger.info("table $dbName.${table.tableName} have been drop")
            }
            logger.info("drop database $dbName")
            conn.prepareStatement("drop database if exists $dbName").use { it.execute() }
            logger.info("database $dbName have been drop")
        }
    }

    /**
     * 清空所有表，然后重新建表
     */
    fun rebuild() = clean().also {
        build()
    }

    /**
     * 寻找当前模块 resource 目录下与[tableName]的表名同名的 txt 文件，然后导入数据库
     * 其中，这个 txt 文件需要带表头，并且以 \t 作为字段分隔符
     */
    fun mock(tableName: String) = checkConnected {
        db.useConnection { conn ->
            val filePath = this.javaClass.classLoader.getResource("tables/$tableName.txt")!!.path
            val meta = conn.prepareStatement("select * from $tableName").use { it.metaData }
            val types = (1..meta.columnCount).map { meta.getColumnTypeName(it)!! }

            val file = Resources.readLines(File(filePath).toURI().toURL(), Charsets.UTF_8)
            file.removeAt(0)

            val values = file.joinToString(",") { line ->
                line.split("\t").mapIndexed { i, value ->
                    val type = types[i]
                    if (type.contains("CHAR") || type.contains("DATE") || type.contains("TEXT") || type == "UNKNOWN") {
                        if (value == "NULL") null else "'$value'"
                    } else {
                        value
                    }
                }.joinToString(",", "(", ")")
            }
            conn.prepareStatement("insert into $tableName values $values").use { it.execute() }
        }
    }

    fun mock(table: Table<*>) = mock(table.tableName)

    fun mockAll() = models.forEach { mock(it) }

}
