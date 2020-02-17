package com.idevel.waterbottle.fcm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import com.idevel.waterbottle.utils.DLog
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.URL

/**
 * Created by djsworld on 2017-08-03.
 */

class PushDataParser {

    companion object {
        enum class PushPopupType {
            IMAGE,
            IMAGE_TEXT,
            TEXT
        }

        enum class AgentPopupType {
            NONE,
            IMAGE,
            TEXT,
            WEBVIEW
        }
    }

    var isUseNoti: Boolean = false
        private set      // 노티 사용여부
    var isUseNotiImage: Boolean = false
        private set  // app 외 이미지 사용 여부

    lateinit var typePushPopup: PushPopupType
    lateinit var typeAgentPopup: AgentPopupType

    var isUseAgntImage: Boolean = false
        private set   // app 내 이미지 사용 여부
    var isUseInPopup: Boolean = false
        private set   // app 내 팝업 사용 여부
    var dataType: String? = null
        private set   // app 내 팝업 webview 사용 여부
    var isLink: Boolean = false
        private set   // 링크 유무

    var notiImage: Bitmap? = null
        private set
    var image: Bitmap? = null
        private set

    // 노티, app 외 팝업
    private var noti_idct_hist_yn: String? = null   // noti 사용여부
    var notiTitle: String? = null
        private set          // 타이틀
    var notiContents: String? = null
        private set           // 내용
    private var noti_img: String? = null            // 이미지 url

    // app 내 실행
    private var agnt_pop_yn: String? = null     // 팝업 사용여부
    private var agnt_disp_cd: String? = null    // 노출방식 (T:text, W:웹뷰)

    var contents: String? = null
        private set       // 내용 (text, url)
    var imageUrl: String? = null
        private set        // 이미지 url
    var linkCode: String? = null
        private set // 링크구분 (001:링크없음, 002,003:링크있음)
    var linkUrl: String? = null
        private set   // 링크 url


    fun setData(data: Bundle) {

        var app_out: JSONObject? = null
        var app_in: JSONObject? = null
        try {
            app_out = JSONObject(data.get("app_out") as String)
            app_in = JSONObject(data.get("app_in") as String)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        try {
            DLog.i("app_out : " + app_out.toString())
            DLog.i("app_in : " + app_in.toString())

            // 노티, 팝업
            noti_idct_hist_yn = app_out?.getString("noti_idct_hist_yn")
            notiTitle = app_out?.getString("noti_title")
            notiContents = app_out?.getString("noti_cnts")
            noti_img = app_out?.getString("noti_img")

            // 앱이 떠있을 경우 내부 팝업. 이때 노티는 무시
            agnt_pop_yn = app_in?.getString("agnt_pop_yn")
            agnt_disp_cd = app_in?.getString("agnt_disp_cd")
            contents = app_in?.getString("agnt_cnts")
            imageUrl = app_in?.getString("agnt_img")
            linkCode = app_in?.getString("agnt_link_dv_cd")
            linkUrl = app_in?.getString("agnt_link_url")

            // 노티 사용 여부.  노티 팝업은 띄움
            if (noti_idct_hist_yn != null && noti_idct_hist_yn?.equals("Y", true) == true) {
                isUseNoti = true
            }
            // 노티 이미지 여부
            if (noti_img != null && noti_img != "") {
                isUseNotiImage = true
                val imageThread1 = object : Thread() {
                    override fun run() {
                        notiImage = getBitmap(noti_img)
                    }
                }
                imageThread1.start()
                try {
                    imageThread1.join()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            // 푸시 팝업 타입
            typePushPopup = if(isUseNotiImage){
                if(!notiTitle.isNullOrBlank() && !notiContents.isNullOrBlank()) {
                    PushPopupType.IMAGE_TEXT
                } else {
                    PushPopupType.IMAGE
                }
            } else {
                PushPopupType.TEXT
            }


            // 내부 팝업 사용 여부
            if (agnt_pop_yn != null && agnt_pop_yn.equals("Y", true)) {
                isUseInPopup = true
            }
            if(isUseInPopup) {
                // 팝업 이미지
                if (imageUrl != null && imageUrl != "") {

                    //test
                    //                agnt_img = "https://s13.favim.com/orig/161201/black-pink-lisa-rose-jisoo-Favim.com-4907428.png";

                    isUseAgntImage = true
                    val imageThread2 = object : Thread() {
                        override fun run() {
                            image = getBitmap(imageUrl)
                        }
                    }
                    imageThread2.start()
                    try {
                        imageThread2.join()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                }
                dataType = agnt_disp_cd
                if (linkCode != null && (linkCode == "002" || linkCode == "003")) {
                    isLink = true
                }
            }

            // 에이전트 팝업 타입
            typeAgentPopup = if(isUseInPopup){
                if(!imageUrl.isNullOrBlank()){
                    AgentPopupType.IMAGE
                } else {
                    when(agnt_disp_cd) {
                        "T" -> AgentPopupType.TEXT
                        "W" -> AgentPopupType.WEBVIEW
                        else -> AgentPopupType.NONE
                    }
                }
            } else {
                AgentPopupType.NONE
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    private fun getBitmap(strUrl: String?): Bitmap? {
        try {
            val url = URL(strUrl)
            val conn = url.openConnection()
            conn.connect()

            val bis = BufferedInputStream(conn.getInputStream())

            return BitmapFactory.decodeStream(bis)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
