package com.idevel.waterbottle.web.interfaces

import com.idevel.waterbottle.web.constdata.RequestCallPhoneInfo
import com.idevel.waterbottle.web.constdata.RequestExternalWebInfo


/**
 * The BridgeListener
 * @company : medialog
 */
interface IWebBridgeApi {
    fun removeSplash()                                      //webUI에서 화면 load가 끝나면 splash 화면을 제거
    fun getPushRegId()                                      //push reg id를 저장
    fun restartApp()                                        //app 재시작
    fun finishApp()                                         //app 종료
    fun getAppVersion()                                     //앱 버전을 return(6자리)
    fun requestCallPhone(data: RequestCallPhoneInfo)        //전화요청
    fun requestExternalWeb(data: RequestExternalWebInfo)    //전달한 url로 외부 브라우져 요청
    fun pageClearHistory() {}                               //web page history 초기화
    fun openSharePopup(url: String) {}                      //공유 팝업노출
}