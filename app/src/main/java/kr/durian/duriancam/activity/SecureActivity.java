package kr.durian.duriancam.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.durian.duriancam.R;
import kr.durian.duriancam.asynctask.GetDeviceTokenTask;
import kr.durian.duriancam.service.DataService;
import kr.durian.duriancam.service.IDataService;
import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.DataPreference;
import kr.durian.duriancam.util.Logger;
import kr.durian.duriancam.util.MediaScanner;

/**
 * Created by sunyungkim on 16. 7. 26..
 */
public class SecureActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private IDataService mService;
    private final String TAG = getClass().getName();
    private JavaCameraView mOpenCVCameraView;
    private BackgroundSubtractorMOG2 mBgSubtractor;
    private Mat mRgb;
    private Mat mFGMask;
    private Mat mGray;
    private List<MatOfPoint> mContours;
    private ArrayList<Integer> mSaveContoursData;
    private double mLearningRate = 0.5;
    private String mRecordingEnable = DataPreference.getVideoRecordingEnable();
    private String mDetectSenesitivity = DataPreference.getDetectSensitivity();
    private int mVideoRecordingTimeSecond = 60;
    private int mSecurityStartTimeSecond = 10;
    private int mRretryDetectTime = 3;
    private TimerHandler mTimeHandler;
    private TextView mOverlayMainText;
    private TextView mOverlaySubText;
    private int START_TIME = 0;
    private int DEFAULT_TIME = -1;
    private int mSecureStartCount = -1;
    private boolean isRecording = false;
    private RelativeLayout mDimLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "SecureActivity oncreate call");
        getSupportActionBar().hide();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (DataPreference.getMode() == Config.MODE_VIEWER) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            if (DataPreference.getKeepSceenOn()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
        setContentView(R.layout.activity_secure);
        mOverlayMainText = (TextView) findViewById(R.id.overlay_main_text);
        mOverlaySubText = (TextView) findViewById(R.id.overlay_sub_text);
        mDimLayout = (RelativeLayout) findViewById(R.id.secure_dim_view);
        setDimLayout();

    }

    private void setDimLayout(){
        if(DataPreference.getSecureDisplayEnable().equals(Config.DISPLAY_HIDE_ON)){
            mDimLayout.setVisibility(View.VISIBLE);
        }else{
            mDimLayout.setVisibility(View.GONE);
        }

    }

    private void saveContoursSize(int data) {
        if (mSaveContoursData != null) {
            if (mSaveContoursData.size() > 5) {
                mSaveContoursData.remove(0);
            }
            mSaveContoursData.add(data);
        }
    }

    private int getPreviousData() {
        if (mSaveContoursData != null) {
            if (mSaveContoursData.size() > 4) {
                return mSaveContoursData.get(mSaveContoursData.size() - 4);
            }
        }
        return 0;
    }

    private int getPrePreviousData() {
        if (mSaveContoursData != null) {
            if (mSaveContoursData.size() > 4) {
                return mSaveContoursData.get(mSaveContoursData.size() - 5);
            }
        }
        return 0;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mContours.clear();
        mGray = inputFrame.gray();
        Imgproc.cvtColor(mGray, mRgb, Imgproc.COLOR_GRAY2RGB);
        mBgSubtractor.apply(mRgb, mFGMask, mLearningRate);
        Imgproc.erode(mFGMask, mFGMask, new Mat());
        Imgproc.dilate(mFGMask, mFGMask, new Mat());
        Imgproc.findContours(mFGMask, mContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        if (mSecureStartCount == DEFAULT_TIME) {
            Logger.d(TAG, "idle state = " + mSecureStartCount);
            return mRgb;
        }
        saveContoursSize(mContours.size());
        Logger.d(TAG, "mContours.size = " + mContours.size());
        if (mSecureStartCount == START_TIME) {
            if (mContours.size() > getDetectSensitivityValue()) {
                if (getPreviousData() > 0 && getPrePreviousData() > 0) {
                    mSecureStartCount = DEFAULT_TIME;
                    Logger.d(TAG, "감지!!");
                    startTextColorBlink();
                    takePicture();
                }
            }
        }
        return mRgb;
    }

    public int getDetectSensitivityValue() {
        return Integer.parseInt(mDetectSenesitivity);

    }

    private String getPictureFileName(long time) {
        Date now = new Date(time);
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String formattedDate = df.format(now);
        String fileExtention = Config.IMAGE_FILE_EXTENTION;
        return formattedDate + fileExtention;
    }

    private void takePicture() {
        if (mOpenCVCameraView == null) {
            return;
        }
        final long time = getCurrentTime();
        new Thread(new Runnable() {
            @Override
            public void run() {
                mOpenCVCameraView.pictureTaken(Config.getDirectory() + getPictureFileName(time), null, String.valueOf(time));
            }
        }).start();
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void startTextColorBlink() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mOverlayMainText != null) {
                    mOverlayMainText.setTextColor(Color.YELLOW);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mOverlayMainText.setTextColor(Color.WHITE);
                        }
                    }, 1000);
                }
            }
        });
    }

    @Override
    public void onCameraViewStopped() {
        Logger.d(TAG, "onCameraViewStopped");
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        Logger.d(TAG, "onCameraViewStarted");
        mBgSubtractor = Video.createBackgroundSubtractorMOG2();
        mRgb = new Mat();
        mFGMask = new Mat();
        mGray = new Mat();
        mContours = new ArrayList<>();
        mSaveContoursData = new ArrayList<>();
        startSecureTimer(mSecurityStartTimeSecond);
    }

    private void stopTimerHandler() {
        setOverLaySubText("");
        if (mTimeHandler != null) {
            mTimeHandler.removeCallbacksAndMessages(null);
            mTimeHandler = null;
        }
    }

    private void startTimerHandler() {
        if (mTimeHandler == null) {
            mTimeHandler = new TimerHandler();
        }
        mTimeHandler.sendMessageDelayed(new Message(), 1000);

    }

    public class TimerHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            if (mSecureStartCount <= START_TIME) {
                stopTimerHandler();
                return;
            }
            mSecureStartCount--;
            setOverLaySubText(mSecureStartCount);
            startTimerHandler();
            super.handleMessage(msg);
        }

    }

    private void startSecureTimer(int secureTime) {
        setOverLayMainText(getResources().getString(R.string.secure_mode_text));
        mSecureStartCount = secureTime;

        if (mSecureStartCount <= START_TIME) {
            return;
        }
        stopTimerHandler();
        startTimerHandler();
    }

    public void setOverLaySubText(final String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mOverlaySubText != null) {
                    mOverlaySubText.setText(data);
                }

            }
        });
    }

    public void setOverLaySubText(final int data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mOverlaySubText != null) {
                    mOverlaySubText.setText("" + data);
                }

            }
        });
    }

    public void setOverLayMainText(final String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mOverlayMainText != null) {
                    mOverlayMainText.setText(data);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        DataPreference.setSecuringMode(true);
        IntentFilter intentFilter = new IntentFilter(Config.BROADCAST_FINISH_SECURE);
        intentFilter.addAction(Config.BROADCAST_CHANGE_SECURE_OPTION);
        registerReceiver(mSecureReceiver, intentFilter);
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        DataPreference.setSecuringMode(false);
        release();
        unregisterReceiver(mSecureReceiver);
        sendSecureFinishData();
        unbindService(mConnection);
    }

    private BroadcastReceiver mSecureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Config.BROADCAST_FINISH_SECURE)) {
                finish();
            } else if (intent.getAction().equals(Config.BROADCAST_CHANGE_SECURE_OPTION)) {
                Logger.d(TAG, "DataPreference.getVideoRecordingEnable()" + DataPreference.getVideoRecordingEnable());
                Logger.d(TAG, "DataPreference.getDetectSensitivity()" + DataPreference.getDetectSensitivity());
                mRecordingEnable = DataPreference.getVideoRecordingEnable();
                mDetectSenesitivity = DataPreference.getDetectSensitivity();
                setDimLayout();
                if (!isRecording && mOpenCVCameraView != null) {
                    startSecureTimer(mSecurityStartTimeSecond);
                }
            }
        }
    };

    @Override
    public void finish() {
        super.finish();
    }

    private void sendSecureFinishData() {
        Logger.d(TAG, "sendSecureFinishData call");
        try {
            if (mService != null) {
                JSONObject data = new JSONObject();
                data.put(Config.PARAM_TYPE, Config.PARAM_GET_CONFIG_ACK);
                data.put(Config.PARAM_SESSION_ID, System.currentTimeMillis());
                data.put(Config.PARAM_FROM, DataPreference.getPeerRtcid());
                data.put(Config.PARAM_TO, DataPreference.getRtcid());
                data.put(Config.PARAM_DESCRIPTION, Config.PARAM_FINISH_SECURE);
                mService.sendData(data.toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void release() {
        if (mOpenCVCameraView != null) {
            mOpenCVCameraView.stopRecording();
            mOpenCVCameraView.disableView();
            mOpenCVCameraView = null;
        }
    }

//    private void registerServiceCallback() {
//        try {
//            mService.registerCallback(mCallbcak);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//    }

//    private void unregisterServiceCallback() {
//        try {
//            boolean b = mService.unregisterCallback(mCallbcak);
//            Logger.d(TAG, "unregisterCallback1 call = ", b);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//    }

//    final IDataServiceCallback mCallbcak = new IDataServiceCallback.Stub() {
//        @Override
//        public void valueChanged(int what, String data) throws RemoteException {
//
//        }
//    };

    private void initCamera() {
        Logger.i(TAG, "initCamera mOpenCVCameraView = " + mOpenCVCameraView);
        if (mOpenCVCameraView == null) {
            mOpenCVCameraView = (JavaCameraView) findViewById(R.id.surface_camera);
            mOpenCVCameraView.setCvCameraViewListener(SecureActivity.this);
            mOpenCVCameraView.setJavaCamViewListener(onMediaListner);
            mOpenCVCameraView.enableView();
        }
    }

    private JavaCameraView.OnJavaCamViewListener onMediaListner = new JavaCameraView.OnJavaCamViewListener() {
        @Override
        public void onPictureTaken(final String file, final String time, final String err) {
            if (err.equals("sdcard_error")) {
                Logger.d(TAG, "sdcard_error == ");
                finish();
                return;
            }
            if (file == null || file.equals("")) {
                Logger.i(TAG, "file == " + file);
                finish();
                return;
            }
            Logger.i(TAG, "photo file == " + file);
            new Thread(new Runnable() {
                @Override
                public void run() {
//                    setData2Database(OuiBotPreferences.getLoginId(SecureActivity.this), file, String.valueOf(getCurrentMode()), time);
                    new MediaScanner(SecureActivity.this, new File(file));
                    new GetDeviceTokenTask(SecureActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                    sendFile2Master(file, getCurrentMode(), time);
                }
            }).start();
            if (mRecordingEnable.equals(Config.VIDEO_RECORDING_ON)) {
                setOverLaySubText(getResources().getString(R.string.video_recording));
                isRecording = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mOpenCVCameraView.beginRecording(getApplicationContext(), Config.getDirectory() + getVideoFileName(getCurrentTime()), mVideoRecordingTimeSecond);
                    }
                }).start();
            } else {
                startSecureTimer(mRretryDetectTime);
            }
        }

        //영상촬영 후 다시 시작하기 위함(영상 촬영중 SecureAcitivty 가 종료된 경우도 호출됨)
        //시작 카운트 다운 시 onRecordingStop이 호출 될 수 있음에 유의
        @Override
        public void onRecordingStop(final String file) {
            Logger.i(TAG, "onRecordingStop call == " + file);

            isRecording = false;
            if (file != null) {
                new MediaScanner(SecureActivity.this, new File(file));
            }
            setOverLaySubText("");
            if (DataPreference.getSecuringMode()) {
                mOpenCVCameraView.reconnectCamera();
                startSecureTimer(mRretryDetectTime);
            }
        }

        @Override
        public void onRecordingError() {
            Logger.i(TAG, "onRecordingError call !!!!");
            finish();
        }
    };

    private String getVideoFileName(long time) {
        Date now = new Date(time);
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String formattedDate = df.format(now);
        String fileExtention = Config.VIDEO_FILE_EXTENTION;
        return formattedDate + fileExtention;
    }

    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                mService = IDataService.Stub.asInterface(service);
                setOverLayMainText(getString(R.string.ready_to_detect_mode));
                mRecordingEnable = DataPreference.getVideoRecordingEnable();
                mDetectSenesitivity = DataPreference.getDetectSensitivity();
                Logger.d(TAG, "DataPreference.getVideoRecordingEnable()" + DataPreference.getVideoRecordingEnable());
                Logger.d(TAG, "DataPreference.getDetectSensitivity()" + DataPreference.getDetectSensitivity());
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!OpenCVLoader.initDebug()) {
                            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, SecureActivity.this, mLoaderCallback);
                        } else {
                            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                        }
                    }
                }, 5000);


            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Logger.i(TAG, "OpenCV Manager Connected");
                    initCamera();
                    break;
                case LoaderCallbackInterface.INIT_FAILED:
                    Logger.i(TAG, "Init Failed");
                    break;
                case LoaderCallbackInterface.INSTALL_CANCELED:
                    Logger.i(TAG, "Install Cancelled");
                    break;
                case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
                    Logger.i(TAG, "Incompatible Version");
                    break;
                case LoaderCallbackInterface.MARKET_ERROR:
                    Logger.i(TAG, "Market Error");
                    break;
                default:
                    Logger.i(TAG, "OpenCV Manager Install");
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
}
