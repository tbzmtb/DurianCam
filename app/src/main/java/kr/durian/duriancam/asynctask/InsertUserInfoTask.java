package kr.durian.duriancam.asynctask;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import kr.durian.duriancam.R;
import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.Logger;

/**
 * Created by tbzm on 16. 5. 11.
 */
public class InsertUserInfoTask extends AsyncTask<String, Void, String> {
    private final String TAG = getClass().getName();
    private final String POST = "POST";
    private Context context;
    private Handler handler;
    private String rtcid;
    private String type;
    private String uuid;
    private String serial_no;
    private String password;
    private String token;
    private String payment;
    private String email;
    private String cert_email;
    private String name;
    private String disable;
    private ProgressDialog mProgressDialog;

    public InsertUserInfoTask(Context context, Handler handler, String rtcid,
                              String type, String uuid, String serial_no, String password,
                              String token, String payment, String email, String cert_email,
                              String name, String disable) {
        this.context = context;
        this.handler = handler;
        this.rtcid = rtcid;
        this.type = type;
        this.uuid = uuid;
        this.serial_no = serial_no;
        this.password = password;
        this.token = token;
        this.payment = payment;
        this.email = email;
        this.cert_email = cert_email;
        this.name = name;
        this.disable = disable;
    }

    private void showProgress() {
        try {
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(context);
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.setMessage(context.getString(R.string.please_wait));
                mProgressDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelProgress() {
        try {
            if (mProgressDialog != null) {
                mProgressDialog.cancel();
                mProgressDialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        showProgress();
    }

    @Override
    protected String doInBackground(String... args) {
        String returnValue = null;
        try {
            String urlString = Config.SERVER_POST_URL + Config.INSERT_USER_INFO_PHP;
            Logger.d(TAG, "urlString = " + urlString);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.setUseCaches(false);
            conn.setReadTimeout(20000);
            conn.setRequestMethod(POST);
            StringBuffer params = new StringBuffer("");
            params.append(Config.PARAM_RTCID + Config.PARAM_EQUALS + rtcid);
            params.append(Config.PARAM_AND + Config.PARAM_TYPE + Config.PARAM_EQUALS + type);
            params.append(Config.PARAM_AND + Config.PARAM_UUID + Config.PARAM_EQUALS + uuid);
            params.append(Config.PARAM_AND + Config.PARAM_SERIAL_NO + Config.PARAM_EQUALS + serial_no);
            params.append(Config.PARAM_AND + Config.PARAM_PASSWORD + Config.PARAM_EQUALS + password);
            params.append(Config.PARAM_AND + Config.PARAM_TOKEN + Config.PARAM_EQUALS + token);
            params.append(Config.PARAM_AND + Config.PARAM_PAYMENT + Config.PARAM_EQUALS + payment);
            params.append(Config.PARAM_AND + Config.PARAM_EMAIL + Config.PARAM_EQUALS + email);
            params.append(Config.PARAM_AND + Config.PARAM_CERT_EMAIL + Config.PARAM_EQUALS + cert_email);
            params.append(Config.PARAM_AND + Config.PARAM_NAME + Config.PARAM_EQUALS + name);
            params.append(Config.PARAM_AND + Config.PARAM_DISABLE + Config.PARAM_EQUALS + disable);
            params.append(Config.PARAM_AND + Config.PARAM_TOKEN + Config.PARAM_EQUALS + token);
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
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        cancelProgress();
        Logger.d(TAG, "result = " + result);
        if (handler != null) {
            Message msg = Message.obtain();
            msg.what = Config.INSERT_USER_INFO_HANDLER;
            msg.obj = result;
            handler.sendMessage(msg);
        }
    }
}
