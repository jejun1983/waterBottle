package com.idevel.waterbottle.web;

import android.content.Context;
import android.webkit.CookieManager;

import com.idevel.waterbottle.utils.DLog;
import com.idevel.waterbottle.utils.SharedPreferencesUtil;
import com.idevel.waterbottle.utils.Utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class WebviewSessionSync implements Callable<Boolean> {
    private final Context ctx;
    public static CountDownLatch syncGate;

    public WebviewSessionSync(Context ctx) {
        this.ctx = ctx;
    }

    public void sync(String url) {
        String cookie = CookieManager.getInstance().getCookie(url);
        DLog.d("***** webview cookies : " + cookie);
        // API 21 이전 버전이면 CookieSyncManager로 쿠키를 수동싱크 해줘야 한다.
        String mainUrl = SharedPreferencesUtil.getString(ctx, SharedPreferencesUtil.Cmd.MAIN_URL);
        CookieManager.getInstance().setCookie(mainUrl, cookie);
    }

    public void checkAndReconnect() {
        ExecutorService exsvc = Executors.newSingleThreadExecutor();
        exsvc.submit(this);
        exsvc.shutdown();
    }

    public Boolean call() throws Exception {
        syncGate = new CountDownLatch(1);
        boolean established = isEstablished();
        DLog.d("***** session established : " + established);
        if (!established) {
            reconnect();
        }
        syncGate.countDown();
        return true;
    }

    private boolean isEstablished() throws Exception {
        String mainUrl = SharedPreferencesUtil.getString(ctx, SharedPreferencesUtil.Cmd.MAIN_URL);
        String cookie = CookieManager.getInstance().getCookie(mainUrl);
        DLog.d("***** session check cookie : " + cookie);

        URL url = new URL(mainUrl + "/mmbr/logincheck.mccx");
        HttpURLConnection conn = getConnection(url);
        DLog.d("***** connect : " + url);
        conn.setRequestProperty("cookie", cookie);
        conn.getOutputStream().write(loginParam());

        String body = getResponseBody(conn);
        conn.disconnect();

        JSONObject json = new JSONObject(body);
        DLog.d("***** session check response = " + json);
        return "D".equals(json.getString("loginResult"));
    }

    private String getResponseBody(HttpURLConnection conn) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
        }
        in.close();
        return sb.toString();
    }

    private void reconnect() throws Exception {
//        String mainUrl = SharedPreferencesUtil.getString(ctx, SharedPreferencesUtil.Cmd.MAIN_URL);
//        CookieManager.getInstance().removeAllCookie();
//
//        URL url = new URL(UrlData.getAutoLoginPageUrl(ctx));
//        HttpURLConnection conn = getConnection(url);
//        DLog.d("***** connect : " + url);
//        conn.getOutputStream().write(loginParam());
//
//        Map<String, List<String>> headers = conn.getHeaderFields();
//        DLog.d("***** headers = " + headers);
//        List<String> cookies = headers.get("Set-Cookie");
//        if (cookies != null) {
//            for (String cookie : cookies) {
//                HttpCookie c = HttpCookie.parse(cookie).get(0);
//                String cookieString = c.getName() + "=" + c.getValue();
//                DLog.d("***** new session : " + cookieString);
//                CookieManager.getInstance().setCookie(mainUrl, cookieString);
//            }
//        }
//
////        String body = getResponseBody(conn);
////        DLog.d("***** session reconnect response = " +  body);
//        conn.disconnect();
//        DLog.d("***** new session established");
    }

    private HttpURLConnection getConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "com.idevel.waterbottle");
        conn.setDoOutput(true);
        return conn;
    }

    private byte[] loginParam() {
        String param = Utils.getPostData(ctx);
        return param.getBytes();
    }
}
