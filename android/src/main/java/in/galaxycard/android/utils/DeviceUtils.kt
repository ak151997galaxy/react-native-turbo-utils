package `in`.galaxycard.android.utils

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.res.Resources
import android.location.LocationManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.*
import android.os.Process.myPid
import android.provider.Settings
import android.provider.Settings.Secure.getString
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.core.content.pm.PackageInfoCompat
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.miui.referrer.annotation.GetAppsReferrerResponse.Companion.OK
import com.miui.referrer.api.GetAppsReferrerClient
import com.miui.referrer.api.GetAppsReferrerStateListener
import java.math.BigInteger

class DeviceUtils(private val context: Context) {
    companion object {
        const val BATTERY_STATE = "batteryState"
        const val BATTERY_LEVEL = "batteryLevel"
        const val INSTALL_REFERRER = "installReferrer"
        const val LOW_POWER_MODE = "lowPowerMode"
    }

    init {
        val sharedPreferences = context.getSharedPreferences(TurboStarterModule.NAME, Context.MODE_PRIVATE)
        getInstallReferrerFromGetApps(sharedPreferences)
    }

    private fun getInstallReferrerFromGetApps(sharedPreferences: SharedPreferences) {
        if (!sharedPreferences.contains(INSTALL_REFERRER)) {
            val referrerClient = GetAppsReferrerClient.newBuilder(context).build()

            referrerClient.startConnection(object : GetAppsReferrerStateListener {
                override fun onGetAppsReferrerSetupFinished(state: Int) {
                    when (state) {
                        OK -> {
                            val editor: SharedPreferences.Editor = sharedPreferences.edit()
                            editor.putString(INSTALL_REFERRER, referrerClient.installReferrer.installReferrer)
                            editor.apply()
                            referrerClient.endConnection()
                        }
                        else -> {
                            getInstallReferrerFromGooglePlay(sharedPreferences)
                        }
                    }
                }
                override fun onGetAppsServiceDisconnected() {
                    Log.d("GetApps", "disconnected")
                }
            })
        }
    }

