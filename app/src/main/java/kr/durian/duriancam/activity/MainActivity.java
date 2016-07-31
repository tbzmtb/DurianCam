package kr.durian.duriancam.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.tools.debugger.Main;

import java.io.IOException;

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
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private Button mViewerButton;
    private Button mCameraButton;
    private Button mStartButton;
    private ProgressDialog mProgressDialog;
    private long mBackKeyPressedTime;
    private Toast mToast;
    private GoogleCloudMessaging gcm;
    private String regid = null;
    private String SENDER_ID = "175455013954";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.d(TAG, "MainActivity onCreate call");
        INSTANCE = this;
        mHandler = new DataHandler();
        DataPreference.PREF = PreferenceManager.getDefaultSharedPreferences(this);
        mStartButton = (Button) findViewById(R.id.sign_in_button);
        mStartButton.setOnClickListener(this);


        mViewerButton = (Button) findViewById(R.id.btn_viewer);
        mViewerButton.setOnClickListener(this);
        mCameraButton = (Button) findViewById(R.id.btn_camera);
        mCameraButton.setOnClickListener(this);

        if (Config.GOOGLE_SERVICE_ENABLE_DEVICE) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(getString(R.string.web_client_id))
//                    .requestIdToken(getString(R.string.android_release_client_id))mBackKeyPressedTime
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
        }
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(MainActivity.this);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;
                    Log.e(",", "token = " + regid);

                    // 서버에 발급받은 등록 아이디를 전송한다.
                    // 등록 아이디는 서버에서 앱에 푸쉬 메시지를 전송할 때 사용된다.
                    sendRegistrationIdToBackend();

                    // 등록 아이디를 저장해 등록 아이디를 매번 받지 않도록 한다.
                    storeRegistrationId(MainActivity.this, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }


        }.execute(null, null, null);
    }

    private void storeRegistrationId(Context context, String regid) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regid);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    private void sendRegistrationIdToBackend() {
        DataPreference.setPushToken(regid);
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }

        // 앱이 업데이트 되었는지 확인하고, 업데이트 되었다면 기존 등록 아이디를 제거한다.
        // 새로운 버전에서도 기존 등록 아이디가 정상적으로 동작하는지를 보장할 수 없기 때문이다.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private SharedPreferences getGCMPreferences(Context context) {
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    public void onBackPressed() {
        if (System.currentTimeMillis() > mBackKeyPressedTime + 2000) {
            mBackKeyPressedTime = System.currentTimeMillis();
            mToast = Toast.makeText(MainActivity.this, getResources().getString(R.string.backbutton_click_is_finish), Toast.LENGTH_SHORT);
            mToast.show();
            return;
        }
        if (System.currentTimeMillis() <= mBackKeyPressedTime + 2000) {
            finish();
            mToast.cancel();
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
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(MainActivity.this);

            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Logger.d(TAG, "No valid Google Play Services APK found.");
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
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

    private void setRegisterCallback(){
        try {
            mService.registerCallback(mCallbcak);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void unRegisterCallback(){
        if (mService != null) {
            try {
                mService.unregisterCallback(mCallbcak);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                mService = IDataService.Stub.asInterface(service);
                setDefaultMode();

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;

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
            String pushToken = DataPreference.getPushToken();
            String payment = "0";
            String email = "tbzmtb@gmail.com";
            String cert_email = "";
            String name = "김선영";
            String disable = "0";
            Logger.d(TAG, "pushToken = " + pushToken);
            Logger.d(TAG, "pushToken = " + regid);
            new InsertUserInfoTask(this, mHandler, DataPreference.getRtcid(), type, uuid, serial_no, password, pushToken,
                    payment, email, cert_email, name, disable).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
            String pushToken = DataPreference.getPushToken();
            String payment = "0";
            String email = emailString;
            String cert_email = "";
            String name = displayName;
            String disable = "0";
            Logger.d(TAG, "pushToken = " + pushToken);
            Logger.d(TAG, "pushToken = " + regid);
            cancelProgress();

            new InsertUserInfoTask(this, mHandler, DataPreference.getRtcid(), type, uuid, serial_no, password, pushToken,
                    payment, email, cert_email, name, disable).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

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
            unRegisterCallback();
            Logger.d(TAG,"valueChanged call");
            if (value == Config.HANDLER_MODE_START) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (!json.isNull(Config.PARAM_DESCRIPTION)) {
                        String desciption = json.getString(Config.PARAM_DESCRIPTION);
                        if (desciption.equals(Config.PARAM_SUCCESS_DESCRIPTION)) {
                            if (DataPreference.getMode() == Config.MODE_CAMERA) {
                                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                                startActivity(intent);
                            }
                        }
                    } else {
                        if (DataPreference.getMode() == Config.MODE_CAMERA) {
                            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                            startActivity(intent);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void setDefaultMode() {
        AllButtonUnselected();
        if (DataPreference.getMode() == Config.MODE_CAMERA) {
            mCameraButton.setSelected(true);
        } else {
            mViewerButton.setSelected(true);
        }
    }

    private void AllButtonUnselected() {
        mViewerButton.setSelected(false);
        mCameraButton.setSelected(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                setRegisterCallback();
                if (DataPreference.getMode() == Config.MODE_VIEWER) {
                    Intent intent = new Intent(MainActivity.this, ViewerModeSelectActivity.class);
                    startActivity(intent);
                } else {
                    signIn();
                }
                break;
            case R.id.btn_viewer:
                AllButtonUnselected();
                v.setSelected(true);
                DataPreference.setMode(Config.MODE_VIEWER);
                break;
            case R.id.btn_camera:
                AllButtonUnselected();
                v.setSelected(true);
                DataPreference.setMode(Config.MODE_CAMERA);
                break;
        }
    }
}
