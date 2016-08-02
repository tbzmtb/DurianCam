package kr.durian.duriancam.activity;

import android.app.PendingIntent;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import kr.durian.duriancam.R;
import kr.durian.duriancam.asynctask.GetInAppPaymentTask;
import kr.durian.duriancam.asynctask.InsertUserInfoTask;
import kr.durian.duriancam.asynctask.SetInAppPaymentTask;
import kr.durian.duriancam.googleutil.IabHelper;
import kr.durian.duriancam.googleutil.IabResult;
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
    private IInAppBillingService mBillingService;
    private IabHelper mHelper;
    private Button mDetectionDimView;
    private PaymentHandler mPaymentHandler;
    private String BILLING_INTENT = "com.android.vending.billing.InAppBillingService.BIND";
    private String BILLING_PACKAGE = "com.android.vending";
    private String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmRiKtyCDD2NZUxXX80gd5a0/PbGW+qBm733z92xt+j2YW/itF+zFrggBbAh5fHDIVoe3Vr9em2BEWGdiuXIe5027EICh3o8c7uKxyRISXuqWoTyzQXP576YoohDnOMLX+N1AVEJp5SXDbZ/ubV1ASAgijZuOOY7XQZ/v/UdcObzrfT5RaUkJGbKFp4iik3Mdbj+C7Hl6581uPQElTpxTjrgsJay6cIsrX4lnpUfAi6fxfw8tpm7PJfR1TuAVJC7ijsom2cv6LTyuuWFDvgtCvy7YoZcsSLc51NwJ6JwNM5TebloVo2bcQx7702gSMCLgydJXc9UWboabZc+kaid/xQIDAQAB+Ndh+kpqHTc8FbzUwrq3rmHhPDTFAlZInZhnW8zwSE/eDhnMHI3pyWmpO/0RxEQOPJxcYqpFpYiAhcyc3dLUowAFTuyVx4uoCncyXKRSwyMBM3B+RFW6ntt6FUlwEg0b491Uchc2Blz4a2utOKbNj4XLcMfJrQHIR6yP1ljoQAHwM0bX1dos0afexCCzf6u5YlJi8ng4CQ56SRUbOTHGRhu/SA+8A0+tbQP7vr7HCb1xSfs7KL0N2pC6H9Gy1JY4bkVpvOxf6zFtTX3x9wa7Kb2RD2qhAh9k8HN7wIDAQAB";
    private final String PAYMENT_ID = "durian_detect_pay";
    private final String PATMENT_OK = "1";
    private final String PARAM_PAYMENT = "payment";

    ServiceConnection mBillingServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBillingService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBillingService = IInAppBillingService.Stub.asInterface(service);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_mode_select);
        DataPreference.PREF = PreferenceManager.getDefaultSharedPreferences(this);
        mPaymentHandler = new PaymentHandler();
        Intent intent = new Intent(BILLING_INTENT);
        intent.setPackage(BILLING_PACKAGE);
        bindService(intent, mBillingServiceConn, Context.BIND_AUTO_CREATE);


        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.enableDebugLogging(true);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Logger.d(TAG, "fail");
                }

                // 구매목록을 초기화하는 메서드입니다.
                // v3으로 넘어오면서 구매기록이 모두 남게 되는데 재구매 가능한 상품( 게임에서는 코인같은아이템은 ) 구매후 삭제해주어야 합니다.
                // 이 메서드는 상품 구매전 혹은 후에 반드시 호출해야합니다. ( 재구매가 불가능한 1회성 아이템의경우 호출하면 안됩니다 )
                AlreadyPurchaseItems();
            }
        });

        mHandler = new DataHandler();
        Logger.d(TAG, "ViewerModeSelectActivity onCreate call");
        mBabyButton = (Button) findViewById(R.id.btn_baby_talk);
        mBabyButton.setOnClickListener(this);

        mCctvButton = (Button) findViewById(R.id.btn_cctv);
        mCctvButton.setOnClickListener(this);
        mDetectionDimView = (Button) findViewById(R.id.detection_dim_layout);
        mDetectionDimView.setOnClickListener(this);

        if (DataPreference.getInAppPayment()) {
            setDetectDimViewVisible(false);
        } else {
            setDetectDimViewVisible(true);

        }
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

    private void setDetectDimViewVisible(boolean enable) {
        Logger.d(TAG, "setDetectDimViewVisible call value == " + enable);
        if (mDetectionDimView == null) {
            Logger.d(TAG, "mDetectionDimView == " + mDetectionDimView);

            return;
        }
        if (enable) {
            mDetectionDimView.setVisibility(View.VISIBLE);

        } else {
            mDetectionDimView.setVisibility(View.GONE);

        }
    }

    private class PaymentHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg != null) {
                if (msg.what == Config.SET_IN_APP_PAY_MENT_HANDLER_KEY) {
                    String result = (String) msg.obj;
                    if (result.contains(Config.PARAM_SUCCESS_DESCRIPTION)) {
                        Logger.d(TAG, "payment complete");
                        DataPreference.setInAppPayment(true);
                        setDetectDimViewVisible(false);
                    }
                } else if (msg.what == Config.GET_IN_APP_PAY_MENT_HANDLER_KEY) {
                    String result = (String) msg.obj;
                    try {
                        JSONArray json = new JSONArray(result);
                        if (json.length() > 0) {
                            String value = json.getJSONObject(0).getString(PARAM_PAYMENT);
                            if (value.equals(PATMENT_OK)) {
                                Logger.d(TAG, "payment sucess");
                                DataPreference.setInAppPayment(true);
                                setDetectDimViewVisible(false);
                            } else {
                                Logger.d(TAG, "not payment");
                                startBuy(PAYMENT_ID);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    public void AlreadyPurchaseItems() {
        try {
            while (mBillingService == null) {
                Thread.sleep(400);
            }
            Bundle ownedItems = mBillingService.getPurchases(5, getPackageName(), "inapp", null);
            int response = ownedItems.getInt("RESPONSE_CODE");
            if (response == 0) {

                ArrayList purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                String[] tokens = new String[purchaseDataList.size()];
                for (int i = 0; i < purchaseDataList.size(); ++i) {
                    String purchaseData = (String) purchaseDataList.get(i);
                    JSONObject jo = new JSONObject(purchaseData);
                    tokens[i] = jo.getString("purchaseToken");
                    // 여기서 tokens를 모두 컨슘 해주기
                    mBillingService.consumePurchase(5, getPackageName(), tokens[i]);
                }
            }
            // 토큰을 모두 컨슘했으니 구매 메서드 처리
        } catch (Exception e) {
            e.printStackTrace();
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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == RC_SIGN_IN) {
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(result);
            } else if (requestCode == 1001) {
                if (mHelper == null) {
                    Logger.d(TAG, "mHelper = " + mHelper);
                    return;
                }
                if (resultCode == RESULT_OK) {
                    if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
                        super.onActivityResult(requestCode, resultCode, data);

                        int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
                        Logger.d(TAG, "responseCode = " + responseCode);
                        new SetInAppPaymentTask(ViewerModeSelectActivity.this, mPaymentHandler).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            Logger.d(TAG, "valueChanged call");
            if (value == Config.HANDLER_MODE_START) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (!json.isNull(Config.PARAM_DESCRIPTION)) {
                        String desciption = json.getString(Config.PARAM_DESCRIPTION);
                        if (desciption.equals(Config.PARAM_SUCCESS_DESCRIPTION)) {
                            startCameraActivity();
                        } else {
                            //nothing to do
                        }
                    } else {
                        startCameraActivity();

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void startCameraActivity() {
        Intent intent = new Intent(ViewerModeSelectActivity.this, CameraActivity.class);
        startActivity(intent);

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
        if (mService != null) {
            try {
                mService.unregisterCallback(mCallbcak);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(mConnection);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBillingServiceConn != null) {
            unbindService(mBillingServiceConn);
        }
        if (mHelper != null) {
            mHelper.dispose();
        }
        mHelper = null;
    }

    public void startBuy(String id_item) {
        try {
            Bundle buyIntentBundle = mBillingService.getBuyIntent(5, getPackageName(), id_item, "inapp", "test");
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

            if (pendingIntent != null) {
                startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
//                mHelper.launchPurchaseFlow(this, getPackageName(), 1001, mPurchaseFinishedListener, "test");

            } else {
                // 결제가 막혔다면
                Logger.d(TAG, "pendingIntent = " + pendingIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
//        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
//            repairingPictures(mAdpater.getSelectedItem());
    // 여기서 아이템 추가 해주시면 됩니다.
    // 만약 서버로 영수증 체크후에 아이템 추가한다면, 서버로 purchase.getOriginalJson() , purchase.getSignature() 2개 보내시면 됩니다.
//}
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
            case R.id.detection_dim_layout: {
                if (DataPreference.getRtcid() == null) {
                    Toast.makeText(ViewerModeSelectActivity.this, getString(R.string.you_have_to_google_login_first), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!DataPreference.getInAppPayment()) {
                    new GetInAppPaymentTask(ViewerModeSelectActivity.this, mPaymentHandler).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
//                Toast.makeText(ViewerModeSelectActivity.this, getString(R.string.not_supported_right_now),Toast.LENGTH_SHORT).show();
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

            mService = null;
        }
    };
}
