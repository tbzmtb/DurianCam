package kr.durian.duriancam.asynctask;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
 * Created by tbzm on 16. 5. 11.
 */
public class GetDeviceTokenTask extends AsyncTask<String, Void, String> {
    private final String TAG = getClass().getName();
    private Context mContext;
    private String mImageDate;

    public GetDeviceTokenTask(Context context, String imageDate) {
        mContext = context;
        mImageDate = imageDate;
    }

    @Override
    protected String doInBackground(String... args) {
        String returnValue = null;
        try {
            String urlString = Config.SERVER_POST_URL + Config.GET_PUSH_TOKEN_PHP;
            Logger.d(TAG, "urlString = " + urlString);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.setUseCaches(false);
            conn.setReadTimeout(20000);
            conn.setRequestMethod("POST");
            StringBuffer params = new StringBuffer("");
            params.append(Config.PARAM_RTCID + Config.PARAM_EQUALS + DataPreference.getPeerRtcid());
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
        if (s != null && mContext != null) {
            Logger.d(TAG, "token = " + s.trim());
            try {
                JSONArray array = new JSONArray(s.trim());
                List<String> ids = new ArrayList<>();

                for (int i = 0; i < array.length(); i++) {
                    Logger.d(TAG, "array.getJSONObject(i).getString(Config.PARAM_TOKEN) = " + array.getJSONObject(i).getString(Config.PARAM_TOKEN));
                    ids.add(array.getJSONObject(i).getString(Config.PARAM_TOKEN));
                }
                new SendPushAsyncTask(mContext, mContext.getString(R.string.detect_notice), mImageDate, ids).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
