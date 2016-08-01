package kr.durian.duriancam.asynctask;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import kr.durian.duriancam.R;
import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.DataPreference;
import kr.durian.duriancam.util.Logger;

/**
 * Created by sunyungkim on 16. 8. 1..
 */
public class GetInAppPaymentTask extends AsyncTask<String, Void, String> {
    private final String TAG = getClass().getName();
    private Context mContext;
    private Handler handler;

    public GetInAppPaymentTask(Context context, Handler handler) {
        mContext = context;
        this.handler = handler;
    }

    @Override
    protected String doInBackground(String... args) {
        String returnValue = null;
        try {
            String urlString = Config.SERVER_POST_URL + Config.GET_IN_APP_PAY_MENT_PHP;
            Logger.d(TAG, "urlString = " + urlString);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.setUseCaches(false);
            conn.setReadTimeout(20000);
            conn.setRequestMethod("POST");
            StringBuffer params = new StringBuffer("");
            params.append(Config.PARAM_RTCID + Config.PARAM_EQUALS + DataPreference.getRtcid());
            PrintWriter output = new PrintWriter(conn.getOutputStream());
            output.print(params.toString());
            output.close();

            // Response받기
            StringBuffer sb = new StringBuffer();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            for (; ; ) {
                String line = br.readLine();
                if (line == null) break;
                sb.append(line + "\n");
            }

            br.close();
            conn.disconnect();

            returnValue = sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnValue;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (s != null && mContext != null && handler != null) {
            String result = s.trim();
            Logger.d(TAG, "payment = " + result);
            Message msg = new Message();
            msg.obj = result;
            msg.what = Config.GET_IN_APP_PAY_MENT_HANDLER_KEY;
            handler.sendMessage(msg);

        }
    }
}
