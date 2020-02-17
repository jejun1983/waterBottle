package com.idevel.waterbottle.activity


import ApiManager
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.Window
import android.webkit.*
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.Constants
import com.anjlab.android.iab.v3.TransactionDetails
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions
import com.idevel.waterbottle.MyApplication
import com.idevel.waterbottle.R
import com.idevel.waterbottle.broadcast.DataSaverChangeReceiver
import com.idevel.waterbottle.broadcast.NetworkChangeReceiver
import com.idevel.waterbottle.broadcast.SimStateChangeReceiver
import com.idevel.waterbottle.dialog.AgentPopupDialog
import com.idevel.waterbottle.dialog.CustomAlertDialog
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_LINK_CODE
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA_LINK_URL
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_USE_IN_POPUP
import com.idevel.waterbottle.interfaces.IDataSaverListener
import com.idevel.waterbottle.interfaces.NetworkChangeListener
import com.idevel.waterbottle.interfaces.SimStateChangeListener
import com.idevel.waterbottle.utils.*
import com.idevel.waterbottle.utils.wrapper.LocaleWrapper
import com.idevel.waterbottle.web.BaseWebView
import com.idevel.waterbottle.web.MyWebChromeClient
import com.idevel.waterbottle.web.MyWebViewClient
import com.idevel.waterbottle.web.UrlData.getMainUrl
import com.idevel.waterbottle.web.constdata.*
import com.idevel.waterbottle.web.interfaces.IWebBridgeApi
import java.lang.ref.WeakReference
import java.net.URISyntaxException

/**
 * main activity class.
 *
 * @author : jjbae
 */
class MainActivity : FragmentActivity(), BillingProcessor.IBillingHandler {
    private var mSplashView: View? = null //초기 로딩시 보여주기 위한 view.
    private var mErrorView: View? = null //The network error view.
    private var mWebview: BaseWebView? = null //mobile view page 연결을 위한 webview.
    private var mWebViewClient: MyWebViewClient? = null //The web view client.
    private var mWebChromeClient: MyWebChromeClient? = null //The web chrome client.
    private var isRestartApp = false //The is restart app.

    private var mWebviewSub: RelativeLayout? = null // sub webView parent

    //앱 내 push popup
    private var mAgentPopupDialog: AgentPopupDialog? = null
    private var linkCode: String? = null
    private var linkUrl: String? = null
    private val mHandler = WeakHandler(this)

    private var mReTry: Int = 0

    //billing
    private var billing_test_btn: Button? = null
    private var bp: BillingProcessor? = null

    // PUSH 팝업 버튼 클릭리스너
    private val clickListener = OnClickListener {
        if (linkCode == null || linkUrl == null || linkUrl.equals("", ignoreCase = true)) {
            return@OnClickListener
        }
        if (linkCode!!.equals("002", ignoreCase = true) && null != mWebview) {
            mWebview!!.loadUrl(linkUrl)
        } else if (linkCode!!.equals("003", ignoreCase = true)) {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
            startActivity(i)
        }
        mAgentPopupDialog?.dismiss()
    }

    private val action: Action
        get() = Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject("Main Page", "android-app://com.idevel.waterbottle/http/host/path", "http://host/path")
                .build()

    private var mApiManager: ApiManager? = null
    private val mNetworkChangeReceiver = NetworkChangeReceiver() //네트워크 check
    private val mDataSaverChangeReceiver = DataSaverChangeReceiver()
    private val mSimStateChangeReceiver = SimStateChangeReceiver()


    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        mApiManager = ApiManager.getInstance(this@MainActivity)
        CookieSyncManager.createInstance(this@MainActivity)

