package kr.durian.duriancam.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONException;
import org.json.JSONObject;

import kr.durian.duriancam.R;
import kr.durian.duriancam.asynctask.InsertUserInfoTask;
import kr.durian.duriancam.service.DataService;
import kr.durian.duriancam.service.IDataService;
import kr.durian.duriancam.service.IDataServiceCallback;
import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.DataPreference;
import kr.durian.duriancam.util.Logger;

/**
 * Created by sunyungkim on 16. 7. 26..
 */
public class ViewerModeSelectActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {
    private IDataService mService;
    private final String TAG = getClass().getName();
    private Button mBabyButton;
    private Button mCctvButton;
    private Button mSecureButton;
    private GoogleApiClient mGoogleApiClient;
    private final int RC_SIGN_IN = 0;
    private DataHandler mHandler;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_mode_select);
        mHandler = new DataHandler();

        Logger.d(TAG, "ViewerModeSelectActivity onCreate call");
        mBabyButton = (Button) findViewById(R.id.btn_baby_talk);
        mBabyButton.setOnClickListener(this);

        mCctvButton = (Button) findViewById(R.id.btn_cctv);
        mCctvButton.setOnClickListener(this);

        mSecureButton = (Button) findViewById(R.id.btn_secure);
        mSecureButton.setOnClickListener(this);
        if (Config.GOOGLE_SERVICE_ENABLE_DEVICE) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(getString(R.string.web_client_id))
