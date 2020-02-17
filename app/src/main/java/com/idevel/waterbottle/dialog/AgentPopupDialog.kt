package com.idevel.waterbottle.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import com.idevel.waterbottle.R
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_BODY
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_IMAGE
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_TITLE
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_TYPE
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_TYPE_IMAGE
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_TYPE_WEB
import com.idevel.waterbottle.utils.DLog
import java.io.BufferedInputStream
import java.net.URL

/**
 * Created by djsworld on 2017-08-04.
 */

class AgentPopupDialog(mContext: Context, intent: Intent) : Dialog(mContext, android.R.style.Theme_Translucent_NoTitleBar), View.OnClickListener {

    private var type: String? = null
    private var title: String? = null
    private var body: String? = null
    private var imgUrl: String? = null
    private var linkCode: String? = null
    private var linkUrl: String? = null

    private var okListener: View.OnClickListener? = null

    private var image: Bitmap? = null

    init {
        setData(intent)
    }

    private fun setData(intent: Intent) {

        type = intent.getStringExtra(PUSH_DATA_TYPE)
        title = intent.getStringExtra(PUSH_DATA_TITLE)
        body = intent.getStringExtra(PUSH_DATA_BODY)
        imgUrl = intent.getStringExtra(PUSH_DATA_IMAGE)
//        linkCode = intent.getStringExtra(PUSH_DATA_LINK_CODE)
//        linkUrl = intent.getStringExtra(PUSH_DATA_LINK_URL)

        DLog.v("PushPopup type : $type")
        DLog.v("PushPopup title : $title")
        DLog.v("PushPopup body : $body")
        DLog.v("PushPopup imgUrl : $imgUrl")
//        DLog.v("PushPopup linkCode : $linkCode")
//        DLog.v("PushPopup linkUrl : $linkUrl")

        if (type == null) {
            type = ""
            return
        }

        if (type.equals(PUSH_DATA_TYPE_IMAGE, ignoreCase = true)) {
            val imageThread1 = object : Thread() {
                override fun run() {

                    try {
                        val url = URL(imgUrl)
                        val conn = url.openConnection()
                        conn.connect()

                        val bis = BufferedInputStream(conn.getInputStream())
                        image = BitmapFactory.decodeStream(bis)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }
            imageThread1.start()
            try {
                imageThread1.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 다이얼로그 외부 화면 흐리게 표현
        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.8f
        window.attributes = lpWindow

        if (null == image && null == title && null == body) {
            DLog.e("Popup Data Error")
            return
        }

        if (type.equals(PUSH_DATA_TYPE_IMAGE, ignoreCase = true) && image != null) {
            DLog.i("Set Layout dialog_in_app_image")
            // 이미지 팝업
            setContentView(R.layout.dialog_type_image)
            (findViewById<View>(R.id.image) as ImageView).setImageBitmap(image)
        } else if (type.equals(PUSH_DATA_TYPE_WEB, ignoreCase = true)) {
            DLog.i("Set Layout dialog_in_app_webview")
            // 웹뷰 팝업
            setContentView(R.layout.dialog_type_web)

            (findViewById<View>(R.id.webview_popup) as WebView).webViewClient = WebViewClient()
            (findViewById<View>(R.id.webview_popup) as WebView).loadUrl(body)
        } else {
            DLog.i("Set Layout dialog_in_app_text")
            // 텍스트 팝업
            setContentView(R.layout.dialog_type_text)
            (findViewById<View>(R.id.text_title) as TextView).text = title
            (findViewById<View>(R.id.text_body) as TextView).text = body
        }

//        if (linkUrl != null && linkCode != null && (linkCode.equals("002", ignoreCase = true) || linkCode.equals("003", ignoreCase = true))) {
//            findViewById<View>(R.id.btn_1_close).visibility = View.GONE
//            findViewById<View>(R.id.layout_btn_2).visibility = View.VISIBLE
//
//            findViewById<View>(R.id.btn_2_close).setOnClickListener(this)
//            findViewById<View>(R.id.btn_2_ok).setOnClickListener(okListener)
//        } else {
            findViewById<View>(R.id.btn_1_close).visibility = View.VISIBLE
            findViewById<View>(R.id.layout_btn_2).visibility = View.GONE

            findViewById<View>(R.id.btn_1_close).setOnClickListener(this)
//        }

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_1_close, R.id.btn_2_close -> dismiss()
        }
    }

    override fun dismiss() {
        (findViewById<View>(R.id.webview_popup) as WebView?)?.destroy()
        super.dismiss()
    }

    fun setOkClickListener(clickListener: View.OnClickListener) {
        okListener = clickListener
    }

}
