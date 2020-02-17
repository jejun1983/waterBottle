package com.idevel.waterbottle.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.*
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.Gson
import com.idevel.waterbottle.R
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_BODY
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_IMAGE
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_LINK_CODE
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_LINK_URL
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_TITLE
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_TYPE
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_USE_IN_POPUP
import com.idevel.waterbottle.fcm.PushDataParser
import com.idevel.waterbottle.notification.PushNotification
import com.idevel.waterbottle.notification.PushNotification.sendNotification
import com.idevel.waterbottle.utils.DLog
import com.idevel.waterbottle.utils.isAppRunning
import org.json.JSONObject

/**
 * Created by djsworld on 2017-07-17.
 */

class PushPopupActivity : Activity(), View.OnClickListener {

    private lateinit var parser: PushDataParser

    private var isScreenOn = true

    /**
     * 메인 액티비티를 실행하는 인텐트 생성
     */
    private val mainActivityIntent: Intent
        get() {
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (parser.isUseInPopup) {
                i.putExtra(PUSH_USE_IN_POPUP, "Y")

                DLog.v("-- InApp Popup Data --")
                DLog.v("ImageUrl : " + parser.imageUrl)
                DLog.v("Title : " + parser.notiTitle)
                DLog.v("Body : " + parser.contents)
                DLog.v("getDataType : " + parser.dataType)

                i.putExtra(PUSH_DATA_IMAGE, parser.imageUrl)
                i.putExtra(PUSH_DATA_TITLE, parser.notiTitle)
                i.putExtra(PUSH_DATA_BODY, parser.contents)
                i.putExtra(PUSH_DATA_TYPE, parser.dataType)
                if (parser.isLink) {
                    i.putExtra(PUSH_DATA_LINK_CODE, parser.linkCode)
                    i.putExtra(PUSH_DATA_LINK_URL, parser.linkUrl)
                }
            } else {
                i.putExtra(PUSH_USE_IN_POPUP, "N")
            }

            return i
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        parser = PushDataParser()
        parser.setData(intent.getBundleExtra(PUSH_DATA))
        DLog.i("parser:${Gson().toJson(parser)}")

        // 화면 꺼져있는 경우 잠금화면 상태에서 팝업 띄움
        if (!isScreenOn()) {
            isScreenOn = false
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        setNotification()
        setPushPopup()

        window.attributes.width = WindowManager.LayoutParams.MATCH_PARENT

        findViewById<Button>(R.id.btn_1_close)?.setOnClickListener(this)
        findViewById<Button>(R.id.btn_2_close)?.setOnClickListener(this)
        findViewById<Button>(R.id.btn_2_ok)?.setOnClickListener(this)

    }


    /**
     * 단말기 화면이 켜져있는지 여부
     */
    private fun isScreenOn(): Boolean {
        return try {
            val powerManager = this.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                powerManager.isScreenOn
            } else {
                powerManager.isInteractive
            }
        } catch (e: NullPointerException) {
            false
        }

    }

    /**
     * Notification 설정
     */
    private fun setNotification() {
        if (parser.isUseNoti) {
            if (parser.isUseNotiImage) {
                sendNotification(this, mainActivityIntent, parser.notiTitle, parser.notiContents, parser.notiImage)
            } else {
                sendNotification(this, mainActivityIntent, parser.notiTitle, parser.notiContents)
            }
//            // 화면이 켜져있는 경우 noti만 띄우고 팝업은 띄우지 않는다
//            if (isScreenOn) {
//                DLog.d("isScreenOn... finish")
//                finish()
//            }
        } else {
            setNotiSound()
        }
    }

    private fun setNotiSound() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone.play()
        } else if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val millisecond: Long = 700
            vibrator.vibrate(millisecond)
        }
    }

    /**
     * 팝업 설정
     */
    private fun setPushPopup() {
        DLog.d("isScreenOn : $isScreenOn")
        if (isAppRunning(this) && isScreenOn) {
            if (parser.isUseInPopup) {
                // 앱 실행중, 내부팝업 사용인 경우 내부팝업만 띄움
                // 내부팝업 데이터 전달
                // MainActivity onNewIntent에서 처리
                startActivity(mainActivityIntent)
                finish()
            } else {
                // 앱 실행중, 내부팝업 미사용인 경우 외부팝업을 띄움. 버튼 한개
                // MainActivity onNewIntent에서 처리
                initLayout(true)
            }
        } else {
            // 앱 실행중이 아닌경우 외부팝업 띄움. 버튼 두개
            initLayout(false)
        }
    }


    /**
     * 레이아웃 초기화
     */
    private fun initLayout(useOneButton: Boolean) {
        if (parser.isUseNotiImage) {
            if (!parser.notiTitle.isNullOrBlank() && !parser.notiContents.isNullOrBlank()) {
                setContentView(R.layout.dialog_type_imagetext)
            } else {
                setContentView(R.layout.dialog_type_image)
            }
            (findViewById<View>(R.id.image) as ImageView).setImageBitmap(parser.notiImage)
        } else {
            setContentView(R.layout.dialog_type_text)
        }

        (findViewById<View>(R.id.text_title) as TextView?)?.text = parser.notiTitle
        (findViewById<View>(R.id.text_body) as TextView?)?.text = parser.notiContents

        if (useOneButton) {
            findViewById<Button>(R.id.btn_1_close)?.visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.layout_btn_2)?.visibility = View.GONE
        } else {
            findViewById<Button>(R.id.btn_1_close)?.visibility = View.GONE
            findViewById<LinearLayout>(R.id.layout_btn_2)?.visibility = View.VISIBLE
        }
    }

    /**
     * push data 확인 테스트
     */
    private fun initTest() {

        val body = findViewById<TextView>(R.id.text_body)
        body.text = ""

        val buttonOk = findViewById<Button>(R.id.btn_cancel)
        buttonOk.setOnClickListener(this)


        val data = intent.getBundleExtra(PUSH_DATA)

        for (key in data.keySet()) {
            try {
                val s = data.getString(key)
                DLog.v("$key : $s")
                val jsonObject = JSONObject(s)

                val i = jsonObject.keys()
                while (true) {
                    if (!i.hasNext()) break

                    val k = i.next()

                    val log = "[" + key + "]" + k + " : " + jsonObject.getString(k)
                    DLog.e(log)

                    body.text = body.text.toString() + log + "\n"
                }

            } catch (e: Exception) {

            }

        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_2_ok -> {
                PushNotification.clearNotification(this)
                startActivity(mainActivityIntent)
                finish()
            }
            R.id.btn_1_close, R.id.btn_2_close -> finish()
        }
    }

    override fun onResume() {
        // 꺼진 화면에서 팝업 뜬 경우 몇 초 후 닫음
        if (!isScreenOn) {
            Handler().postDelayed({ finish() }, 7000)
        }
        super.onResume()
    }

}
