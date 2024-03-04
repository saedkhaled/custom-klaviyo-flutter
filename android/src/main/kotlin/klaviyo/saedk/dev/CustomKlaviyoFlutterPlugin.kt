package klaviyo.saedk.dev

import android.app.Application
import android.content.Context
import com.klaviyo.analytics.Klaviyo
import com.klaviyo.analytics.model.Event
import com.klaviyo.analytics.model.EventKey
import com.klaviyo.analytics.model.EventType
import com.klaviyo.analytics.model.Profile
import com.klaviyo.analytics.model.ProfileKey
import io.flutter.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import java.io.Serializable
import android.content.Intent
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import org.json.JSONException
import org.json.JSONObject
import android.os.Bundle
import org.json.JSONArray
import android.net.Uri


private const val METHOD_UPDATE_PROFILE = "updateProfile"
private const val METHOD_INITIALIZE = "initialize"
private const val METHOD_SEND_TOKEN = "sendTokenToKlaviyo"
private const val METHOD_LOG_EVENT = "logEvent"
private const val METHOD_HANDLE_PUSH = "handlePush"
private const val METHOD_ON_NOTIFICATION = "onNotification"
private const val METHOD_GET_EXTERNAL_ID = "getExternalId"
private const val METHOD_RESET_PROFILE = "resetProfile"
private const val METHOD_SET_EMAIL = "setEmail"
private const val METHOD_GET_EMAIL = "getEmail"
private const val METHOD_SET_PHONE_NUMBER = "setPhoneNumber"
private const val METHOD_GET_PHONE_NUMBER = "getPhoneNumber"

private const val PROFILE_PROPERTIES_KEY = "properties"

private const val TAG = "KlaviyoFlutterPlugin"

class CustomKlaviyoFlutterPlugin : MethodCallHandler, FlutterPlugin, ActivityAware {
    private var applicationContext: Context? = null
    private lateinit var channel: MethodChannel


    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
        if (applicationContext is Application) {
            val app = applicationContext as Application
            app.registerActivityLifecycleCallbacks(Klaviyo.lifecycleCallbacks)
        } else {
            Log.w(
                TAG,
                "Context $applicationContext was not an application, can't register for lifecycle callbacks. Some notification events may be dropped as a result."
            )
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        applicationContext = null
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            METHOD_INITIALIZE -> {
                val apiKey = call.argument<String>("apiKey")
                Klaviyo.initialize(apiKey!!, applicationContext!!)
                Log.d(TAG, "initialized apiKey: $apiKey")
                result.success("Klaviyo initialized")
            }

            METHOD_SEND_TOKEN -> {
                val pushToken = call.argument<String>("token")
                if (pushToken != null) {
                    Klaviyo.setPushToken(pushToken)

                    result.success("Token sent to Klaviyo")
                }
            }

            METHOD_UPDATE_PROFILE -> {
                try {
                    val profilePropertiesRaw = call.arguments<Map<String, Any>?>()
                        ?: throw RuntimeException("Profile properties not exist")

                    var profileProperties = convertMapToSeralizedMap(profilePropertiesRaw)

                    val customProperties =
                        profileProperties[PROFILE_PROPERTIES_KEY] as Map<String, Serializable>?

                    if (customProperties != null) {
                        // as Android Klaviyo SDK requests properties to be on same Map level
                        // we should unwrap properties
                        profileProperties = profileProperties.minus(PROFILE_PROPERTIES_KEY)
                        profileProperties = profileProperties.plus(customProperties)
                    }

                    val profile = Profile(
                        profileProperties.map { (key, value) ->
                            ProfileKey.CUSTOM(key) to value
                        }.toMap()
                    )

                    Klaviyo.setProfile(profile)
                    Log.d(
                        TAG,
                        "Profile updated: ${Klaviyo.getExternalId()}, profileMap: $profileProperties"
                    )


                    result.success("Profile updated")
                } catch (e: Exception) {
                    result.error("Profile update error", e.message, e)
                }
            }

            METHOD_LOG_EVENT -> {
                val eventName = call.argument<String>("name")
                val metaDataRaw = call.argument<Map<String, Any>?>("metaData")

                if (eventName != null && metaDataRaw != null) {
                    val event = Event(EventType.CUSTOM(eventName))

                    val metaData = convertMapToSeralizedMap(metaDataRaw)
                    for (item in metaData) {
                        event.setProperty(EventKey.CUSTOM(item.key), value = item.value)
                    }
                    Klaviyo.createEvent(event)

                    Log.d(
                        TAG,
                        "Event created: $event, type: ${event.type}, value:${event.value} eventMap: ${event.toMap()}"
                    )
                    result.success("Event[$eventName] created with metadataMap: $metaData")
                }
            }

            METHOD_HANDLE_PUSH -> {
                val metaData =
                    call.argument<HashMap<String, String>>("message") ?: emptyMap<String, String>()

                if (isKlaviyoPush(metaData)) {
                    val event = Event(EventType.CUSTOM("\$opened_push"), metaData.mapKeys {
                        EventKey.CUSTOM(it.key)
                    })
                    return try {
                        Klaviyo.getPushToken()?.let { event[EventKey.CUSTOM("push_token")] = it }

                        Klaviyo.createEvent(event)
                        result.success(true)
                    } catch (e: Exception) {
                        Log.e(
                            TAG, "Failed handle push metaData:$metaData. Cause: $e"
                        )
                        result.error("Failed handle push metaData", e.message, null)
                    }
                } else {
                    return result.success(false)
                }
            }

            METHOD_GET_EXTERNAL_ID -> result.success(Klaviyo.getExternalId())

            METHOD_RESET_PROFILE -> {
                Klaviyo.resetProfile()
                result.success(true)
            }

            METHOD_GET_EMAIL -> result.success(Klaviyo.getEmail())
            METHOD_GET_PHONE_NUMBER -> result.success(Klaviyo.getPhoneNumber())

            METHOD_SET_EMAIL -> {
                call.argument<String>("email")?.let { newEmail ->
                    Klaviyo.setEmail(newEmail)
                    result.success("Email updated")
                }
            }

            METHOD_SET_PHONE_NUMBER -> {
                call.argument<String>("phoneNumber")?.let { newPhone ->
                    Klaviyo.setPhoneNumber(newPhone)
                    result.success("Phone number updated")
                }
            }

            else -> result.notImplemented()
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        binding.addOnNewIntentListener(fun(intent: Intent?): Boolean {
            // Tracks when a system tray notification is opened
            if (intent != null) {
                channel.invokeMethod(METHOD_ON_NOTIFICATION, intent.extras?.let { bundleToJSON(it).toString() })
                Klaviyo.handlePush(intent)
            }
            return false;
        })
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        binding.addOnNewIntentListener(fun(intent: Intent?): Boolean {
            // Tracks when a system tray notification is opened
            if (intent != null) {
                channel.invokeMethod(METHOD_ON_NOTIFICATION, intent.extras?.let { bundleToJSON(it).toString() })
                Klaviyo.handlePush(intent)
            }
            return false;
        })
    }

