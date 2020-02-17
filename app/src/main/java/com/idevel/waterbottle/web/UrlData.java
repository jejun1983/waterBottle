package com.idevel.waterbottle.web;

import android.content.Context;

import com.idevel.waterbottle.utils.SharedPreferencesUtil;

/**
 * The UrlData Class.
 *
 * @author : jjbae
 */
public class UrlData {
    /**
     * The Constant LOGIN_PAGE_URL.
     */
    public static String NORMAL_SERVER_URL = "https://www.google.co.kr";
    public static final String SETTING_URL = "https://poll.medialog.co.kr/ords/aop/v1/app/";

    public static String getMainUrl(Context context) {
//        if (BuildConfig.DEBUG) {
//            String url = SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.MAIN_URL);
//
//            if (url.isEmpty()) {
//                url = NORMAL_SERVER_URL;
//                SharedPreferencesUtil.setString(context, SharedPreferencesUtil.Cmd.MAIN_URL, url);
//            }
//
//            return url;
//        } else {
//            return NORMAL_SERVER_URL;
//        }


        String url = NORMAL_SERVER_URL;
        SharedPreferencesUtil.setString(context, SharedPreferencesUtil.Cmd.MAIN_URL, url);

        return url;
    }
}
