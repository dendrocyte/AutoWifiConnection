package com.example.autowificonnection

import android.app.Activity
import android.app.IntentService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.*
import android.net.wifi.WifiManager.*
import android.os.ResultReceiver
import androidx.core.os.bundleOf
import java.util.*

/**
 * Created by luyiling on 2022/5/12
 * Modified by
 *
 * NOTE: Android Q 不支援 WifiManager turn on wifi, 也拿不到configuration, 也無法設定wifi disconnect
 * Description:
 *
 * @params
 * @params
 */
const val TAG_RESULT_RECIEVER = "result receiver"
const val TAG_SSID_NAME = "ssid"
open class WiFiSwitchService : IntentService("Wifi Switch") {
    lateinit var manager : WifiManager
    protected var resultReciever : ResultReceiver? = null
    protected var comsuming = false
    protected var terminating = false
    lateinit var wifiReciever: BroadcastReceiver
    open val targetQuotedSSID
    = "\"ling's Galaxy Note10+\""
    // ="\"Xiaomi_C8C4\""


    override fun onCreate() {
        super.onCreate()
        println("Create")
        //step1: register scan result
        wifiReciever = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.getBooleanExtra(EXTRA_RESULTS_UPDATED, false)
                println("get wifi scan result...")
                tryConnectTarget()
            }
        }
        val intentFor = IntentFilter(SCAN_RESULTS_AVAILABLE_ACTION).apply {
            addAction(EXTRA_RESULTS_UPDATED)
//            addAction(EXTRA_NETWORK_INFO)
        }
        baseContext.registerReceiver(wifiReciever, intentFor)
    }

    override fun onHandleIntent(intent: Intent?) {
        println("start wifi switch service...")

        resultReciever = intent?.getParcelableExtra(TAG_RESULT_RECIEVER)
        manager = baseContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


        //step2: start scan
        /**
         * startScan()
         * 雖然是Deprecated
         * 但他有但書
         * “The ability for apps to trigger scan requests will be removed in a future release”
         * 可以查到是需要permission
         * https://developer.android.com/guide/topics/connectivity/wifi-scan#wifi-scan-restrictions
         */
        while (!terminating){
            println("scan start!")
            comsuming = true
            manager.startScan()
            while (comsuming){
            }
        }
//        println("*******************************************")
//        println("Timeout")
//        println("*******************************************")
//        resultReciever?.send(Activity.RESULT_CANCELED, bundleOf(TAG_SSID_NAME to deQoute(targetSSID)))
//        baseContext.unregisterReceiver(wifiReciever)
    }

    /**
     * scan result's ssid is not quoted
     * connectInfo's ssid is quoted
     */
    protected open fun tryConnectTarget() {

        var isOnline = false
        println("scan size = ${manager.scanResults.size}")

        var recordSSID = ""
        var recordBSSID = ""
        var recordId = 0
        //step3: get scan result and check target is online
        loop@ for (result in manager.scanResults) {
            println("checking ${result.SSID}...")
            //result.capabilities //看是不是open wifi network
            if (result.SSID == deQoute(targetQuotedSSID)){
                isOnline = true
                recordBSSID = result.BSSID
                recordSSID = result.SSID
                break@loop
            }
        }

        if (!isOnline){
            println("target wifi is not exist")
            //FIXME: add network by myself ?
            resultReciever?.send(Activity.RESULT_CANCELED, bundleOf(TAG_SSID_NAME to deQoute(targetQuotedSSID)))
            comsuming = false
            return
        }


        //step4: change connect target ssid or bssid
        //manager.connectionInfo.ssid is quoted
        if (manager.connectionInfo.ssid == targetQuotedSSID){
            println("Already Connect to $targetQuotedSSID")
            resultReciever?.send(Activity.RESULT_OK, bundleOf(TAG_SSID_NAME to recordSSID))
            comsuming = false
            return
        }else{
            println("Switching to $targetQuotedSSID")

            //step5: get netId
            var targetConfig : WifiConfiguration? = null
            for (config in manager.configuredNetworks){
                println("check wifi config: ${config.SSID}, ${config.BSSID}, ${config.networkId}, ${config.status}")

                if (config.SSID == targetQuotedSSID) {
                    println("GOT one...")

                    targetConfig = config
                    recordId = config.networkId

                }
            }

            if (targetConfig == null){
                println("create Wifi config")
                //set wifi config
                /**
                 * WifiConfiguration
                 * 雖然是Deprecated
                 * 但他有但書
                 * This will become a system use only object in the future.
                 *
                 * api 29 above: WifiNetworkSpecifier.Builder()
                 */
                val config = WifiConfiguration()
                config.SSID = targetQuotedSSID
                config.BSSID = recordBSSID
                config.allowedKeyManagement = BitSet(0)
                recordId = manager.addNetwork(config)
                if (recordId == -1){
                    println("add network failed")
                    resultReciever?.send(Activity.RESULT_CANCELED, bundleOf(TAG_SSID_NAME to recordSSID))
                    terminating = true
                    comsuming = false
                    return
                }
                config.networkId = recordId
                targetConfig = config
            }


            //step6: connect network
            val success = manager.enableNetwork(recordId, true)
            println("is success: $success")

            //check connect ssid is what we want
            if (manager.connectionInfo.ssid == targetQuotedSSID){
                resultReciever?.send(Activity.RESULT_OK, bundleOf(TAG_SSID_NAME to recordSSID))
                comsuming = false
            }
            comsuming = false
        }

    }


    fun deQoute(s :String): String{
        return s.replace("\"", "",false)
    }

    override fun stopService(name: Intent?): Boolean {
        println("Stop service")
        terminating = true
        comsuming = false

        baseContext.unregisterReceiver(wifiReciever)

        return super.stopService(name)
    }

    override fun onDestroy() {
        println("Destroy")
        super.onDestroy()
        terminating = true
        comsuming = false
        baseContext.unregisterReceiver(wifiReciever)
    }
}