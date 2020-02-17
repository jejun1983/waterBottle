package com.idevel.waterbottle.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.idevel.waterbottle.R
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA
import com.idevel.waterbottle.utils.DLog
import com.idevel.waterbottle.utils.SharedPreferencesUtil
import com.idevel.waterbottle.web.UrlData.NORMAL_SERVER_URL
import kotlinx.android.synthetic.main.activity_dev.*

class DevActivity : AppCompatActivity(), View.OnClickListener {

    private var buttonOk: Button? = null
    private var buttonServerChange: Button? = null
    private var textviewServerUrl: TextView? = null
    private var buttonCTNChange: Button? = null
    private var textviewCTN: TextView? = null

    var urlList = ArrayList<String>()
    var ctnList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dev)

        initView()

        initURL()

        if (enableGetCTN()) {
            initCTN()
        } else {
            textviewCTN?.text = "권한허용 후 CTN 설정 가능합니다"
            buttonCTNChange?.visibility = View.GONE
        }
    }

    private fun initView() {
        buttonOk = findViewById<View>(R.id.button) as Button
        buttonServerChange = findViewById<View>(R.id.btn_server_change) as Button
        textviewServerUrl = findViewById<View>(R.id.textview_server_url) as TextView
        buttonCTNChange = findViewById<View>(R.id.btn_ctn_change) as Button
        textviewCTN = findViewById<View>(R.id.textview_ctn) as TextView

        buttonOk?.setOnClickListener(this)
        buttonServerChange?.setOnClickListener(this)
        buttonCTNChange?.setOnClickListener(this)

        btn_popup_test.setOnClickListener(this)
    }

    private fun initURL() {
        urlList.add(NORMAL_SERVER_URL)
//        urlList.add(DEV_SERVER_URL2)
//        urlList.add(DEV_SERVER_URL3)
//        urlList.add(DEV_SERVER_URL4)
        var url: String? = SharedPreferencesUtil.getString(this, SharedPreferencesUtil.Cmd.MAIN_URL)

        if (null == url || url.equals("", ignoreCase = true)) {
            url = NORMAL_SERVER_URL
        }

        textviewServerUrl?.text = url
    }

    private fun initCTN() {
        ctnList.add(getCTN())

        var ctn: String? = SharedPreferencesUtil.getString(this, SharedPreferencesUtil.Cmd.DEV_LAST_USED_CTN)
        if (null == ctn || ctn.equals("", ignoreCase = true)) {
            ctn = getCTN()
        }

        textviewCTN?.text = ctn
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button -> {
                val url = textviewServerUrl?.text.toString()
                if (null == url || url.isEmpty()) {
                    Toast.makeText(this, "서버주소를선택하세요", Toast.LENGTH_SHORT).show()
                    return
                }
                SharedPreferencesUtil.setString(this, SharedPreferencesUtil.Cmd.MAIN_URL, textviewServerUrl?.text.toString())
                if (enableGetCTN()) {
                    SharedPreferencesUtil.setString(this, SharedPreferencesUtil.Cmd.DEV_LAST_USED_CTN, textviewCTN?.text.toString())
                }
                finish()
            }
            R.id.btn_server_change -> showSelectDialog("URL", urlList, textviewServerUrl)
            R.id.btn_ctn_change -> showSelectDialog("CTN", ctnList, textviewCTN)
            R.id.btn_popup_test -> popupTest()
        }
    }

    private fun showSelectDialog(title: String, list: ArrayList<String>, textView: TextView?) {
        val alertBuilder = AlertDialog.Builder(
                this@DevActivity)
        alertBuilder.setTitle("$title 선택")

        // List Adapter 생성
        val adapter = ArrayAdapter<String>(
                this@DevActivity,
                android.R.layout.select_dialog_singlechoice)
        for (s in list) {
            adapter.add(s)
        }
        adapter.add("직접입력")

        // 버튼 생성
        alertBuilder.setNegativeButton("취소"
        ) { dialog, _ -> dialog.dismiss() }

        // Adapter 셋팅
        alertBuilder.setAdapter(adapter
        ) { _, id ->
            // AlertDialog 안에 있는 AlertDialog
            val strName = adapter.getItem(id)

            if ("직접입력".equals(strName, ignoreCase = true)) {
                showEtcDialog(title, textView)
            } else {
                textView?.text = strName
            }
        }
        alertBuilder.show()
    }

    private fun showEtcDialog(title: String, textView: TextView?) {
        val alertBuilder = AlertDialog.Builder(
                this@DevActivity)
        alertBuilder.setTitle("$title 입력")

        val edit = EditText(this)
        edit.setText(textView?.text)
        alertBuilder.setView(edit)

        alertBuilder.setNegativeButton("취소"
        ) { dialog, _ -> dialog.dismiss() }
        alertBuilder.setPositiveButton("확인"
        ) { dialog, _ ->
            textView?.text = edit.text.toString()
            dialog.dismiss()
        }
        alertBuilder.show()
    }


    private fun enableGetCTN(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }

        return true
    }

    private fun getCTN(): String {
        val mgr = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return ""
                }
            }
            var userPhone: String? = mgr.line1Number
            if (null == userPhone) {
                DLog.e("getLine1Number is null")
                return ""
            }
            userPhone = userPhone.replace("+82", "0")
            return userPhone
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ""
    }

    fun popupTest() {
        val data = Bundle()

        // test1
//        data.putString("app_in", "{\"agnt_disp_cd\":\"T\",\"agnt_pop_yn\":\"Y\",\"agnt_img\":\"\",\"agnt_link_dv_cd\":\"001\",\"agnt_cnts\":\"텍스트 에이전트 팝업 내용\",\"agnt_link_url\":\"\"}")
//        data.putString("app_out", "{\"noti_cnts\":\"푸시 내용~\",\"noti_img\":\"\",\"noti_idct_hist_yn\":\"Y\",\"noti_title\":\"푸시 제목\"}")

        // test2
//        data.putString("app_in", "{\"agnt_disp_cd\":\"I\",\"agnt_pop_yn\":\"Y\",\"agnt_img\":\"https://image.shutterstock.com/image-vector/sample-stamp-grunge-texture-vector-260nw-1389188327.jpg\",\"agnt_link_dv_cd\":\"001\",\"agnt_cnts\":\"http://www.google.com\",\"agnt_link_url\":\"\"}")
//        data.putString("app_out", "{\"noti_cnts\":\"선불 사용기간 1일 남았습니다.\",\"noti_img\":\"\",\"noti_idct_hist_yn\":\"N\",\"noti_title\":\"충전알림\"}")


        //test
//        noti_img = "https://image.shutterstock.com/image-vector/sample-stamp-grunge-texture-vector-260nw-1389188327.jpg"
//        noti_img = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTc1WPZGHbBfZxkNhe2AK5732SQ0uwq0qDWX5BaciUANgTkAsnz"


        // popup
        val i = Intent(applicationContext, PushPopupActivity::class.java)
        i.putExtra(PUSH_DATA, data)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(i)
//        Handler().postDelayed({
//            startActivity(i)
//        }, 5000)
    }
}
