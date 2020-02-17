package com.idevel.waterbottle.web

import android.content.Context
import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.idevel.waterbottle.utils.DLog
import com.idevel.waterbottle.web.constdata.OpenSharePopupInfo
import com.idevel.waterbottle.web.constdata.RequestCallPhoneInfo
import com.idevel.waterbottle.web.constdata.RequestExternalWebInfo
import com.idevel.waterbottle.web.interfaces.IWebBridgeApi

/**
 * web 과의 연동을 위한 interface Class.
 *
 */
class MyWebInterface(private val mContext: Context, private val api: IWebBridgeApi) {
    companion object {
        const val webInvoker = "NativeInvoker"
        const val NAME = "waterBottle"
    }

    private fun gson(): Gson {
        return GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    }

    @JavascriptInterface
    fun removeSplash() {
        DLog.e("bjj data: removeSplash ")
        api.removeSplash()
    }

    @JavascriptInterface
    fun getPushRegId() {
        DLog.e("bjj data: getPushRegId ")
        api.getPushRegId()
    }

    @JavascriptInterface
    fun restartApp() {
        DLog.e("bjj data: restartApp ")
        api.restartApp()
    }

    @JavascriptInterface
    fun finishApp() {
        DLog.e("bjj data: finishApp ")
        api.finishApp()
    }

    @JavascriptInterface
    fun getAppVersion() {
        DLog.e("bjj data: getAppVersion ")
        api.getAppVersion()
    }

    @JavascriptInterface
    fun requestCallPhone(jsonData: String) {
        val data = gson().fromJson(jsonData, RequestCallPhoneInfo::class.java)
        DLog.e("bjj data: requestCallPhone " + data)
        api.requestCallPhone(data)
    }

    @JavascriptInterface
    fun requestExternalWeb(jsonData: String) {
        val data = gson().fromJson(jsonData, RequestExternalWebInfo::class.java)
        DLog.e("bjj data: requestExternalWeb " + data)
        api.requestExternalWeb(data)
    }

    @JavascriptInterface
    fun openSharePopup(jsonData: String) {
        val data = gson().fromJson(jsonData, OpenSharePopupInfo::class.java)
        DLog.e("bjj data: openSharePopup " + jsonData)
        api.openSharePopup(data.text)
    }

    @JavascriptInterface
    fun pageClearHistory() {
        DLog.e("bjj data: pageClearHistory ")
        api.pageClearHistory()
    }
}