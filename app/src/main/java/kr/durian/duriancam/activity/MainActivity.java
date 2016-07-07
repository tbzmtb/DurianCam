package kr.durian.duriancam.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.d(TAG, "MainActivity onCreate call");
        INSTANCE = this;
        mHandler = new DataHandler();
        DataPreference.PREF = PreferenceManager.getDefaultSharedPreferences(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.btn_viewer).setOnClickListener(this);
        findViewById(R.id.btn_camera).setOnClickListener(this);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


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
        unbindService(mConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d(TAG, "MainActivity onResume Call");
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
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

    private void signIn() {
        if (DataPreference.getMode() == Config.MODE_NONE) {
            Toast.makeText(this, getString(R.string.please_choose_mode), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!DataPreference.getEasyLogin()) {
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.clearDefaultAccountAndReconnect();
            }
        }

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
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

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());

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

            String rtcid = getRtcid(emailString, DataPreference.getMode());
            DataPreference.setRtcid(rtcid);
            String type = Config.DEVICE_TYPE_ANDROID_VALUE;
            String uuid = "";
            String serial_no = Build.SERIAL;
            String password = "";
            String master_rtcid = "";
            String cert_master = "";
            String email = emailString;
            String cert_email = "";
            String name = displayName;
            String disable = "0";


            new InsertUserInfoTask(this, mHandler, rtcid, type, uuid, serial_no, password, master_rtcid,
                    cert_master, email, cert_email, name, disable).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        } else {
            // Signed out, show unauthenticated UI.
            Logger.d(TAG, "google login fail");
        }
    }

    private String getRtcid(String email, int mode) {
        return email + "_" + mode;
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
        public void valueChanged(long value) throws RemoteException {
            if (value == Config.MODE_START) {
                Logger.d(TAG, "카메라 시작 ");
//            if (value == Config.MODE_CAMERA) {
//                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
//                startActivity(intent);
//            } else {
//                Intent intent = new Intent(MainActivity.this, ViewerActivity.class);
//                startActivity(intent);
//            }
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.btn_viewer:
                DataPreference.setMode(Config.MODE_VIEWER);
                break;
            case R.id.btn_camera:
                DataPreference.setMode(Config.MODE_CAMERA);
                break;
        }
    }
}