    private fun getInstallReferrerFromGooglePlay(sharedPreferences: SharedPreferences) {
        if (!sharedPreferences.contains(INSTALL_REFERRER)) {
            val referrerClient = InstallReferrerClient.newBuilder(context).build()

            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            editor.putString(INSTALL_REFERRER, referrerClient.installReferrer.installReferrer)
                        }
                        else -> {
                            // Maybe set it to ""?
                            editor.putString(INSTALL_REFERRER, "");
                        }
                    }
                    editor.apply()
                    referrerClient.endConnection()
                }

                override fun onInstallReferrerServiceDisconnected() {
                    Log.d("GooglePlay", "disconnected")
                }
            })
        }
    }

    fun constants(): MutableMap<String, Any> {
        var appVersion: String
        var buildNumber: String
        var appName: String
        try {
            val packageInfo = getPackageInfo(context)
            appVersion = packageInfo.versionName
            buildNumber = PackageInfoCompat.getLongVersionCode(packageInfo).toString()
            appName =
                context.applicationInfo.loadLabel(context.packageManager)
                    .toString()
        } catch (e: Exception) {
            appVersion = Build.UNKNOWN
            buildNumber = Build.UNKNOWN
            appName = Build.UNKNOWN
        }
        val constants: MutableMap<String, Any> = HashMap()
        constants["uniqueId"] =
            getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        constants["deviceId"] = Build.BOARD
        constants["bundleId"] = context.packageName
        constants["systemVersion"] = Build.VERSION.RELEASE
        constants["appVersion"] = appVersion
        constants["buildNumber"] = buildNumber
        constants["appName"] = appName
        constants["brand"] = Build.BRAND
        constants["model"] = Build.MODEL
        constants["manufacturer"] = Build.MANUFACTURER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics: WindowMetrics = context.getSystemService(WindowManager::class.java).currentWindowMetrics
            constants["screenWidth"] = metrics.bounds.width()
            constants["screenHeight"] = metrics.bounds.height()
        } else {
            val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            val metrics = if (display != null) {
                DisplayMetrics().also { display.getRealMetrics(it) }
            } else {
                Resources.getSystem().displayMetrics
            }
            constants["screenWidth"] = metrics.widthPixels
            constants["screenHeight"] = metrics.heightPixels
        }
        constants["screenDensity"] = context.resources.displayMetrics.density
        val sharedPref = context.getSharedPreferences(
            TurboStarterModule.NAME,
            Context.MODE_PRIVATE
        )
        constants["installReferrer"] = sharedPref.getString(INSTALL_REFERRER, Build.UNKNOWN)!!

        return constants
    }

    fun dynamicValues(): MutableMap<String, Any> {
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val deviceData = HashMap<String, Any>()

        deviceData["hasHeadphones"] = audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn

        val telMgr =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        deviceData["carrier"] = telMgr.networkOperatorName

        deviceData["airplaneMode"] = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0

        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val powerState = getPowerStateFromIntent(context, intent)

        if (powerState != null) {
            deviceData[BATTERY_STATE] = powerState[BATTERY_STATE]!!
            deviceData[BATTERY_LEVEL] = powerState[BATTERY_LEVEL]!!
            deviceData[LOW_POWER_MODE] = powerState[LOW_POWER_MODE]!!
        }
        val keyguardManager =
            context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        deviceData["pinOrFingerprintSet"] = keyguardManager.isKeyguardSecure

        deviceData["fontScale"] = context.resources.configuration.fontScale

        try {
            val rootDir = StatFs(Environment.getRootDirectory().absolutePath)
            val dataDir = StatFs(Environment.getDataDirectory().absolutePath)
            val rootFree: Double =
                BigInteger.valueOf(rootDir.availableBlocksLong).multiply(BigInteger.valueOf(rootDir.blockSizeLong)).toDouble()
            val dataFree: Double =
                BigInteger.valueOf(dataDir.availableBlocksLong).multiply(BigInteger.valueOf(rootDir.blockSizeLong)).toDouble()
            deviceData["freeDiskStorage"] = (rootFree + dataFree).toBigDecimal()
        } catch (e: java.lang.Exception) {
            deviceData["freeDiskStorage"] = -1
        }

        try {
            val rootDir = StatFs(Environment.getRootDirectory().absolutePath)
            val dataDir = StatFs(Environment.getDataDirectory().absolutePath)

            val rootDirCapacity: BigInteger = BigInteger.valueOf(rootDir.blockCountLong).multiply(
                BigInteger.valueOf(rootDir.blockSizeLong))
            val dataDirCapacity: BigInteger = BigInteger.valueOf(dataDir.blockCountLong).multiply(
                BigInteger.valueOf(dataDir.blockSizeLong))
            deviceData["totalDiskCapacity"] = rootDirCapacity.add(dataDirCapacity).toDouble().toBigDecimal()
        } catch (e: java.lang.Exception) {
            deviceData["totalDiskCapacity"] = -1
        }

        deviceData["maxMemory"] = Runtime.getRuntime().maxMemory().toDouble().toBigDecimal()

        val actMgr =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pid = myPid()
        val memInfos = actMgr.getProcessMemoryInfo(intArrayOf(pid))
        if (memInfos.size != 1) {
            System.err.println("Unable to getProcessMemoryInfo. getProcessMemoryInfo did not return any info for the PID")
            deviceData["usedMemory"] = -1
        } else {
            val memInfo = memInfos[0]
            deviceData["usedMemory"] = (memInfo.totalPss * 1024.0).toBigDecimal()
        }

        var hasLocation = false
        val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        hasLocation =
            hasLocation or locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        hasLocation =
            hasLocation or locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        deviceData["hasLocation"] = hasLocation

        val info = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
        deviceData["wifiName"] = Build.UNKNOWN
        if (info != null && info.ssid !== null) {
            deviceData["wifiName"] = info.ssid
        }
        deviceData["accessPointName"] = Build.UNKNOWN
        if (info != null && info.bssid !== null) {
            deviceData["accessPointName"] = info.bssid
        }

        deviceData["deviceName"] = Build.UNKNOWN
        try {
            val bluetoothName =
                getString(context.contentResolver, "bluetooth_name")
            if (bluetoothName != null) {
                deviceData["deviceName"] =  bluetoothName
            }
            if (Build.VERSION.SDK_INT >= 25) {
                val deviceName = Settings.Global.getString(
                    context.contentResolver,
                    Settings.Global.DEVICE_NAME
                )
                if (deviceName != null) {
                    deviceData["deviceName"] = deviceName
                }
            }
        } catch (e: java.lang.Exception) {
            // same as default unknown return
        }

        return deviceData
    }

    @Throws(java.lang.Exception::class)
    private fun getPackageInfo(context: Context): PackageInfo {
        return context.packageManager.getPackageInfo(
            context.packageName,
            0
        )
    }

    private fun getPowerStateFromIntent(context: Context, intent: Intent?): HashMap<String, Any>? {
        if (intent == null) {
            return null
        }
        val batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val isPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val batteryPercentage = batteryLevel / batteryScale.toFloat()
        var batteryState = Build.UNKNOWN
        if (isPlugged == 0) {
            batteryState = "unplugged"
        } else if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            batteryState = "charging"
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            batteryState = "full"
        }
        val powerManager =
            context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val powerSaveMode = powerManager.isPowerSaveMode
        val powerState = HashMap<String, Any>()
        powerState[BATTERY_STATE] = batteryState
        powerState[BATTERY_LEVEL] = batteryPercentage.toDouble()
        powerState[LOW_POWER_MODE] = powerSaveMode
        return powerState
    }
}