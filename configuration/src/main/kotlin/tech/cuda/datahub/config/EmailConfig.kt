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
package tech.cuda.datahub.config

import com.sun.mail.util.MailSSLSocketFactory
import java.util.*

/**
 * @author Jensen Qi <jinxiu.qi@alu.hit.edu.cn>
 * @since 1.0.0
 */
data class EmailConfig(
    val smtpHost: String, // e.g smtp.163.com
    val sender: String, // e.g someone@163.com
    val password: String, // SMTP授权密码
    val port: Int = 25,
    val protocol: String = "SMTP",
    val encoding: String = "UTF-8",
    val properties: Properties = Properties().also {
        it["mail.transport.protocol"] = "smtp"
        it["mail.smtp.host"] = smtpHost
        it["mail.smtp.auth"] = "true"
        it["mail.smtp.ssl.enable"] = true
        it["mail.smtp.ssl.socketFactory"] = MailSSLSocketFactory().also { sf -> sf.isTrustAllHosts = true }
    }
)