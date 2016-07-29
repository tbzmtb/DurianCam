package kr.durian.duriancam.asynctask;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import kr.durian.duriancam.util.Logger;

/**
 * Created by tbzm on 16. 5. 31.
 */
public class SendPushAsyncTask extends AsyncTask<String, String, String> {
    public List<String> ids = new ArrayList<>();
    public Context mContext;
    public String TAG = getClass().getName();
    public String title;
    public String message;

    public SendPushAsyncTask(Context context, String title, String message, List<String> ids) {
        mContext = context;
        this.title = title;
        this.message = message;
        this.ids = ids;

    }

    @Override
    protected String doInBackground(String... args) {

        try {

            JSONObject data = new JSONObject();
            data.put("title", title);
            data.put("message", message);

            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();
            for (int i = 0; i < ids.size(); i++) {
                array.put(ids.get(i));
            }
            json.put("registration_ids", array);

            json.put("data", data);

            json.put("collapse_key", "score_update");
            json.put("time_to_live", 108);
            json.put("delay_while_idle", true);

            URL url = new URL("https://android.googleapis.com/gcm/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "key=AIzaSyB0pQ2wg-pDepV5SkIe3iyRbGYQy5VPkRg");

            conn.setDoOutput(true);


            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            Logger.d(TAG, "json = " + json.toString());
            byte[] realdata = json.toString().getBytes("UTF-8");
            Logger.d(TAG, "realdata = " + realdata);
            wr.write(json.toString().getBytes());

            wr.flush();
            wr.close();

            //Get the response
            int responseCode = conn.getResponseCode();
            Logger.d(TAG, "\nSending 'POST' request to URL : " + url);
            Logger.d(TAG, "Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            Logger.e("@", "in : " + in);
            String inputLine;
            StringBuffer response = new StringBuffer();
            Logger.d(TAG, "sun response : " + response);

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            Logger.d(TAG, "result = " + response.toString());
            return response.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
