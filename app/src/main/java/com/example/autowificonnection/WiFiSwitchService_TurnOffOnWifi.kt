package com.example.autowificonnection

import android.app.Activity
import android.net.wifi.*
import android.net.wifi.WifiManager.*
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
class WiFiSwitchService_TurnOffOnWifi : WiFiSwitchService() {
    override val targetQuotedSSID
    = "\"ling's Galaxy Note10+\""
    // ="\"Xiaomi_C8C4\""



    /**
     * scan result's ssid is not quoted
     * connectInfo's ssid is quoted
     */
    override fun tryConnectTarget() {

        //step1: check target ssid is online
        var isOnline = false
        println("scan size = ${manager.scanResults.size}")

        var recordSSID = ""
        var recordBSSID = ""
        var recordId = 0
        //step2: get scan result and check target is online
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

        //step3: get current ssid
        val nowSSID = manager.connectionInfo.ssid

        //step4: change connect target ssid or bssid
        println("now: $nowSSID, target: $targetQuotedSSID")
        if (nowSSID == targetQuotedSSID){
            println("Already Connect to $targetQuotedSSID")
            resultReciever?.send(Activity.RESULT_OK, bundleOf(TAG_SSID_NAME to recordSSID))
            comsuming = false
            return
        }else{
            println("Switching to $targetQuotedSSID")

            //step5: turn off
            manager.setWifiEnabled(false)

            /**
             * 在家裡小米基地台存在時，是需要做
             * Thread.sleep(500)
             * 比較穩
             *
             * 現在家裡換成TOTO基地台，就不要做Thread.sleep(500)
             * 比較穩
             */
            //Thread.sleep(500)

            //step6: turn on
            manager.setWifiEnabled(true)

            //step7: connect target
            connectTarget(nowSSID, recordId, recordBSSID, recordSSID)

        }

    }


    private fun connectTarget(
        nowSSID: String?,
        recordId: Int,
        recordBSSID: String,
        recordSSID: String
    ) {
        //step7: get netId
        var recordId1 = recordId
        var nowNetworkId = -1
        var targetConfig: WifiConfiguration? = null
        for (config in manager.configuredNetworks) {
            println("check wifi config: ${config.SSID}, ${config.BSSID}, ${config.networkId}")

            if (config.SSID == targetQuotedSSID) {
                targetConfig = config
                recordId1 = config.networkId
                println("Target netId = ${recordId1}")
            }
        }

        if (targetConfig == null) {
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
            recordId1 = manager.addNetwork(config)
            if (recordId1 == -1) {
                println("add network failed")
                resultReciever?.send(
                    Activity.RESULT_CANCELED,
                    bundleOf(TAG_SSID_NAME to recordSSID)
                )
                terminating = true
                comsuming = false
                return
            }
            config.networkId = recordId1
            println("Target netId = ${recordId1}")
            targetConfig = config
        }



        //step8: connect target network
        val success = manager.enableNetwork(recordId1, true)
        println("is success: $success")

        //check connect ssid is what we want
        if (manager.connectionInfo.ssid == targetQuotedSSID) {
            resultReciever?.send(Activity.RESULT_OK, bundleOf(TAG_SSID_NAME to recordSSID))
            comsuming = false
        }
        comsuming = false
    }


}