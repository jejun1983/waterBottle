package com.idevel.waterbottle.web.constdata

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName


data class RequestCallPhoneInfo(
        @SerializedName("phoneNumber") val phoneNumber: String
)

data class RequestExternalWebInfo(
        @SerializedName("url") val url: String
)

data class OpenSharePopupInfo(
        @SerializedName("text") val text: String
)

data class getPushRegIdInfo(
        @SerializedName("regId") val regId: String,
        @SerializedName("os") val os: String
) {
    fun toJsonString(): String {
        return gson().toJson(this)
    }
}

data class GetAppVersionInfo(
        @SerializedName("appversion") val appversion: String
) {
    fun toJsonString(): String {
        return gson().toJson(this)
    }
}

data class PushDataInfo(
        @SerializedName("app_in") val app_in: String,                       //내부팝업 데이터
        @SerializedName("agnt_pop_yn") val agnt_pop_yn: String,             //팝업 사용여부
        @SerializedName("agnt_disp_cd") val agnt_disp_cd: String,           //노출방식 (T:text, W:웹뷰)
        @SerializedName("agnt_cnts") val agnt_cnts: String,                 //내용 (text, url)
        @SerializedName("agnt_img") val agnt_img: String,                   //이미지 url
        @SerializedName("agnt_link_dv_cd") val agnt_link_dv_cd: String,     //링크구분 (001:링크없음, 002,003:링크있음)
        @SerializedName("agnt_link_url") val agnt_link_url: String,         //링크 url
        @SerializedName("app_out") val app_out: String,                     //알림/외부팝업 데이터
        @SerializedName("noti_idct_hist_yn") val noti_idct_hist_yn: String, //알림 사용여부
        @SerializedName("noti_title") val noti_title: String,               //알림 제목
        @SerializedName("noti_cnts") val noti_cnts: String,                 //알림 내용
        @SerializedName("noti_img") val noti_img: String                    //이미지 url
)

private fun gson(): Gson {
    return GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
}