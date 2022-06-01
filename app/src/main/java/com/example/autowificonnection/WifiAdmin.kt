package com.example.autowificonnection

import android.content.Context
import android.net.wifi.*
import android.net.wifi.WifiManager.WifiLock
import android.util.Log
import android.util.Patterns
import java.util.regex.Pattern


/**
 * Created by luyiling on 2020/7/24
 * Modified by
 * 26以上 29以下 但又要廢止了
 *
<title> </title>
 * TODO:
 * Description:
 *
 *<IMPORTANT>
 * @params
 * @params
 *</IMPORTANT>
 */

class WifiAdmin(context: Context) {
    // 定義WifiManager物件
    private val mWifiManager: WifiManager

    // 定義WifiInfo物件
    private val mWifiInfo: WifiInfo?

    // 掃描出的網路連線列表
    private var mWifiList: List<ScanResult>? = null

    // 得到配置好的網路
    // 網路連線列表
    var configuration: List<WifiConfiguration>? = null
        private set

    // 定義一個WifiLock
    var mWifiLock: WifiLock? = null

    // 開啟WIFI
    fun openWifi() {
        if (!mWifiManager.isWifiEnabled) {
            mWifiManager.isWifiEnabled = true
        }
    }

    // 關閉WIFI
    fun closeWifi() {
        if (mWifiManager.isWifiEnabled) {
            mWifiManager.isWifiEnabled = false
        }
    }

    // 檢查當前WIFI狀態
    fun checkState(): Int {
        return mWifiManager.wifiState
    }

    // 鎖定WifiLock
    fun acquireWifiLock() {
        mWifiLock!!.acquire()
    }

    // 解鎖WifiLock
    fun releaseWifiLock() {
        // 判斷時候鎖定
        if (mWifiLock!!.isHeld) {
            mWifiLock!!.acquire()
        }
    }

    // 建立一個WifiLock
    fun createWifiLock() {
        mWifiLock = mWifiManager.createWifiLock("Test")
    }

    // 指定配置好的網路進行連線
    fun connectConfiguration(index: Int) {
        // 索引大於配置好的網路索引返回
        if (index > configuration!!.size) {
            return
        }
        // 連線配置好的指定ID的網路
        mWifiManager.enableNetwork(
            configuration!![index].networkId,
            true
        )
    }

    fun startScan() {
        mWifiManager.startScan()
        // 得到掃描結果
        mWifiList = mWifiManager.scanResults
        // 得到配置好的網路連線
        configuration = mWifiManager.configuredNetworks
    }

    // 得到網路列表
    val wifiList: List<Any>?
        get() = mWifiList

    // 檢視掃描結果
    fun lookUpScan(): StringBuilder {
        val stringBuilder = StringBuilder()
        for (i in mWifiList!!.indices) {
            stringBuilder
                .append("Index_" + i + 1.toString() + ":")
            // 將ScanResult資訊轉換成一個字串包
            // 其中把包括：BSSID、SSID、capabilities、frequency、level
            stringBuilder.append(mWifiList!![i].toString())
            stringBuilder.append("/n")
        }
        return stringBuilder
    }

    // 得到MAC地址
    val macAddress: String
        get() = if (mWifiInfo == null) "NULL" else mWifiInfo.macAddress //照理說是拿不到

    // 得到接入點的BSSID
    val bSSID: String
        get() = if (mWifiInfo == null) "NULL" else mWifiInfo.bssid

    // 得到IP地址
    val iPAddress: Int
        get() = mWifiInfo?.ipAddress ?: 0

    // 得到連線的ID
    val networkId: Int
        get() = mWifiInfo?.networkId ?: 0

    // 得到WifiInfo的所有資訊包
    val wifiInfo: String
        get() = mWifiInfo?.toString() ?: "NULL"

    // 新增一個網路並連線
    fun addNetwork(wcg: WifiConfiguration?) : Boolean {
        mWifiManager.disconnect()
        val wcgID = mWifiManager.addNetwork(wcg)
        val b = mWifiManager.enableNetwork(wcgID, true)
        mWifiManager.reconnect()
        Log.d("Wifi","a--$wcgID")
        Log.d("Wifi","b--$b")
        return b
    }

    // 清除所有連線的網路
    fun disconnectAllWifi(taskConfig: WifiConfiguration){
        configuration?.let {
            for (config in it){
                if(!(config.SSID.equals(taskConfig.SSID )
                            || config.SSID.equals("\"" + taskConfig.SSID +  "\""))) {
                    mWifiManager.disableNetwork(config.networkId)
                }
            }
        }
    }



    // 斷開指定ID的網路
    fun disconnectWifi(netId: Int) {
        mWifiManager.disableNetwork(netId)
        mWifiManager.disconnect()
    }

    //然後是一個實際應用方法，只驗證過沒有密碼的情況：
    fun CreateWifiInfo(
        SSID: String,
        Password: String,
        Type: Int
    ): WifiConfiguration{
        val config = WifiConfiguration()
        config.allowedAuthAlgorithms.clear()
        config.allowedGroupCiphers.clear()
        config.allowedKeyManagement.clear()
        config.allowedPairwiseCiphers.clear()
        config.allowedProtocols.clear()
        config.SSID = "\"" + SSID + "\""
        /**
         * 每執行一次程式，列表中就會多一個相同名字的ssid。而該方法就是檢查wifi列表中是否有以輸入引數為名的wifi熱點，
         * 如果存在，則在CreateWifiInfo方法開始配置wifi網路之前將其移除，以避免ssid的重複
         */
        val tempConfig = isExsits(SSID)
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId)
        }
        if (Type == 1) //WIFICIPHER_NOPASS
        {
            config.wepKeys[0] = ""
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            config.wepTxKeyIndex = 0
        }
        if (Type == 2) //WIFICIPHER_WEP
        {
            config.hiddenSSID = true
            config.wepKeys[0] = if (isHexString(Password)) Password else "\"" + Password + "\""
//            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)//open network
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)//share
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
            config.wepTxKeyIndex = 0
        }
        if (Type == 3) //WIFICIPHER_WPA
        {
            config.preSharedKey = "\"" + Password + "\""
            config.hiddenSSID = true
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)//open network
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
            //否則當wifi熱點需要輸入密碼時，無法加入網路
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
            config.status = WifiConfiguration.Status.ENABLED
        }
        return config
    }

    private fun isExsits(SSID: String): WifiConfiguration? {
        val existingConfigs =
            mWifiManager.configuredNetworks
        for (existingConfig in existingConfigs) {
            if (existingConfig.SSID == "\"" + SSID + "\"") {
                return existingConfig
            }
        }
        return null
    }

    private fun isHexString(s: String) = Pattern.matches("^[0-9a-fA-F]+$", s)
    // 構造器
    init {
        // 取得WifiManager物件
        mWifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        // 取得WifiInfo物件
        mWifiInfo = mWifiManager.connectionInfo
    }
}
//分為三種情況：1沒有密碼 2用wep加密 3用wpa加密