    override fun onDetachedFromActivity() {
    }

    private fun isKlaviyoPush(payload: Map<String, String>) = payload.containsKey("_k")

    companion object {
        private const val CHANNEL_NAME = "klaviyo.saedk.dev/klaviyo"
    }
}

private fun convertMapToSeralizedMap(map: Map<String, Any>): Map<String, Serializable> {
    val convertedMap = mutableMapOf<String, Serializable>()

    for ((key, value) in map) {
        if (value is Serializable) {
            convertedMap[key] = value
        } else {
            // Handle non-serializable values here if needed
            // For example, you could skip them or throw an exception
            // depending on your requirements.
        }
    }

    return convertedMap
}

private fun bundleToJSON(bundle: Bundle): JSONObject {
    val json = JSONObject()
    val ks = bundle.keySet()
    val iterator: Iterator<String> = ks.iterator()
    while (iterator.hasNext()) {
        val key = iterator.next()
        try {
            // Log.e("ReceiveIntentPlugin wrapping key", "$key")
            json.put(key, wrap(bundle.get(key)))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    return json
}

private fun wrap(o: Any?): Any? {
    if (o == null) {
        // Log.e("ReceiveIntentPlugin", "$o is null")
        return JSONObject.NULL
    }
    if (o is JSONArray || o is JSONObject) {
        // Log.e("ReceiveIntentPlugin", "$o is JSONArray or JSONObject")
        return o
    }
    if (o == JSONObject.NULL) {
        // Log.e("ReceiveIntentPlugin", "$o is JSONObject.NULL")
        return o
    }
    try {
        if (o is Collection<*>) {
            // Log.e("ReceiveIntentPlugin", "$o is Collection<*>")
            if (o is ArrayList<*>) {
                // Log.e("ReceiveIntentPlugin", "..And also ArrayList")
                return toJSONArray(o)
            }
            return JSONArray(o as Collection<*>?)
        } else if (o.javaClass.isArray) {
            // Log.e("ReceiveIntentPlugin", "$o is isArray")
            return toJSONArray(o)
        }
        if (o is Map<*, *>) {
            // Log.e("ReceiveIntentPlugin", "$o is Map<*, *>")
            return JSONObject(o as Map<*, *>?)
        }
        if (o is Boolean ||
            o is Byte ||
            o is Char ||
            o is Double ||
            o is Float ||
            o is Int ||
            o is Long ||
            o is Short ||
            o is String
        ) {
            return o
        }
        if (o is Uri || o.javaClass.getPackage().name.startsWith("java.")) {
            return o.toString()
        }
    } catch (e: Exception) {
        // Log.e("ReceiveIntentPlugin", e.message, e)
    }
    return null
}

@Throws(JSONException::class)
private fun toJSONArray(array: Any): JSONArray? {
    val result = JSONArray()
    if (!array.javaClass.isArray && array !is ArrayList<*>) {
        // Log.e("ReceiveIntentPlugin not a primitive array", "")
        throw JSONException("Not a primitive array: " + array.javaClass)
    }

    when (array) {
        is List<*> -> {
            // Log.e("ReceiveIntentPlugin toJSONArray List", "")
            // Log.e("ReceiveIntentPlugin toJSONArray List size", "${array.size}")
            array.forEach { result.put(wrap(it)) }
        }

        is Array<*> -> {
            // Log.e("ReceiveIntentPlugin toJSONArray Array", "")
            // Log.e("ReceiveIntentPlugin toJSONArray Array size", "${array.size}")
            array.forEach { result.put(wrap(it)) }
        }

        is ArrayList<*> -> {
            // Log.e("ReceiveIntentPlugin toJSONArray ArrayList", "")
            array.forEach { result.put(wrap(it)) }
        }

        is ByteArray -> {
            // Log.e("ReceiveIntentPlugin toJSONArray ByteArray", "")
            array.forEach { result.put(wrap(it)) }
        }

        else -> {
            // val typename = array.javaClass.kotlin.simpleName
            // Log.e("ReceiveIntentPlugin toJSONArray else", "$typename")
            val length = java.lang.reflect.Array.getLength(array)
            for (i in 0 until length) {
                result.put(wrap(java.lang.reflect.Array.get(array, i)))
            }
        }
    }

    // Log.e("ReceiveIntentPlugin toJSONArray result", "$result")

    return result
}