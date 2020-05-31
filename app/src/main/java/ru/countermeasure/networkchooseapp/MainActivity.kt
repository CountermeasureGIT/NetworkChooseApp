package ru.countermeasure.networkchooseapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
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
    private var okHttpClient = OkHttpClient()
    private val request = Request.Builder().url("https://api.ipify.org").build()

    private var flag = false

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

            val networks = connectivityManager.allNetworks
            var capabilities: NetworkCapabilities?

            for (network in networks) {
                capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.let {
                    if (it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    ) {
                        log("MOBILE Available $network")
                        val result = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            ConnectivityManager.setProcessDefaultNetwork(null)
                            ConnectivityManager.setProcessDefaultNetwork(network)
                        } else {
                            connectivityManager.bindProcessToNetwork(null)
                            connectivityManager.bindProcessToNetwork(network)
                        }
                        if (result) {
                            flag = true
                            log("Bound to MOBILE: $result, network: $network")
                            setCurrentNetworkText("MOBILE $network")
                            return@setOnClickListener
                        }
                    }
                }
            }
            log("DID NOT BOUND TO MOBILE NETWORK")
        }

        btn_wifi.setOnClickListener {
            log("FETCHING WIFI")

            val networks = connectivityManager.allNetworks
            var capabilities: NetworkCapabilities?

            for (network in networks) {
                capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.let {
                    if (it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    ) {
                        log("WIFI Available $network")
                        val result = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            ConnectivityManager.setProcessDefaultNetwork(null)
                            ConnectivityManager.setProcessDefaultNetwork(network)
                        } else {
                            connectivityManager.bindProcessToNetwork(null)
                            connectivityManager.bindProcessToNetwork(network)
                        }
                        if (result) {
                            flag = true
                            log("Bound to WIFI: $result, network: $network")
                            setCurrentNetworkText("WIFI $network")
                            return@setOnClickListener
                        }
                    }
                }
            }
            log("DID NOT BOUND TO WIFI NETWORK")
        }

        btn_getIp.setOnClickListener {
            if (flag) {
                okHttpClient = OkHttpClient()
                flag = false
            }
            log("Getting IP")
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    log("IP GET Error")
                    setIpText("Error")
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    log("IP GET Success: $body")
                    setIpText(body.toString())
                    response.close()
                }
            })
        }
    }

    private fun setCurrentNetworkText(str: String) {
        runOnUiThread {
            tv_currentNetwork.text = str
        }
    }

    @SuppressLint("SetTextI18n")
    private fun log(str: String) {
        runOnUiThread {
            tv_log.text = "${tv_log.text}\n${Date().format("hh:mm:ss")} $str"
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun setIpText(str: String) {
        runOnUiThread {
            tv_ip.text = str
        }
    }
}