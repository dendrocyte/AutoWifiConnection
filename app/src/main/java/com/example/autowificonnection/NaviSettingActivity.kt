package com.example.autowificonnection

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_setting.*


/**
 * Created by luyiling on 2022/5/6
 * Modified by
 *
 * TODO:
 * Description:
 * https://blog.csdn.net/mcsbary/article/details/102365928
 * https://stackoverflow.com/questions/21391395/get-ssid-when-wifi-is-connected
 * @params
 * @params
 */
class NaviSettingActivity : AppCompatActivity() {
    val TAG = this::class.java.simpleName
    val requestCode = 2222
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)


        textView.setOnClickListener {

            /** Step 1:
             * Starting with Android 8.1 (API 27),
             * apps must be granted the ACCESS_COARSE_LOCATION (or ACCESS_FINE_LOCATION) permission
             * in order to obtain results from WifiInfo.getSSID() or WifiInfo.getBSSID().
             * Apps that target API 29 or higher (Android 10) must be granted ACCESS_FINE_LOCATION.
             */
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1){
                /*ask permission*/
            }

            /**
             * Step2:
             * 會在系統page上得到完成和返回,
             * 但無法得到選擇哪種ssid
             *
             * NOTE: sharp 手機無法設定 extra_prefs_set_next_text 的文字
             * 若想在extra_prefs_set_next_text 為Bkkkkkk, 還是為default "下一步“做呈現
             * 但可以設定成"" 讓next btn 不顯示
             * */
            val intent1 = Intent(Settings.ACTION_WIFI_SETTINGS)
                .putExtra("extra_prefs_show_button_bar", true) //顯示button 在跳轉頁
                .putExtra("extra_prefs_set_next_text", "") //不讓它顯示出字，會呈現只有一個按鈕
                .putExtra("extra_prefs_set_back_text", "Back to App") //顯示Back to app的按鈕

            /**
             * 也可以去選擇wifi
             */
            val intent2 = Intent(WifiManager.ACTION_PICK_WIFI_NETWORK)

            /**
             * for webview, 也可以去選擇wifi
             * https://developer.android.com/about/versions/10/features#settings-panels
             */
            val intent3 = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            startActivityForResult(intent1, requestCode)
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: $resultCode, $data")
        /**
         * Setting
         * @situation 在setting 有做選擇就按下一步 result code = -1  //OK
         * @situation 在setting 沒做選擇就按下一步 result code = -1 //OK
         * @situation 有做選擇就按返回 result code = 0 //Cancel
         * @situation 沒做選擇就按返回 result code = 0 //Cancel
         * 總結有按沒按, result code 都沒有意義
         */

        if (this.requestCode == requestCode){
            Log.d(TAG, "onActivityResult: Wifi setting navi back")


            val wifiManager : WifiManager? = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            var wifiInfo: WifiInfo? = null
            if (wifiManager != null) wifiInfo = wifiManager.connectionInfo

            if (wifiInfo != null) {
                val ssid = wifiInfo.ssid /*you will get SSID <unknown ssid> if location turned off*/
                /*ssid 會有"" 要記得去除*/
                Log.d(TAG, "onActivityResult: $ssid")
            }

        }
    }
}