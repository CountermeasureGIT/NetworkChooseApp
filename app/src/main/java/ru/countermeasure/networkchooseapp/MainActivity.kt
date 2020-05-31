package ru.countermeasure.networkchooseapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private val connectivityManager by lazy {
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private val telephonyManager by lazy {
        applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }
    private val okHttpClient = OkHttpClient()
    private val request = Request.Builder().url("https://api.ipify.org").build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissions = arrayOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE
        )

        for (permission in permissions)
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, permissions, 100)
            }

        btn_mobile.setOnClickListener {
            log("FETCHING MOBILE AVAILABILITY")

            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.requestNetwork(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onUnavailable() {
                        log("MOBILE UNAVAILABLE")
                    }

                    override fun onAvailable(network: Network) {
                        log("MOBILE Available")
                        val result = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            ConnectivityManager.setProcessDefaultNetwork(network)
                        } else {
                            connectivityManager.bindProcessToNetwork(network)
                        }
                        log("Bound to MOBILE: $result, network: $network")
                        if (result) setCurrentNetwork("MOBILE $network")
                    }
                }
            )
        }

        btn_wifi.setOnClickListener {
            log("FETCHING WIFI")

            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.requestNetwork(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onUnavailable() {
                        log("WIFI unavailable")
                    }

                    override fun onAvailable(network: Network) {
                        log("WIFI Available")
                        val result = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            ConnectivityManager.setProcessDefaultNetwork(network)
                        } else {
                            connectivityManager.bindProcessToNetwork(network)
                        }
                        log("Bound to WIFI: $result, network: $network")
                        if (result) setCurrentNetwork("WIFI $network")
                    }
                }
            )
        }

        btn_getIp.setOnClickListener {
            log("Getting IP")
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    log("IP GET Error")
                    setIp("Error")
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    log("IP GET Success")
                    setIp(body.toString())
                    response.close()
                }
            })
        }
    }

    private fun setCurrentNetwork(str: String) {
        runOnUiThread {
            tv_currentNetwork.text = str
        }
    }

    private fun log(str: String) {
        runOnUiThread {
            tv_log.text = "${tv_log.text}\n${Date().format("hh:mm:ss")} $str"
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun setIp(str: String) {
        runOnUiThread {
            tv_ip.text = str
        }
    }
}