package ru.countermeasure.networkchooseapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
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

    private val networkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {

            override fun onLost(network: Network) {
//                log("callback: onLost ${getCurrentNetworkCapabilities()}")
            }

            override fun onAvailable(network: Network) {
//                val capabilities = connectivityManager.getNetworkCapabilities(network)
//                val string = buildString {
//                    appendln("wifi: ${capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true}")
//                    appendln("mobile: ${capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true}")
//                    append("vpn: ${capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true}")
//                }
//                log("callback: available $string")
            }
        }
    }

    private val service by lazy {
        NotificationManagerCompat.from(this)// getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissions = arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
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

        btn_network_capabilities.setOnClickListener {
            log(getCurrentNetworkCapabilities())
        }

        btn_getIp.setOnClickListener {
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

        log(getCurrentNetworkCapabilities())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(
                networkCallback
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getCurrentNetworkCapabilities(): String {
        val list = mutableListOf<NetworkType>()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val allNetworks = connectivityManager.allNetworks
            var networkInfo: NetworkInfo?

            for (network: Network in allNetworks) {
                networkInfo = connectivityManager.getNetworkInfo(network)
                if (networkInfo != null && networkInfo.isConnected) {
                    when (networkInfo.type) {
                        ConnectivityManager.TYPE_WIFI ->
                            list.add(NetworkType.WIFI)
                        ConnectivityManager.TYPE_MOBILE ->
                            list.add(NetworkType.MOBILE)
                        ConnectivityManager.TYPE_VPN ->
                            list.add(NetworkType.WIFI_VPN)
                    }
                }
            }
            if (list.isEmpty())
                list.add(NetworkType.NOTHING)
        } else {
            val allNetworks = connectivityManager.allNetworks
            var networkCapabilities: NetworkCapabilities
            var networkType: NetworkType?

            for (network: Network in allNetworks) {
                networkType = null
                networkCapabilities =
                    connectivityManager.getNetworkCapabilities(network) ?: continue

                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                    networkType =
                        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
                            NetworkType.WIFI_VPN
                        else NetworkType.WIFI

                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                    networkType =
                        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
                            NetworkType.MOBILE_VPN
                        else NetworkType.MOBILE

                networkType?.let { list.add(it) }
            }
        }

        if (list.isEmpty())
            list.add(NetworkType.NOTHING)

        return list.joinToString(separator = ", ") { it.name }
    }

    private fun setCurrentNetworkText(str: String) {
        runOnUiThread {
            tv_currentNetwork.text = str
        }
    }

    @SuppressLint("SetTextI18n")
    private fun log(str: String) {
        runOnUiThread {
            tv_log.text = "${tv_log.text}\n${Date().format("hh:mm:ss")}\n$str"
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun setIpText(str: String) {
        runOnUiThread {
            tv_ip.text = str
        }
    }
}

enum class NetworkType {
    WIFI,
    MOBILE,
    WIFI_VPN,
    MOBILE_VPN,
    NOTHING
}