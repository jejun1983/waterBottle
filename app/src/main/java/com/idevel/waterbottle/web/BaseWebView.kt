package com.idevel.waterbottle.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import com.idevel.waterbottle.BuildConfig
import com.idevel.waterbottle.utils.DLog
import com.idevel.waterbottle.web.constdata.WaterBottleServerScript
import com.idevel.waterbottle.web.interfaces.IWebBridgeApi

@SuppressLint("SetJavaScriptEnabled")
class BaseWebView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    init {
        val webSettings = settings

        if (BuildConfig.DEBUG) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
            }
        }

        webSettings.javaScriptEnabled = true
        webSettings.setSupportZoom(false)
        webSettings.builtInZoomControls = false
        webSettings.useWideViewPort = true
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE

        // 텍스트 크기 고정
        webSettings.textZoom = 100

        val userAgent = StringBuffer(webSettings.userAgentString)
//        userAgent.append(";" + "customerservice")
//        userAgent.append(";" + context.packageName)
//        userAgent.append(";" + "OS")
//        userAgent.append(";" + "AOS")

//        userAgent.append("Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/56.0.0.0 Mobile Safari/535.19")
//        webSettings.userAgentString = userAgent.toString()

        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setSupportMultipleWindows(true)
        webSettings.userAgentString = webSettings.userAgentString.replace("; wv","")
    }

    fun setJSInterface(api: IWebBridgeApi) {
        addJavascriptInterface(MyWebInterface(context, api), MyWebInterface.NAME)
    }

    fun sendEvent(function: String) {
        sendEvent(function, "")
    }

    fun sendEvent(function: WaterBottleServerScript) {
        sendEvent(function.scriptName, "")
    }

    fun sendEvent(function: WaterBottleServerScript, params: String) {
        sendEvent(function.scriptName, params)
    }

    fun sendEvent(function: String, params: String?) {
        (context as Activity).runOnUiThread {
            val hasParams = params != null && params.isNotEmpty()
            val script = if (hasParams) {
                "${MyWebInterface.webInvoker}('$function', $params)"
            } else {
                "${MyWebInterface.webInvoker}('$function')"
            }

            DLog.e("bjj data: script:  $script")

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                evaluateJavascript(script, null)
            } else {
                loadUrl("javascript:" + script)
            }
        }
    }
}