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
 * Description: 因為使用新的router又再次調整過
 *
 * @params
 * @params
 */
class WiFiSwitchService_TurnOffOnWiFi_tune : WiFiSwitchService() {

    override val targetQuotedSSID
    //= "\"ling's Galaxy Note10+\""
     ="\"Skylink-0ecf\""
    // ="\"Xiaomi_C8C4\""

    private val keepConnectThreshold = 3
    @Volatile
    private var continuedConnectCount = 0

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
            calContinuedConnectCount(false)
            resultReciever?.send(Activity.RESULT_CANCELED, bundleOf(TAG_SSID_NAME to deQoute(targetQuotedSSID)))
            comsuming = false
            return
        }

        //step3: get current ssid
        var nowSSID = manager.connectionInfo.ssid
        while (nowSSID == "<unknown ssid>"){
            println("目前正在轉換網路，也有可能沒有權限得知")
            nowSSID = manager.connectionInfo.ssid
        }

        //step4: change connect target ssid or bssid
        println("now: $nowSSID, nowId: ${manager.connectionInfo.networkId}, target: $targetQuotedSSID")
        if (nowSSID == targetQuotedSSID){
            println("Already Connect to $targetQuotedSSID")
            //note: 有時候並非是成功轉過去，需要確定幾次 continuedConnectCount
            if (calContinuedConnectCount(true) >= keepConnectThreshold) {
                resultReciever?.send(Activity.RESULT_OK, bundleOf(TAG_SSID_NAME to recordSSID))
            }
            comsuming = false
            return
        }else{
            println("Switching to $targetQuotedSSID")
            calContinuedConnectCount(false)

            //step5: turn off
            manager.setWifiEnabled(false)
            /**
             * 在小米基地台存在時，是需要做
             * Thread.sleep(500)
             * 比較穩
             *
             * 現在換成TOTO基地台，就不要做Thread.sleep(500)
             * 比較穩
             */

            //檢查確認wifi state 為disabled 再做後續
            while (manager.wifiState != WIFI_STATE_DISABLED){
                println("enable status: ${manager.wifiState}")
            }

            Thread.sleep(1000)

            //step6: turn on
            manager.setWifiEnabled(true)

            //檢查確認wifi state 為enabled 再做後續
            while (manager.wifiState != WIFI_STATE_ENABLED){
                println("check enable status: ${manager.wifiState}")
            }

            //step7: connect target
            connectTarget(nowSSID, recordId, recordBSSID, recordSSID)

        }

    }

    private fun calContinuedConnectCount(isSuccess: Boolean) : Int{
        return (
                if (!isSuccess)
                    if (continuedConnectCount == 0) continuedConnectCount
                    else --continuedConnectCount
                else ++continuedConnectCount
                ).also {
                    println("continued connect count= $continuedConnectCount")
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
            //println("check wifi config: ${config.SSID}, ${config.status}, ${config.networkId}")

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
        while(!manager.enableNetwork(recordId1, true).also { println("is success: $it") }){
            //只要false 就持續要求
            /**
             * @前提 200ms 休息的狀況下，enable wifi 直到成功，
             * @result scan result 會去除他
             * @result 14sec後，穩定連上target wifi
             *
             *
             * 等連成功到穩定的already connect target wifi, 即可確定他斷掉app後也能繼續穩定連線
             */
            Thread.sleep(50)
        }
        /**
         * 當success 時
         * 此時此刻ssid: <unknown ssid>, 或是原本連上的wifi ssid
         *
         * println("此時此刻ssid: ${manager.connectionInfo.ssid}")
         */

        //check connect ssid is what we want
        if (manager.connectionInfo.ssid == targetQuotedSSID) {
            if (calContinuedConnectCount(true) >= keepConnectThreshold) {
                resultReciever?.send(Activity.RESULT_OK, bundleOf(TAG_SSID_NAME to recordSSID))
            }
            comsuming = false
        }
        comsuming = false
    }


}