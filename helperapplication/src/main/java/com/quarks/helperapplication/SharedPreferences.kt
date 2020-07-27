import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class SharedPreferences {
    companion object {
        private const val API_CONTROLLER = "api.php"
        private const val API_VERSION = "v1"
        private fun getSharedPreferences(context: Context): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(context)
        }

        public fun setProtocol(context: Context, protocol: String) {
            val editor = getSharedPreferences(context).edit()
            editor.putString("protocol", protocol)
            editor.apply()
        }

        public fun getProtocol(context: Context): String? = getSharedPreferences(context).getString("protocol", "https://")

        public fun setLang(context: Context, lang: String?) {
            val editor = getSharedPreferences(context).edit()
            editor.putString("lang", lang)
            editor.apply()
        }

        public fun getLang(context: Context): String? = getSharedPreferences(context).getString("lang", "en")


        public fun setWorkspace(context: Context, workspace: String?) {
            val editor = getSharedPreferences(context).edit()
            editor.putString("workspace", workspace)
            editor.apply()
        }

        public fun getWorkspace(context: Context): String? = getSharedPreferences(context).getString("workspace", "")


        public fun hasWorkspace(context: Context): Boolean {
            val workspace = getWorkspace(context)
            return workspace!!.isNotEmpty()
        }

        public fun getWorkspaceUrl(context: Context): String? {
            var workspaceUrl = ""
            val workspace: String? = this.getWorkspace(context)
            if (workspace !== "") {
                val lang: String? = getLang(context)
                workspaceUrl = "${getProtocol(context)}$workspace/$API_CONTROLLER/$lang/$API_VERSION/"
                return workspaceUrl
            }
            return workspaceUrl
        }

        public fun setToken(context: Context, token: String?) {
            val editor =
                getSharedPreferences(context).edit()
            editor.putString("token", token)
            editor.apply()
        }

        public fun resetToken(context: Context) {
            val editor =
                getSharedPreferences(context).edit()
            editor.remove("token")
            editor.apply()
        }

        public fun getToken(context: Context): String? = getSharedPreferences(context).getString("token", "")


        public fun hasToken(context: Context): Boolean = getToken(context)!!.isNotEmpty()

        public fun setUsername(context: Context, username: String?) {
            val editor = getSharedPreferences(context).edit()
            editor.putString("username", username)
            editor.apply()
        }

        public fun resetUsername(context: Context) {
            val editor = getSharedPreferences(context).edit()
            editor.remove("username")
            editor.apply()
        }

        public fun getUsername(context: Context): String? = getSharedPreferences(context).getString("username", "")

        public fun setServiceId(context: Context, serviceId: Int) {
            val editor = getSharedPreferences(context).edit()
            editor.putInt("service_id", serviceId)
            editor.apply()
        }

        public fun resetServiceId(context: Context) {
            val editor = getSharedPreferences(context).edit()
            editor.apply {
                remove("service_id")
                apply()
            }
        }

        public fun getServiceId(context: Context): Int = getSharedPreferences(context).getInt("service_id", -1)


        public fun hasServiceId(context: Context): Boolean = getServiceId(context) != -1

        public fun clearSaveSharedPreference(context: Context) {
            val editor = getSharedPreferences(context).edit()
            editor.apply {
                clear()
                apply()
            }
        }
    }

}