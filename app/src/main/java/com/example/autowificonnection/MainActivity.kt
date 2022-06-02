package com.example.autowificonnection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


/**
 * 可能需要測試 多做
 * 當api > 29
 *
 * 當api < 28
 *
 *
 */
class MainActivity : AppCompatActivity() {
    lateinit var wifiAdmin : WifiAdmin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //method1
        //disconnectManager_connect_reconnectManager()


        /*
        val permissions = arrayOf(//這些非危險權限需要開啟
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        Permissions.check(
            this *//*context*//*,
            permissions,
            null *//*rationale*//*,
            null *//*options*//*,
            object : PermissionHandler() {
                override fun onGranted() {
                    // do your task.

                }
            })
*/

    }



    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Android8開始一定要做，即使Manifest 有提列,這樣才拿得到scan result
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                println("Not Granted: Fine")
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
            }
        }

        if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED){
//                connectTargetDirectly()
//                disconnect_then_connect()
            turnoff_on()
//            delConfig_then_connect()
        }
    }

    private fun disconnectManager_connect_reconnectManager() {
        val wifiAdmin = WifiAdmin(context = baseContext)
        wifiAdmin.openWifi()
        //        wifiAdmin.startScan()
        val config = wifiAdmin.CreateWifiInfo("Xiaomo_C8C4", "062598418", 3)
        //        wifiAdmin.disconnectAllWifi(config)
        wifiAdmin.addNetwork(config)
    }


    private fun connectTargetDirectly() {
        val intent = Intent(this, WiFiSwitchService::class.java)
        intent.putExtra(TAG_RESULT_RECIEVER, object : ResultReceiver(Handler()) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                super.onReceiveResult(resultCode, resultData)
                Log.d(this@MainActivity::class.java.simpleName, "onReceiveResult: $resultCode")
                if (resultCode == RESULT_OK) {
                    println("成功了")
                } else {
                    println("失敗囉")
                }

            }
        })
        startService(intent)
    }

    private fun disconnect_then_connect() {
        //method2
        val intent = Intent(this, WiFiSwitchService_DisconnectOriWifi::class.java)
        intent.putExtra(TAG_RESULT_RECIEVER, object : ResultReceiver(Handler()) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                super.onReceiveResult(resultCode, resultData)
                Log.d(this@MainActivity::class.java.simpleName, "onReceiveResult: $resultCode")
                if (resultCode == RESULT_OK) {
                    println("成功了")
                } else {
                    println("失敗囉")
                }

            }
        })
        startService(intent)
    }

    private fun turnoff_on() {
        println("turn off on.........")
        //method2
        val intent = Intent(this, WiFiSwitchService_TurnOffOnWifi::class.java)
        intent.putExtra(TAG_RESULT_RECIEVER, object : ResultReceiver(Handler()) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                super.onReceiveResult(resultCode, resultData)
                Log.d(this@MainActivity::class.java.simpleName, "onReceiveResult: $resultCode")
                if (resultCode == RESULT_OK) {
                    println("成功了")
                } else {
                    println("失敗囉")
                }

            }
        })
        startService(intent)
    }

    private fun delConfig_then_connect() {
        //method2
        val intent = Intent(this, WiFiSwitchService_DeleteOriWifiConfig::class.java)
        intent.putExtra(TAG_RESULT_RECIEVER, object : ResultReceiver(Handler()) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                super.onReceiveResult(resultCode, resultData)
                Log.d(this@MainActivity::class.java.simpleName, "onReceiveResult: $resultCode")
                if (resultCode == RESULT_OK) {
                    println("成功了")
                } else {
                    println("失敗囉")
                }

            }
        })
        startService(intent)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        println("granted permission...")
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
//            disconnect_then_connect()
            turnoff_on()
        }
    }

    companion object{
        const val REQUEST_CODE = 333
    }
}
