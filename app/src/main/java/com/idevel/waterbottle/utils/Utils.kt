package com.idevel.waterbottle.utils

import android.content.Context
import java.util.*

/**
 * The Utils Class.
 */
object Utils {

    /**
     * Gets the post data.
     *
     * @param context the conext
     * @return the post data
     */
    @JvmStatic
    fun getPostData(context: Context): String {
//        val ctn: String = getCTN(context)
//        val language: String = SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.MULTI_LANGUAGE) ?: ""
//        val easyLoginInfo = SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.EASY_LOGIN_INFO)
//
//        return if (!easyLoginInfo.isNullOrEmpty()) {
//            "ctn=$ctn&language=$language&regLoginYN=Y"
//        } else {
//            "ctn=$ctn&language=$language&regLoginYN=N"
//        }

        return "aaaa"
    }


    fun getUUID(context: Context): String {
        var uuid = SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.UUID) ?: ""
        if (uuid.isEmpty()) {
            uuid = UUID.randomUUID().toString()
            SharedPreferencesUtil.setString(context, SharedPreferencesUtil.Cmd.UUID, uuid)
        }

        return uuid
    }

}