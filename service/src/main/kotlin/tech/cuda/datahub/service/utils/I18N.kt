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
package tech.cuda.datahub.service.utils

import java.util.*

/**
 * @author Jensen Qi <jinxiu.qi@alu.hit.edu.cn>
 * @since 1.0.0
 */
object I18N {
    private var bundle = ResourceBundle.getBundle("datahub", Locale.CHINA)

    fun setLanguage(lang: Locale = Locale.CHINA) {
        bundle = ResourceBundle.getBundle("datahub", lang)
    }

    fun getString(key: String): String = bundle.getString(key)

    fun getStringArray(key: String): Array<String> = bundle.getStringArray(key)

    fun getObject(key: String): Any = bundle.getObject(key)
}