        bp = BillingProcessor(this@MainActivity, BILLING_LICENS_KEY, this@MainActivity)
        bp?.initialize()
        billing_test_btn = findViewById(R.id.billing_test_btn)
        billing_test_btn?.setOnClickListener {
            val isPurchase = SharedPreferencesUtil.getBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH)

//            if (isPurchase) {
//            } else {
            bp?.subscribe(this@MainActivity, BILLING_SUBSCRIBE_MONTH_PRODUCT_ID)
//            }
        }

        mWebviewSub = findViewById(R.id.webview_sub)
        mWebview = findViewById(R.id.webview_main)
        mSplashView = findViewById(R.id.view_splash)
        mSplashView?.visibility = View.VISIBLE
        mErrorView = findViewById(R.id.view_error)
        mErrorView?.visibility = View.GONE

        cleanCookie()

        //네트워크 연결 여부
        if (!isNetworkConnected(this)) {
            showErrorDlg(NETWORK_CONNECTION_ERROR)
            return
        }

        //data saver 설정 여부
        if (checkDataSave(this)) {
            showDataSaveDlg(R.string.popup_title_data_save, R.string.popup_msg_data_save)
            return
        }

        //usim 장착 여부
        if (!hasUsim(this@MainActivity)) {
            showErrorDlg(USIM_MOUNTED)
            return
        }

        // 앱 사용 중 data saver 사용 Listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mDataSaverChangeReceiver.setListener(dataSaverListener)
        }

        // 앱 사용 중 data network 변경 Listener
        mNetworkChangeReceiver.setListener(networkListener)

        // 앱 사용 중 usim state 변경 Listener
        mSimStateChangeReceiver.setListener(mSimStateChangeListener)

        //debug build인 경우 히든 메뉴 이동
//        if (BuildConfig.DEBUG && !MyApplication.instance.isChangeLanguage) {
//            goToDevActivity()
//        } else {
        checkSettingInfo()
