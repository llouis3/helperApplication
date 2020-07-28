package com.quarks.helperapplication
import SharedPreferences
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.apache.commons.io.IOUtils
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.DecimalFormat
import javax.net.ssl.*

val applicationId: String? = null
val versionCode: String? = null
val versionName: String? = null

/**
 * Class for call web api with json object
 * @property INTENT_EXTRA_CATEGORY name of extra intent for category file
 * @property INTENT_EXTRA_CLASSNAME name of extra intent for class name file
 * @property INTENT_EXTRA_FILENAME name of extra intent for name file
 * @property INTENT_EXTRA_FILE name of extra intent for byte array of file
 */
class ApiService : IntentService(ApiService::class.java.name) {
    companion object {
        const val INTENT_EXTRA_CATEGORY = "category"
        const val INTENT_EXTRA_CLASSNAME = "className"
        const val INTENT_EXTRA_FILENAME = "fileName"
        const val INTENT_EXTRA_FILE = "fileByteArray"
        const val REQUEST_ERROR = 0
        const val REQUEST_SUCCESS = 1
        private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
        private val PDF = "application/pdf".toMediaTypeOrNull()
        private val IMAGE = "image".toMediaTypeOrNull()
        private val TAG = "ApiService"

        /**
         * Create request with the route and the json object to send
         *
         *@param context use context for SharedPreference
         * @param requestBody json object to send
         * @param fileToSend byte array of file
         * @param fileName filename to send, null if you do not want to send
         * @param category category of file, null if you do not want to send
         * @param className of file, null if you do not want to send
         *
         * @return the created request
         */
        fun createRequest(
            context: Context,
            route: String,
            requestBody: String?,
            fileToSend: ByteArray?,
            fileName: String?,
            category: String?,
            className: String?
        ): Request {
            var requestBuilder = Request.Builder()
            if (SharedPreferences.hasWorkspace(context)) {
                val url = SharedPreferences.getWorkspaceUrl(context) + route
                requestBuilder.url(url)
                Log.i(TAG, "$url > $requestBody")
            }
            if (SharedPreferences.hasToken(context)) {
                val token = SharedPreferences.getToken(context)
                requestBuilder.header("Authorization", "Bearer $token")
            }
            try {
                val body: JSONObject = if (requestBody != null) {
                    JSONObject(requestBody)
                } else {
                    JSONObject("{}")
                }
                body.put("context", createContextData(context))
                Log.i(TAG, body.toString())
                if (fileToSend != null) {
                    requestBuilder = getBuilderForSendFile(
                        requestBuilder,
                        fileToSend,
                        fileName,
                        category!!,
                        className!!
                    )
                } else {
                    requestBuilder.post(RequestBody.create(JSON, body.toString()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.i(TAG, "create Request")
            }
            return requestBuilder.build()
        }

        /**
         * Create builder for send file
         * @param requestBuilder add file to request builder
         * @param fileToSend byteArray of file
         * @param fileName of file
         * @param category of file
         * @param className of file
         */
        private fun getBuilderForSendFile(
            requestBuilder: Request.Builder,
            fileToSend: ByteArray,
            fileName: String?,
            category: String,
            className: String
        ): Request.Builder {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
            if (fileName!!.toLowerCase().contains("pdf")) {
                requestBody.addFormDataPart("image", fileName, RequestBody.create(PDF, fileToSend))
            } else {
                requestBody.addFormDataPart(
                    "image",
                    fileName,
                    RequestBody.create(IMAGE, BitmapHelper.getByteArrayOfBitmap(fileToSend, fileName))
                )
            }
            requestBody.apply {
                addFormDataPart("class_name", className)
                addFormDataPart("category", category)
            }
            requestBody.build()
            val builder = requestBody.build()
            requestBuilder.post(builder)
            return requestBuilder
        }

        private fun createContextData(context: Context): JSONObject {
            val contextData = JSONObject()
            try {
                contextData.put("device_name", Build.DEVICE)
                contextData.put("device_model", Build.MODEL)
                contextData.put("device_brand", Build.BRAND)
                contextData.put("device_manufacturer", Build.MANUFACTURER)
                val batteryStatus: Intent = context.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )!!
                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val charging =
                    status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val battery = level * 100 / (scale.toFloat())
                contextData.put("device_charging", charging)
                contextData.put("device_battery", battery)
                contextData.put(
                    "device_available_internal_memory",
                    bytesToHuman(getAvailableInternalMemory())
                )
                contextData.put(
                    "device_available_external_memory",
                    bytesToHuman(getAvailableExternalMemory())
                )
                contextData.put(
                    "device_available_system_memory",
                    bytesToHuman(getAvailableSystemMemory())
                )
                contextData.put("app_name", applicationId)
                contextData.put(
                    "app_version",
                    "$versionName (build " + String.format(
                        "%06d",
                        versionCode
                    ) + ")"
                )
                contextData.put("os_name", "android");
                contextData.put(
                    "os_version",
                    Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")"
                )

            } catch (e: Exception) {
                e.printStackTrace()
                Log.i(TAG, "create context data")
            }
            return contextData
        }

        private fun getAvailableInternalMemory(): Long {
            return getAvailableMemory(Environment.getDataDirectory())
        }

        private fun getAvailableExternalMemory(): Long {
            return getAvailableMemory(Environment.getExternalStorageDirectory())
        }

        private fun getAvailableSystemMemory(): Long {
            return getAvailableMemory(Environment.getRootDirectory())
        }

        private fun getAvailableMemory(path: File): Long {
            val stats = StatFs(path.absolutePath)
            return stats.availableBlocksLong * stats.blockSizeLong
        }

        private fun floatForm(d: Double): String {
            return DecimalFormat("#.##").format(d)
        }

        private fun bytesToHuman(size: Long): String {
            val Kb = 1 * 1024.toLong()
            val Mb = Kb * 1024
            val Gb = Mb * 1024
            val Tb = Gb * 1024
            val Pb = Tb * 1024
            val Eb = Pb * 1024

            if (size < Kb) return floatForm(size.toDouble()) + " byte"
            if (size in Kb until Mb) return floatForm(size.toDouble() / Kb) + " Kb"
            if (size in Mb until Gb) return floatForm(size.toDouble() / Mb) + " Mb"
            if (size in Gb until Tb) return floatForm(size.toDouble() / Gb) + " Gb"
            if (size in Tb until Pb) return floatForm(size.toDouble() / Tb) + " Tb"
            if (size in Pb until Eb) return floatForm(size.toDouble() / Pb) + " Pb"
            return if (size >= Eb) floatForm(size.toDouble() / Eb) + " Eb" else "???"
        }

        public fun getCertificateOkHttpClient(): OkHttpClient.Builder {
            return try {
                val trustAllCerts =
                    arrayOf<TrustManager>(
                        object : X509TrustManager {
                            override fun checkClientTrusted(
                                chain: Array<X509Certificate>,
                                authType: String
                            ) {
                            }

                            override fun checkServerTrusted(
                                chain: Array<X509Certificate>,
                                authType: String
                            ) {
                            }

                            override fun getAcceptedIssuers(): Array<X509Certificate> {
                                return arrayOf()
                            }
                        }
                    )

                val sslContext =
                    SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())

                val sslSocketFactory = sslContext.socketFactory
                val builder = OkHttpClient.Builder()
                builder.sslSocketFactory(
                    sslSocketFactory,
                    (trustAllCerts[0] as X509TrustManager)
                )
                builder.hostnameVerifier(HostnameVerifier { _: String?, _: SSLSession? -> true })
                builder
            } catch (e: java.lang.Exception) {
                throw RuntimeException(e)
            }
        }
    }
    override fun onHandleIntent(intent: Intent?) {
        val receiver: ResultReceiver? = intent!!.getParcelableExtra("receiver")
        val bundle = Bundle()
        bundle.putSerializable(
            INTENT_EXTRA_CATEGORY, intent.getSerializableExtra(
                INTENT_EXTRA_CATEGORY
            ))
        try {
            Log.i(TAG, "send on api service")
            val request = createRequest(
                this@ApiService,
                intent.getStringExtra("url"),
                intent.getStringExtra("requestBody"),
                intent.getByteArrayExtra(INTENT_EXTRA_FILE),
                intent.getStringExtra(INTENT_EXTRA_FILENAME),
                intent.getStringExtra(INTENT_EXTRA_CATEGORY),
                intent.getStringExtra(INTENT_EXTRA_CLASSNAME)
            )
            val client = getCertificateOkHttpClient().build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.i(TAG, e.message)
                    bundle.apply {
                        putString(INTENT_EXTRA_CATEGORY, intent.getStringExtra(INTENT_EXTRA_CATEGORY))
                        putString(INTENT_EXTRA_FILENAME, intent.getStringExtra(INTENT_EXTRA_FILENAME))
                    }
                    receiver!!.send(REQUEST_ERROR, bundle)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.isSuccessful) {
                            val jsonResponse = JSONObject(response.body!!.string())
                            bundle.putString("data", jsonResponse.getString("data"))
                            receiver!!.send(REQUEST_SUCCESS, bundle)
                        } else {
                            val jsonResponse = JSONObject(response.body!!.string())
                            bundle.apply {
                                putString("message", jsonResponse.getString("message"))
                                putString(
                                    INTENT_EXTRA_FILENAME, intent.getStringExtra(
                                        INTENT_EXTRA_FILENAME
                                    ))
                                putString(
                                    INTENT_EXTRA_CATEGORY, intent.getStringExtra(
                                        INTENT_EXTRA_CATEGORY
                                    ))
                            }
                            receiver!!.send(REQUEST_ERROR, bundle)
                        }
                    } catch (e: Exception) {
                        Log.i(TAG, e.message!!)
                        e.printStackTrace()
                        bundle.apply {
                            putString(
                                INTENT_EXTRA_FILENAME, intent.getStringExtra(
                                    INTENT_EXTRA_FILENAME
                                ))
                            putString(
                                INTENT_EXTRA_CATEGORY, intent.getStringExtra(
                                    INTENT_EXTRA_CATEGORY
                                ))
                        }
                        receiver!!.send(REQUEST_ERROR, bundle)
                    }
                }

            })
        } catch (e: Exception) {
            e.printStackTrace()
            bundle.apply {
                putString(INTENT_EXTRA_FILENAME, intent.getStringExtra(INTENT_EXTRA_FILENAME))
                putString(INTENT_EXTRA_CATEGORY, intent.getStringExtra(INTENT_EXTRA_CATEGORY))
            }
            receiver!!.send(REQUEST_ERROR, bundle)
        }
    }
}