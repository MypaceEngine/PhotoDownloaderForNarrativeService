package com.mypaceengine.downloadnarrative;

import android.util.Log;
import android.webkit.CookieManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public abstract class Job_LoadNarrativeInfo_Abstract extends AbstractJobN  implements Serializable {

    String url = null;


    public void setInfo(String _url){
        url = _url;
    }

    void stop(){
        super.stop();
        if(con!=null){
            con.disconnect();
        }
    }

    abstract List<AbstractJobN> createJobInfo(JSONObject jsonObj) throws Exception;

    public JSONObject getNarrativetExec(String urlStr) throws Exception {
        return getNarrativetExec(urlStr, 0);
    }
    HttpURLConnection con=null;
    public JSONObject getNarrativetExec(String urlStr, int retryCnt) throws Exception {
        Log.d("MainServiceTask", "GetInfo_FromNarrative:" + urlStr);

        if(this.shutdownFlg){
            throw new Exception();
        }
        String seacret = dataUtil.getNarrativeKey();
        if (seacret != null) {

            InputStream in = null;
            InputStreamReader inReader = null;
            BufferedReader bufReader = null;
            StringBuffer buf = new StringBuffer();
            try {
// URLの作成
                URL url = new URL(urlStr);

// 接続用HttpURLConnectionオブジェクト作成
                con = (HttpURLConnection) url.openConnection();
                ArrayList<String> cookie_list = dataUtil.getNarrativeCookie();
                for (String cookie : cookie_list) {
                    CookieManager.getInstance().setCookie(WebActivity.DOMAIN, cookie);
                }

                String cookie = CookieManager.getInstance().getCookie(WebActivity.DOMAIN);

                con.addRequestProperty("Cookie", cookie);

// リクエストメソッドの設定
                con.setRequestMethod("GET");
// リダイレクトを自動で許可しない設定
                con.setInstanceFollowRedirects(true);
// ヘッダーの設定(複数設定可能)
                con.setRequestProperty("Authorization", "Bearer " + seacret);
// 接続
                con.connect();

                int responseCode = con.getResponseCode();
                String responseMessage = con.getResponseMessage();

                Log.d("Narrative Responce", String.format("%s[%d]", responseMessage, responseCode));

                if ((responseCode == HttpURLConnection.HTTP_UNAUTHORIZED)||(responseCode == HttpURLConnection.HTTP_NOT_FOUND)){
                        //TODO handle unauthorized
                        dataUtil.setNarrativeReauthNeed(true);
                        throw new Exception();
                }
                in = con.getInputStream();

                inReader = new InputStreamReader(in);
                bufReader = new BufferedReader(inReader);
                String line = null;
                // 1行ずつテキストを読み込む
                while ((line = bufReader.readLine()) != null) {
                    buf.append(line);
                }
                bufReader.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                if ((retryCnt < 10)&&(!this.shutdownFlg)) {
                    getNarrativetExec(urlStr, retryCnt++);
                } else {
                    throw ex;
                }
            } finally {
                if (bufReader != null) {
                    bufReader.close();
                }
                if (inReader != null) {
                    inReader.close();
                }

                if (con != null) {
                    con.disconnect();
                }
                if (in != null) {
                    in.close();
                }
            }
            String jsonStr = buf.toString();
            while (true) {
                int c = jsonStr.charAt(0);
                if (c == -1) {
                    jsonStr = jsonStr.substring(1);
                } else {
                    break;
                }
            }

            Log.d("XMLDEBUG", jsonStr);
            JSONObject json = new JSONObject(jsonStr);
            return json;
        } else {
            dataUtil.setNarrativeReauthNeed(true);
        }
        return null;
    }
}