//        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        checkPushData(intent)
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(mDataSaverChangeReceiver, IntentFilter(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED))
        }

        registerReceiver(mNetworkChangeReceiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
        registerReceiver(mSimStateChangeReceiver, IntentFilter("android.intent.action.SIM_STATE_CHANGED"))

        mWebview?.onResume()
    }

    override fun onPause() {
        super.onPause()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            unregisterReceiver(mDataSaverChangeReceiver)
        }

        unregisterReceiver(mNetworkChangeReceiver)
        unregisterReceiver(mSimStateChangeReceiver)

        mWebview?.onPause()
    }

    private fun checkSettingInfo() {
//        val url = URL(SETTING_URL)
//        mApiManager?.getSettingInfo(url.toString(), object : OnResultListener<Any> {
//            override fun onResult(result: Any, flag: Int) {
//                if (result == null) {
//                    return
//                }
//
//                val data = result as SettingInfoData
//
//                DLog.e("bjj checkSettingInfo :: "
//                        + data.items[0].app_version + " ^ "
//                        + data.items[0].main_url)
//
//                NORMAL_SERVER_URL = data.items[0].main_url
//
//                if (data.items[0].app_version.isNullOrEmpty()) {
//                    appVersionCal("", true)
//                } else {
//                    appVersionCal(data.items[0].app_version ?: "")
//                }
//            }
//
//            override fun onFail(error: Any, flag: Int) {
//                appVersionCal("", true)
//            }
//        })

        //TODO test
        mHandler.sendEmptyMessageDelayed(HANDLER_SPLASH, 3000L)
    }

    private fun appVersionCal(version: String, isError: Boolean = false) {
        if (isError) {
            showAlertDlg(0, R.string.popup_msg_error_version, APP_VERSION_CHECK)
        } else {
            mReTry = 0

            val serverVersion = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val appVersion = getVersionName(this)!!.split("\\.".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()

            DLog.d("bjj appVersionCal "
                    + appVersion?.size + " ^ "
                    + serverVersion[0] + " ^ "
                    + serverVersion[1] + " ^ "
                    + serverVersion[2] + " ^ "
                    + appVersion[0]!! + " ^ "
                    + appVersion[1]!! + " ^ "
                    + appVersion[2]!!)

            if (Integer.parseInt(appVersion[0] + appVersion[1]) < Integer.parseInt(serverVersion[0] + serverVersion[1])) {
                //MAJOR
                DLog.d("bjj appVersionCal MAJOR")
                showOtherAppVersionDlg()

            } else if (Integer.parseInt(appVersion[2]) < Integer.parseInt(serverVersion[2]) &&
                    Integer.parseInt(appVersion[0] + appVersion[1]) == Integer.parseInt(serverVersion[0] + serverVersion[1])) {
                //MINOR
                DLog.d("bjj appVersionCal MINOR")
                showOtherAppVersionDlg()
            } else {
                DLog.d("bjj appVersionCal NOTTING")
                mHandler.sendEmptyMessage(HANDLER_SPLASH)
            }
        }
    }

    /**
     * 스플래시 애니메이션 설정
     */
    private fun setSplash() {
//        val iv = findViewById<ImageView>(R.id.splash_bg)
//        var anim: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.splash_scale_anim)
//
//        anim.setAnimationListener(object : Animation.AnimationListener {
//            override fun onAnimationRepeat(animation: Animation?) {
//            }
//
//            override fun onAnimationEnd(animation: Animation?) {
//                showMainView()
//            }
//
//            override fun onAnimationStart(animation: Animation?) {
//            }
//        })
//
//        iv.startAnimation(anim)

        showMainView()
    }

    /**
     * Sets the main view.
     */
    private fun setMainView() {
        if (mWebview == null) {
            return
        }

        mHandler.sendEmptyMessageDelayed(HANDLER_NETWORK_TIMER, PING_TIME.toLong())

        mWebview!!.setBackgroundColor(Color.WHITE)
        mWebview!!.setJSInterface(iWebBridgeApi)

//        val param = Utils.getPostData(this)
//        mWebview!!.postUrl(getMainUrl(this@MainActivity), param.toByteArray())

        mWebview!!.loadUrl(getMainUrl(this@MainActivity))
        mWebViewClient = object : MyWebViewClient(this) {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                DLog.e("bjj mWebViewClient onPageStarted : $url")
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                //TODO test
                removeSplash()

                DLog.e("bjj mWebViewClient onPageFinished : $url, ${getMainUrl(this@MainActivity)}")
            }

            override fun showErrorPage() {
                DLog.e("bjj mWebViewClient showErrorPage : ")
                showErrorView()
            }

            override fun setUntouchableProgress(visible: Int) {
                DLog.e("bjj mWebViewClient setUntouchableProgress : $visible")
            }

            @SuppressWarnings("deprecation")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return urlLoading(view, Uri.parse(url))
            }


            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    urlLoading(view, request?.getUrl())
                } else {
                    super.shouldOverrideUrlLoading(view, request)
                }
            }
        }

        mWebChromeClient = object : MyWebChromeClient(this, findViewById<View>(R.id.mainview) as RelativeLayout) {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (View.GONE == mSplashView?.visibility) {
                    super.onProgressChanged(view, newProgress)
                }
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                return super.onJsAlert(view, url, message, result)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                return super.onConsoleMessage(consoleMessage)
            }
        }

        mWebview!!.webViewClient = mWebViewClient
        mWebview!!.webChromeClient = mWebChromeClient
    }

    private fun checkPushData(intent: Intent) {
        // 푸시를 통해 진입한 경우가 아님
        if (null == intent.extras || null == intent.getStringExtra(PUSH_USE_IN_POPUP)) {
            return
        }

        // 8.0미만 버전에서 뱃지 카운트 0 으로 설정
        setIconBadge(this, 0)

        // 내부 팝업 사용하지 않는 경우
        if (intent.getStringExtra(PUSH_USE_IN_POPUP).equals("N", ignoreCase = true)) {
            return
        }

        linkCode = intent.getStringExtra(PUSH_DATA_LINK_CODE)
        linkUrl = intent.getStringExtra(PUSH_DATA_LINK_URL)

        if (mAgentPopupDialog != null && mAgentPopupDialog?.isShowing == true) {
            mAgentPopupDialog!!.dismiss()
        }

        mAgentPopupDialog = AgentPopupDialog(this, intent)
        mAgentPopupDialog?.setOkClickListener(clickListener)
        mAgentPopupDialog?.show()
    }

    /**
     * Show error view.
     */
    fun showErrorView() {
        mErrorView?.visibility = View.VISIBLE
        val homeBtn = mErrorView?.findViewById<Button>(R.id.homeBtn)
        homeBtn?.setOnClickListener { finish() }
    }

    /**
     * Show main view.
     */
    private fun showMainView() {
        DLog.e("bjj showMainView : ")

        mHandler.removeMessages(HANDLER_NETWORK_TIMER)

        mSplashView?.visibility = View.GONE
        mErrorView?.visibility = View.GONE
        mWebview?.visibility = View.VISIBLE
//        mInitFlag = false

        // 푸시팝업 통해서 실행한 것인지 체크
        checkPushData(intent)

        //test
        //        showPopuopDialog("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSdjbqS-6mBO66GP5likT4MvbtBEgEOVpQC79UtQZBEtGS4Xwu1Gw", null, null);
        //        showPopuopDialog("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR69FT94A3ocqYlXC-sTZNrCTMUtqWTZkkFglh3LdxeoNDJixDb-A", null, null);
    }

    /**
     * Show other app version dlg.
     */
    private fun showOtherAppVersionDlg() {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog?.dismiss()
            when (v.id) {
                R.id.btn_ok -> gotoPlayStore()
                R.id.btn_cancel -> finish()
            }
        })

        if (!isFinishing && !isDestroyed) {
            alertDialog?.show()
        }
    }

    private fun showDataSaveDlg(title: Int, content: Int) {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setCancelable(false)
        alertDialog?.setDataSaveLayout(title, content)
        alertDialog?.setButtonString(R.string.popup_btn_ok_dta_save, R.string.popup_btn_cancel_dta_save)

        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog!!.dismiss()
            when (v.id) {
                R.id.btn_cancel -> finish()
                R.id.btn_ok -> {
                    val intent = Intent()
                    intent.action = Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                    finish()
                }
            }
        })

        if (!isFinishing) {
            alertDialog?.show()
        }
    }

    /**
     * Show alert dlg.
     */
    private fun showAlertDlg(title: Int, content: Int, errorType: Int) {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setErrorLayout(title, content)

        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog?.dismiss()

            if (v.id == R.id.btn_error) {
                when (errorType) {
                    NETWORK_CONNECTION_ERROR, TIMEOUT_ERROR, USIM_MOUNTED -> {
                        finish()
                    }
                    APP_VERSION_CHECK -> {
                        if (mReTry > 2) {
                            finish()
                        } else {
                            mReTry++
                            checkSettingInfo()
                        }
                    }
                    else -> {
                    }
                }
            }
        })

        if (!isFinishing) {
            alertDialog?.show()
        }
    }

    /**
     * Show PermissionDenyDialog dlg.
     */
    private fun showPermissionDenyDialog() {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setCancelable(false)
        alertDialog?.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg)
        alertDialog?.setButtonString(R.string.popup_btn_ok_dta_save, R.string.popup_btn_cancel_dta_save)

        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog!!.dismiss()
            when (v.id) {
                R.id.btn_cancel -> finish()
                R.id.btn_ok -> {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                    finish()
                }
            }
        })

        if (!isFinishing) {
            alertDialog?.show()
        }
    }

    /**
     * Show network error dlg.
     *
     * @param errorType the error type
     */
    private fun showErrorDlg(errorType: Int) {
        var titleRes = R.string.popup_title_server_error
        var msgRes = R.string.popup_msg_server_error

        when (errorType) {
            NETWORK_CONNECTION_ERROR -> {
                titleRes = R.string.popup_title_network_error
                msgRes = R.string.popup_msg_network_error
            }
            TIMEOUT_ERROR -> {
                titleRes = R.string.popup_title_server_error
                msgRes = R.string.popup_msg_server_error
            }
            USIM_MOUNTED -> {
                titleRes = R.string.usim_mount_error_title
                msgRes = R.string.usim_mount_error_msg
            }
            else -> {
            }
        }

        showAlertDlg(titleRes, msgRes, errorType)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            DLog.e("bjj onKeyDown ==>> "
                    + backPressFlag + " ^ "
                    + mErrorView?.visibility + " ^ "
                    + mWebview!!.canGoBack())

            mWebview?.let {
                return if (it.canGoBack()) {
                    it.goBack()

                    false
                } else {
                    if (!backPressFlag) {
                        mHandler.sendEmptyMessageDelayed(HANDLER_APP_FINISH, 2000)
                        Toast.makeText(this, R.string.toast_backkey, Toast.LENGTH_SHORT).show()
                        backPressFlag = true

                        false
                    } else {
                        finish()

                        false
                    }
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun webviewDestroy(webview: BaseWebView) {
        webview?.let {
            it.stopLoading()
            it.removeAllViews()
            it.clearHistory()
            it.clearCache(true)
            it.destroy()
        }
    }

    /**
     * Goto play store.
     */
    private fun gotoPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("market://details?id=$packageName")
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        cleanCookie()

        mWebview?.let {
            webviewDestroy(it)
        }

        if (bp != null) {
            bp!!.release()
        }

        backPressFlag = false

        super.onDestroy()
    }

    /**
     * Restart app.
     */
    fun restartApp() {
        cleanCookie()
        isRestartApp = true

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val componentName = intent.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        startActivity(mainIntent)
        System.exit(0)
    }

    /**
     * Clean cookie.
     */
    private fun cleanCookie() {
        DLog.e("isRestartApp ==>> $isRestartApp")

        if (!isRestartApp) {
            this@MainActivity.runOnUiThread {
                mWebview?.clearCache(true)
                mWebview?.clearHistory()

                val cookieSyncMngr = CookieSyncManager.createInstance(this@MainActivity)
                cookieSyncMngr.startSync()
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookie()
                cookieManager.removeSessionCookie()
                cookieSyncMngr.stopSync()
            }
        }
    }

    public override fun onStart() {
        super.onStart()

        FirebaseUserActions.getInstance().start(action)
    }

    public override fun onStop() {
        super.onStop()

        FirebaseUserActions.getInstance().end(action)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        val isBool = bp?.handleActivityResult(requestCode, resultCode, intent) ?: false

        DLog.e("bjj BILLING onActivityResult " + isBool + " ^ " + intent)

        if (isBool) {
            super.onActivityResult(requestCode, resultCode, intent)
        }

        when (requestCode) {
            DEV_REQUEST_CODE -> {
                checkSettingInfo()
            }

            X_PAY_REQUEST_CODE -> {
//                if (mReceivedPaymentResultType != PaymentResultType.NONE) {
//                    // 세션 만료와 관계없이 결제 성공/실패/취소가 되었을 경우
//                    val intent = Intent()
//                    if (mReceivedPaymentResultType == PaymentResultType.SUCCESS) {
//                        intent.putExtra(KEY_CREDITCARD_PAYMENT_RESULT, true)
//                    } else {
//                        intent.putExtra(KEY_CREDITCARD_PAYMENT_RESULT, false)
//                    }
//
//                    intent.putExtra(KEY_CREDITCARD_PAYMENT_RESULT_ERRORCODE, mErrorCode)
//                    intent.putExtra(KEY_CREDITCARD_PAYMENT_RESULT_COUPONINFO, mCouponInfo)
//                    intent.putExtra(KEY_CREDITCARD_PAYMENT_RESULT_MESSAGE, mMessage)
//
//                    setResult(Activity.RESULT_OK, intent)
//                    finish()
//                } else if (mIsStartPaymentApp && isSessionFinish) {
//                    // 외부 카드 앱으로부터 세션 만료 후 result 받은 경우 (결제 성공/실패/취소가 없는 경우)
//                    showSessionTimeoutPopup()
//                } else {
//                    // 세션이 만료되지 않은 상태에서 외부 카드 앱이 종료된 경우
//                    DLog.e("bjj SESSION IS NOT EXPIRED")
//                }
            }
        }
    }

    /**
     * 권한 체크
     */
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            val listPermissionsNeeded = MyApplication.PERMISSIONS.filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            return if (listPermissionsNeeded.isNotEmpty()) {
                val viewPermission = findViewById<View>(R.id.view_permissioin)
                viewPermission.visibility = View.VISIBLE

                findViewById<View>(R.id.btn_permission_ok).setOnClickListener {
                    showPermissionView(listPermissionsNeeded)
                }

                findViewById<View>(R.id.btn_permission_cancel).setOnClickListener {
                    finish()
                }

                false
            } else { // 권한 허용된 경우
                true
            }
        }

        return true
    }

    private fun showPermissionView(listPermissionsNeeded: List<String>) {
        val viewPermission = findViewById<View>(R.id.view_permissioin)
        var isPermissionGranted = true

        for (permissions in listPermissionsNeeded) {
            if (ActivityCompat.checkSelfPermission(this, permissions) != PackageManager.PERMISSION_GRANTED) {
                isPermissionGranted = false
                break
            }
        }

        // Permission 전체 승인
        if (isPermissionGranted) {
            viewPermission.visibility = View.GONE
        }
        // Permission 승인 필요
        else {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 0)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty()) {
            var isGranted = true

            for (granted in grantResults) {
                if (granted != PackageManager.PERMISSION_GRANTED) isGranted = false
            }

            if (isGranted) {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

                if (keyguardManager.isKeyguardLocked) {
                    return
                }

                val viewPermission = findViewById<View>(R.id.view_permissioin)
                viewPermission.visibility = View.GONE

                setMainView()
            } else {
                var isRationale = true
                val listPermissionsNeeded = MyApplication.PERMISSIONS.filter {
                    ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                }

                for (permissions in listPermissionsNeeded) {

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions)) {
                        isRationale = false
                        break
                    }
                }

                if (!isRationale) {
                    // 다시보지않기 권한거절
                    showPermissionDenyDialog()
                } else {
                    finish()
                }
            }
        }
    }

    private fun goToDevActivity() {
        val i = Intent(this, DevActivity::class.java)

        startActivityForResult(i, DEV_REQUEST_CODE)
    }

    private fun removeSplash() {
        DLog.e("bjj removeSplash : ")

        if (isFinishing) {
            return
        }

        (this@MainActivity as Activity).runOnUiThread {
            setSplash()
        }
    }

    //공유 팝업노출
    private fun openSharePopup(url: String) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, url)
        sendIntent.type = "text/plain"
        startActivity(Intent.createChooser(sendIntent, ""))
    }

    // web page clear history
    private fun pageClearHistory() {
        mWebview?.clearHistory()
        mWebview?.removeAllViews()
    }

    /**
     * WEB INTERFACE
     */
    private val iWebBridgeApi = object : IWebBridgeApi {
        override fun pageClearHistory() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.pageClearHistory()
            }
        }

        override fun openSharePopup(url: String) {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.openSharePopup(url)
            }
        }

        override fun getPushRegId() {
            (this@MainActivity as Activity).runOnUiThread {
                val regId = SharedPreferencesUtil.getString(this@MainActivity, SharedPreferencesUtil.Cmd.PUSH_REG_ID)
                mWebview?.sendEvent(WaterBottleServerScript.GET_PUSH_REG_ID, getPushRegIdInfo(regId!!, "AOS").toJsonString())
            }
        }

        override fun restartApp() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.restartApp()
            }
        }

        override fun finishApp() {
            (this@MainActivity as Activity).runOnUiThread {
                System.exit(0)
            }
        }

        override fun getAppVersion() {
            (this@MainActivity as Activity).runOnUiThread {
                val version = getVersionName(this@MainActivity)
                mWebview?.sendEvent(WaterBottleServerScript.GET_APP_VERSION, GetAppVersionInfo(version!!).toJsonString())
            }
        }

        override fun requestCallPhone(data: RequestCallPhoneInfo) {
            (this@MainActivity as Activity).runOnUiThread {
                try {
                    val callIntent = Intent(Intent.ACTION_DIAL)
                    callIntent.data = Uri.parse("tel:${data.phoneNumber}")
                    startActivity(callIntent)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }

        override fun requestExternalWeb(data: RequestExternalWebInfo) {
            (this@MainActivity as Activity).runOnUiThread {
                intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.url))
                startActivity(intent)
            }
        }

        override fun removeSplash() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.removeSplash()
            }
        }
    }

    companion object {
        private var backPressFlag: Boolean = false // app 종료시 back key 가 2번째 눌렸는지 확인하기 위한 flag.

        private val HANDLER_APP_FINISH = 0 // app 종료시 back key 가 2번째 눌렸는지 확인하기 위한 handler.
        private val HANDLER_NETWORK_TIMER = 1 // The network timer handler.
        private val HANDLER_SPLASH = 2 // 스플래시 종료 핸들러

        private val MOBILE_ISP = "kvp.jjy.MispAndroid320"
        private val SMARTXPAY_TRANSFER = "kr.co.uplus.ecredit"
        private val V3_MOBILE_PLUS = "com.ahnlab.v3mobileplus"
        private val PAYNOW = "com.lguplus.paynow"
        private val PAYPIN = "com.skp.android.paypin"

        private val DEV_REQUEST_CODE = 1 // 히든 메뉴에서 돌아왔을 때 flag값
        private val X_PAY_REQUEST_CODE = 999

        private val PING_TIME = 100000 //The ping time.
        private val NETWORK_CONNECTION_ERROR = 1 //The network connection error.
        private val TIMEOUT_ERROR = 2 //The timeout error.
        private val USIM_MOUNTED = 3 //유심 장착 error.
        private val APP_VERSION_CHECK = 4 //앱 버전 check.

        private val REQ_PERMISSION_CODE = 1

        private val BILLING_SUBSCRIBE_MONTH_PRODUCT_ID = "subscribe_month"
        private val BILLING_SINGLE_MONTH_PRODUCT_ID = "single_month"
        private val BILLING_LICENS_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq/ZVmW2YZvVWSDLBJDVCe26C1vzDZ+qpWnQzE+7yia/a6hCK/x7DXKgtSYWcYEtFX1jeqG7KeGbDfhDxjX2q4OqJM4Vd4tV9XsZxB67v+DYnw4D0ZyKtd94WBQVLnHTi9O+ArH8RNAGpGzCncoQB0+we6bKGwQ/1L1Whn5aGImAPr4zHZLeBTKLByKFAKiWABWMFyjGl07x7UOq9DW6H4gGTKYlMbmbZsE7I7164j6N8lGb+LSESh5UoH8oNZmtxS1Y9Yn59EG4LXX37gjcZtj4JTy2A4ew5xXWE1Gvy25cmWpxMCwjooaEu4RYo/juCOsawDIQe37NEBtJgc4o4TwIDAQAB"
    }

    private class WeakHandler(act: MainActivity) : Handler() {
        private val ref: WeakReference<MainActivity> = WeakReference(act)

        override fun handleMessage(msg: Message) {
            val act = ref.get()

            if (act != null) {
                when (msg.what) {
                    HANDLER_APP_FINISH -> {
                        backPressFlag = false
                    }
                    HANDLER_NETWORK_TIMER -> {
                        act.showErrorDlg(TIMEOUT_ERROR)
                    }
                    HANDLER_SPLASH -> if (act.checkPermission()) {
                        act.setMainView()
                    }
                }
            }
        }
    }

    private val networkListener = object : NetworkChangeListener {
        override fun onNetworkDisconnected() {
            DLog.e("bjj Listener onNetworkDisconnected")

            // 네트워크 전환 시 onNetworkDisconnected 들어왔을 경우 1초 딜레이 후 네트워크 상태 체크하여 네트워크 차단팝업 발생하도록 함
            (this@MainActivity as Activity).runOnUiThread {
                Handler().postDelayed({
                    if (getNetworkInfo(applicationContext) == NETWORK_TYPE_ETC) {
                        showErrorDlg(NETWORK_CONNECTION_ERROR)
                    }
                }, 1000)
            }
        }

        override fun onNetworkconnected() {
            DLog.e("bjj Listener onNetworkconnected")
        }

        override fun onDataSaverChanged() {
            DLog.e("bjj Listener onDataSaverChanged")

            (this@MainActivity as Activity).runOnUiThread {
                showDataSaveDlg(R.string.popup_title_data_save, R.string.popup_msg_data_save)
            }
        }
    }

    private var dataSaverListener = object : IDataSaverListener {
        override fun onDataSaverChanged() {
            DLog.e("bjj Listener onDataSaverChanged")

            (this@MainActivity as Activity).runOnUiThread {
                showDataSaveDlg(R.string.popup_title_data_save, R.string.popup_msg_data_save)
            }
        }
    }

    private val mSimStateChangeListener = object : SimStateChangeListener {
        override fun onUsimMount() {
            DLog.e("bjj Listener onUsimMount")
        }

        override fun onUsimUnMount() {
            DLog.e("bjj Listener onUsimUnMount")
        }
    }

    private fun urlLoading(view: WebView?, uri: Uri?): Boolean {
        if (uri.toString().isNullOrEmpty()) {
            return false
        }

        val url = uri.toString()
        val scheme = uri!!.scheme

        DLog.e("bjj uri.toString() = $uri, ${uri!!.scheme}")

        when (scheme) {
            "https" -> return false
            "intent" -> {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    val isInstalled = isInstalledApp(this, intent.getPackage())

                    if (isInstalled) {
//                    mIsStartPaymentApp = true

                        startActivityForResult(intent, X_PAY_REQUEST_CODE)
                    } else {
//                    mIsStartPaymentApp = true

                        val marketIntent = Intent(Intent.ACTION_VIEW)
                        val urlSb = StringBuilder()
                        urlSb.append("market://details?id=").append(intent.getPackage())
                        marketIntent.data = Uri.parse(urlSb.toString())
                        startActivityForResult(marketIntent, X_PAY_REQUEST_CODE)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return true
            }
            "market" -> {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (intent != null) {
//                    mIsStartPaymentApp = true

                        startActivityForResult(intent, X_PAY_REQUEST_CODE)
                    }
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }

                return true
            }
            "ahnlabv3mobileplus" -> {
                try {
                    val isInstalled = isInstalledApp(this, V3_MOBILE_PLUS)

                    if (!isInstalled) {
//                    mIsStartPaymentApp = true

                        val marketIntent = Intent(Intent.ACTION_VIEW)
                        val urlSb = StringBuilder()
                        urlSb.append("market://details?id=").append(V3_MOBILE_PLUS)
                        marketIntent.data = Uri.parse(urlSb.toString())
                        startActivityForResult(marketIntent, X_PAY_REQUEST_CODE)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return true
            }
            "ispmobile" -> {
                try {
                    val isInstalled = isInstalledApp(this, MOBILE_ISP)

                    if (isInstalled) {
//                    mIsStartPaymentApp = true
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivityForResult(intent, X_PAY_REQUEST_CODE)
                    } else {
//                    mIsStartPaymentApp = true

                        val marketIntent = Intent(Intent.ACTION_VIEW)
                        val urlSb = StringBuilder()
                        urlSb.append("market://details?id=").append(MOBILE_ISP)
                        marketIntent.data = Uri.parse(urlSb.toString())
                        startActivityForResult(marketIntent, X_PAY_REQUEST_CODE)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return true
            }
            "smartxpay-transfer" -> {
                try {
                    val isInstalled = isInstalledApp(this, SMARTXPAY_TRANSFER)

                    if (isInstalled) {
//                    mIsStartPaymentApp = true
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivityForResult(intent, X_PAY_REQUEST_CODE)
                    } else {
//                    mIsStartPaymentApp = true

                        val marketIntent = Intent(Intent.ACTION_VIEW)
                        val urlSb = StringBuilder()
                        urlSb.append("market://details?id=").append(SMARTXPAY_TRANSFER)
                        marketIntent.data = Uri.parse(urlSb.toString())
                        startActivityForResult(marketIntent, X_PAY_REQUEST_CODE)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return true
            }
            "lguthepay-xpay", "lguthepay" -> {
                try {
                    val isInstalled = isInstalledApp(this, PAYNOW)

                    if (isInstalled) {
//                    mIsStartPaymentApp = true
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivityForResult(intent, X_PAY_REQUEST_CODE)
                    } else {
//                    mIsStartPaymentApp = true

                        val marketIntent = Intent(Intent.ACTION_VIEW)
                        val urlSb = StringBuilder()
                        urlSb.append("market://details?id=").append(PAYNOW)
                        marketIntent.data = Uri.parse(urlSb.toString())
                        startActivityForResult(marketIntent, X_PAY_REQUEST_CODE)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return true
            }
            "paypin" -> {
                try {
                    val isInstalled = isInstalledApp(this, PAYPIN)

                    if (isInstalled) {
//                    mIsStartPaymentApp = true
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivityForResult(intent, X_PAY_REQUEST_CODE)
                    } else {
//                    mIsStartPaymentApp = true

                        val marketIntent = Intent(Intent.ACTION_VIEW)
                        val urlSb = StringBuilder()
                        urlSb.append("market://details?id=").append(PAYPIN).append("&feature=search_result#?t=W251bGwsMSwxLDEsImNvbS5za3AuYW5kcm9pZC5wYXlwaW4iXQ..")
                        marketIntent.data = Uri.parse(urlSb.toString())
                        startActivityForResult(marketIntent, X_PAY_REQUEST_CODE)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return true
            }
            "tel" -> {
//            mIsStartPaymentApp = true

                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
                startActivityForResult(dialIntent, X_PAY_REQUEST_CODE)
                return true
            }
            else -> return false
        }
    }

    private fun isInstalledApp(context: Context, packageName: String?): Boolean {
        val appList = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in appList) {
            if (appInfo.packageName == packageName) {
                return true
            }
        }

        return false
    }


    //BILLING
    override fun onBillingInitialized() {
        // * 구매 완료시 호출
        // productId: 구매한 sku (ex) no_ads)
        // details: 결제 관련 정보
        val isBool = bp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
        SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)

        DLog.e("bjj BILLING onBillingInitialized")
    }

    override fun onPurchaseHistoryRestored() {
        // * 구매 정보가 복원되었을때 호출
        // bp.loadOwnedPurchasesFromGoogle() 하면 호출 가능
        val isBool = bp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
        SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)

        DLog.e("bjj BILLING onPurchaseHistoryRestored")
    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        // * 구매 오류시 호출
        // errorCode == Constants.BILLING_RESPONSE_RESULT_USER_CANCELED 일때는
        // 사용자가 단순히 구매 창을 닫은것임으로 이것 제외하고 핸들링하기.

        DLog.e("bjj BILLING onProductPurchased " + productId + " ^ " + details)

        if (productId.equals("single_month")) {
            // TODO: 구매 해 주셔서 감사합니다! 메세지 보내기
            val isBool = bp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
            SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)

            // * 광고 제거는 1번 구매하면 영구적으로 사용하는 것이므로 consume하지 않지만,
            // 만약 게임 아이템 100개를 주는 것이라면 아래 메소드를 실행시켜 다음번에도 구매할 수 있도록 소비처리를 해줘야한다.
            // bp.consumePurchase(Config.Sku);
        }
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        // * 처음에 초기화됬을때.
        val isBool = bp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
        SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)

        if (errorCode != Constants.BILLING_RESPONSE_RESULT_USER_CANCELED) {
            Toast.makeText(this, errorCode, Toast.LENGTH_SHORT).show()
        }

        DLog.e("bjj BILLING onBillingError " + errorCode + " ^ " + error)
    }
}