//                    .requestIdToken(getString(R.string.android_release_client_id))
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
        }
    }

    private void connectWebSocket() {
        if (mService != null) {
            try {
                mService.connectWebSocket();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private class DataHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == Config.INSERT_USER_INFO_HANDLER) {
                connectWebSocket();

            }
        }
    }

    private void signIn() {
        showProgress();
        if (Config.GOOGLE_SERVICE_ENABLE_DEVICE) {
            if (!DataPreference.getEasyLogin()) {
                if (mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.clearDefaultAccountAndReconnect();
                }
            }

            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
//        else {
//            String displayName = "김선영";
//            String emailString = "tbzmtb@gmail.com";
//            String userId = "100398893839570743730";
//            String token = "";
//            DataPreference.setLoginName(displayName);
//            DataPreference.setLoginEmail(emailString);
//            DataPreference.setLoginNumber(userId);
//            DataPreference.setLoginToken(token);
//
//            DataPreference.setRtcid(userId);
//            DataPreference.setPeerRtcid("tbzmtb");
//
//
//            String type = Config.DEVICE_TYPE_ANDROID_VALUE;
//            String uuid = Build.SERIAL;
//            String serial_no = Build.SERIAL;
//            String password = "";
//            String master_rtcid = "";
//            String cert_master = "";
//            String email = "tbzmtb@gmail.com";
//            String cert_email = "";
//            String name = "김선영";
//            String disable = "0";
//
//
//            new InsertUserInfoTask(this, mHandler, DataPreference.getRtcid(), type, uuid, serial_no, password, master_rtcid,
//                    cert_master, email, cert_email, name, disable).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void cancelProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.setOnDismissListener(null);
                    mProgressDialog.cancel();
                    mProgressDialog = null;
                }
            }
        });

    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        Log.d(TAG, "handleSignInResult:" + result.toString());
        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            if (acct == null) {
                return;
            }
            String displayName = "";
            String emailString = "";
            String userId = "";
            String token = "";


            if (acct.getDisplayName() != null) {
                displayName = acct.getDisplayName();
            }
            if (acct.getEmail() != null) {
                emailString = acct.getEmail();
            }
            if (acct.getId() != null) {
                userId = acct.getId();
            }
            if (acct.getIdToken() != null) {
                token = acct.getIdToken();
            }
            Logger.d(TAG, "displayName =" + displayName);
            Logger.d(TAG, "email =" + emailString);
            Logger.d(TAG, "userId =" + userId);
            Logger.d(TAG, "token =" + token);
            Logger.d(TAG, "google login ok");

            DataPreference.setLoginName(displayName);
            DataPreference.setLoginEmail(emailString);
            DataPreference.setLoginNumber(userId);
            DataPreference.setLoginToken(token);
            if (DataPreference.getMode() != Config.MODE_VIEWER) {
                DataPreference.setRtcid(userId);
                DataPreference.setPeerRtcid(emailString.split("@")[0]);

            } else {
                DataPreference.setRtcid(emailString.split("@")[0]);
                DataPreference.setPeerRtcid(userId);
            }

            String type = Config.DEVICE_TYPE_ANDROID_VALUE;
            String uuid = Build.SERIAL;
            String serial_no = Build.SERIAL;
            String password = "";
            String pushToken = DataPreference.getPushToken();
            String payment = "0";
            String email = emailString;
            String cert_email = "";
            String name = displayName;
            String disable = "0";

            cancelProgress();

            new InsertUserInfoTask(this, mHandler, DataPreference.getRtcid(), type, uuid, serial_no, password, pushToken,
                    payment, email, cert_email, name, disable).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        } else {
            cancelProgress();
            Toast.makeText(ViewerModeSelectActivity.this, getString(R.string.fail_login), Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog == null) {
                    mProgressDialog = new ProgressDialog(ViewerModeSelectActivity.this);
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.setMessage(getString(R.string.please_wait));
                    mProgressDialog.show();
                }
            }
        });
    }


    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // ...
                    }
                });
    }

    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // ...
                    }
                });
    }

    final IDataServiceCallback mCallbcak = new IDataServiceCallback.Stub() {

        @Override
        public void valueChanged(int value, String data) throws RemoteException {
            if (value == Config.HANDLER_MODE_START) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (!json.isNull(Config.PARAM_DESCRIPTION)) {
                        String desciption = json.getString(Config.PARAM_DESCRIPTION);
                        if (desciption.equals(Config.PARAM_SUCCESS_DESCRIPTION)) {
                            Intent intent = new Intent(ViewerModeSelectActivity.this, CameraActivity.class);
                            startActivity(intent);
                        }
                    } else {
                        Intent intent = new Intent(ViewerModeSelectActivity.this, CameraActivity.class);
                        startActivity(intent);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult == null) {
            return;
        }
        if (connectionResult.getErrorMessage() == null) {
            return;
        }
        Logger.d(TAG, "onConnectionFailed call Message = " + connectionResult.getErrorMessage().toString());
        Logger.d(TAG, "onConnectionFailed call isSuccess = " + connectionResult.isSuccess());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Config.GOOGLE_SERVICE_ENABLE_DEVICE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void AllButtonUnselected() {
        mBabyButton.setSelected(false);
        mCctvButton.setSelected(false);
        mSecureButton.setSelected(false);
    }

    private void setDefaultMode() {
        AllButtonUnselected();
        if (DataPreference.getViewerWillConnectMode() == Config.MODE_BABY_TALK) {
            mBabyButton.setSelected(true);
        } else if (DataPreference.getViewerWillConnectMode() == Config.MODE_CCTV) {
            mCctvButton.setSelected(true);
        } else {
            mSecureButton.setSelected(true);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_baby_talk: {
                AllButtonUnselected();
                v.setSelected(true);
                DataPreference.setViewerWillConnectMode(Config.MODE_BABY_TALK);
                signIn();
                break;
            }
            case R.id.btn_cctv: {
                AllButtonUnselected();
                v.setSelected(true);
                DataPreference.setViewerWillConnectMode(Config.MODE_CCTV);
                signIn();
                break;
            }
            case R.id.btn_secure: {
                AllButtonUnselected();
                v.setSelected(true);
                DataPreference.setViewerWillConnectMode(Config.MODE_SECURE);
                signIn();
                break;
            }
        }
    }

    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                mService = IDataService.Stub.asInterface(service);
                try {
                    mService.registerCallback(mCallbcak);
                    setDefaultMode();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            if (mService != null) {
                try {
                    mService.unregisterCallback(mCallbcak);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
