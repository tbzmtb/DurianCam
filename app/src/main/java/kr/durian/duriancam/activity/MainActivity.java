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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
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
import org.mozilla.javascript.tools.debugger.Main;

import kr.durian.duriancam.R;
import kr.durian.duriancam.asynctask.InsertUserInfoTask;
import kr.durian.duriancam.service.DataService;
import kr.durian.duriancam.service.IDataService;
import kr.durian.duriancam.service.IDataServiceCallback;
import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.DataPreference;
import kr.durian.duriancam.util.Logger;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {
    public static MainActivity INSTANCE;
    private IDataService mService;
    private final String TAG = getClass().getName();
    private GoogleApiClient mGoogleApiClient;
    private final int RC_SIGN_IN = 0;
    private DataHandler mHandler;
    private ImageButton mBabyButton;
    private ImageButton mCctvButton;
    private Button mViewerButton;
    private Button mStartButton;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.d(TAG, "MainActivity onCreate call");
        INSTANCE = this;
        mHandler = new DataHandler();
        DataPreference.PREF = PreferenceManager.getDefaultSharedPreferences(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mStartButton = (Button) findViewById(R.id.sign_in_button);
        mStartButton.setOnClickListener(this);
        mBabyButton = (ImageButton) findViewById(R.id.btn_baby_talk);
        mBabyButton.setOnClickListener(this);

        mCctvButton = (ImageButton) findViewById(R.id.btn_cctv);
        mCctvButton.setOnClickListener(this);

        mViewerButton = (Button) findViewById(R.id.btn_viewer);
        mViewerButton.setOnClickListener(this);


        if (Config.GOOGLE_SERVICE_ENABLE_DEVICE) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(getString(R.string.server_client_id))
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
        }
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    private long backKeyPressedTime;
    private Toast toast;

    public void onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
            toast = Toast.makeText(MainActivity.this, getResources().getString(R.string.backbutton_click_is_finish), Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            finish();
            toast.cancel();
        }
    }

    public class DataHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == Config.INSERT_USER_INFO_HANDLER) {
                connectWebSocket();

            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.d(TAG, "MainActivity onPause Call");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Config.GOOGLE_SERVICE_ENABLE_DEVICE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        Logger.d(TAG, "MainActivity onResume Call");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    private void connectWebSocket() {
        if (mService != null) {
            try {
                mService.connectWebSocket();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void showProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog == null) {
                    mProgressDialog = new ProgressDialog(MainActivity.this);
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.setMessage(getString(R.string.please_wait));
                    mProgressDialog.show();
                }
            }
        });
    }

    private void signIn() {
        if (DataPreference.getMode() == Config.MODE_NONE) {
            Toast.makeText(this, getString(R.string.please_choose_mode), Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress();
        if (Config.GOOGLE_SERVICE_ENABLE_DEVICE) {
            if (!DataPreference.getEasyLogin()) {
                if (mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.clearDefaultAccountAndReconnect();
                }
            }

            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } else {
            String displayName = "김선영";
            String emailString = "tbzmtb@gmail.com";
            String userId = "100398893839570743730";
            String token = "";
            DataPreference.setLoginName(displayName);
            DataPreference.setLoginEmail(emailString);
            DataPreference.setLoginNumber(userId);
            DataPreference.setLoginToken(token);

            DataPreference.setRtcid(userId);
            DataPreference.setPeerRtcid("tbzmtb");


            String type = Config.DEVICE_TYPE_ANDROID_VALUE;
            String uuid = Build.SERIAL;
            String serial_no = Build.SERIAL;
            String password = "";
            String master_rtcid = "";
            String cert_master = "";
            String email = "tbzmtb@gmail.com";
            String cert_email = "";
            String name = "김선영";
            String disable = "0";


            new InsertUserInfoTask(this, mHandler, DataPreference.getRtcid(), type, uuid, serial_no, password, master_rtcid,
                    cert_master, email, cert_email, name, disable).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
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
            String master_rtcid = "";
            String cert_master = "";
            String email = emailString;
            String cert_email = "";
            String name = displayName;
            String disable = "0";

            cancelProgress();

            new InsertUserInfoTask(this, mHandler, DataPreference.getRtcid(), type, uuid, serial_no, password, master_rtcid,
                    cert_master, email, cert_email, name, disable).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        } else {
            cancelProgress();
            Toast.makeText(MainActivity.this, getString(R.string.fail_login), Toast.LENGTH_SHORT).show();
        }
    }

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

    final IDataServiceCallback mCallbcak = new IDataServiceCallback.Stub() {

        @Override
        public void valueChanged(int value, String data) throws RemoteException {
            if (value == Config.HANDLER_MODE_START) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (!json.isNull(Config.PARAM_DESCRIPTION)) {
                        String desciption = json.getString(Config.PARAM_DESCRIPTION);
                        if (desciption.equals(Config.PARAM_SUCCESS_DESCRIPTION)) {
                            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                            startActivity(intent);
                        }
                    } else {
                        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                        startActivity(intent);
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }
    };

    private void setDefaultMode(){
        AllButtonUnselected();
        if(DataPreference.getMode() == Config.MODE_BABY_TALK){
            mBabyButton.setSelected(true);
        }else if(DataPreference.getMode() == Config.MODE_CCTV){
            mCctvButton.setSelected(true);
        }else if(DataPreference.getMode() == Config.MODE_VIEWER){
            mViewerButton.setSelected(true);
        }else{

        }
    }

    private void AllButtonUnselected() {
        mBabyButton.setSelected(false);
        mViewerButton.setSelected(false);
        mCctvButton.setSelected(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.btn_viewer:
                AllButtonUnselected();
                v.setSelected(true);
                DataPreference.setMode(Config.MODE_VIEWER);
                break;
            case R.id.btn_baby_talk:
                AllButtonUnselected();
                v.setSelected(true);
                DataPreference.setMode(Config.MODE_BABY_TALK);
                DataPreference.setPeerMode(Config.MODE_VIEWER);
                break;
            case R.id.btn_cctv:
                AllButtonUnselected();
                v.setSelected(true);
                DataPreference.setMode(Config.MODE_CCTV);
                DataPreference.setPeerMode(Config.MODE_VIEWER);
                break;
        }
    }
}
