package com.idevel.waterbottle.fcm

/**
 * Created by djsworld on 2016-02-25.
 */
object PushPreferences {

    const val PUSH_DATA = "data"

    const val PUSH_DATA_TITLE = "pushDataTitle"
    const val PUSH_DATA_BODY = "pushDataBody"
    const val PUSH_DATA_TYPE = "pushDataType"
    const val PUSH_DATA_IMAGE = "pushDataImage"
    const val PUSH_DATA_LINK_CODE = "pushDataLinkCode"
    const val PUSH_DATA_LINK_URL = "pushDataLinkUrl"

    // 내부팝업 없는 푸시로 진입 구분용
    const val PUSH_USE_IN_POPUP = "pushUseInPopup"

    // 푸시 데이터 타입
    const val PUSH_DATA_TYPE_TEXT = "T"
    const val PUSH_DATA_TYPE_WEB = "W"
    const val PUSH_DATA_TYPE_IMAGE = "I"
}
