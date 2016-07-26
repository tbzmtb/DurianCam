package kr.durian.duriancam.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import java.util.Calendar;

/**
 * Created by tbzm on 16. 4. 18.
 */
public class Config {
    public static boolean GOOGLE_SERVICE_ENABLE_DEVICE = true;

    public static final String SERVER_POST_URL = "";
    public static final String WEB_SOCKET_URL = "";
    public static final String STUN_SERVER = "";
    public static final String TURN_SERVER = "";





    public static final String INSERT_USER_INFO_PHP = "insert_user_info_post.php";

    public static final String PARAM_RTCID = "rtcid";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_SUB_TYPE = "subtype";
    public static final String PARAM_ACCEPT = "accept";
    public static final String PARAM_UUID = "uuid";
    public static final String PARAM_SERIAL_NO = "serial_no";
    public static final String PARAM_EQUALS = "=";
    public static final String PARAM_AND = "&";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_MASTER_RTCID = "master_rtcid";
    public static final String PARAM_CERT_MASTER = "cert_master";
    public static final String PARAM_EMAIL = "email";
    public static final String PARAM_CERT_EMAIL = "cert_email";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_DISABLE = "disable";
    public static final String PARAM_CHECK = "check";

    public static final String SERVER_PARAM_VERSION = "version";
    public static final String SERVER_PARAM_DEVICE_TYPE = "devicetype";
    public static final String DEVICE_TYPE_ANDROID_VALUE = "2";
    public static final String SERVER_PARAM_LOGIN = "login";


    public static final int PARKING_DATA = 1000;
    public static final int PARKING_LOT_DATA_HANDLER = PARKING_DATA * 0x01;
    public static final int PARKING_SEVER_DATA_HANDLER = PARKING_DATA * 0x02;
    public static final int INSERT_USER_INFO_HANDLER = PARKING_DATA * 0x03;

    private static int sScreenWidthDP = -1;
    private static int sScreenWidth = -1;
    private static int sScreenHeight = -1;

    public static final String PREF_LOGIN_NAME_KEY = "login_name_key";
    public static final String PREF_LOGIN_EMAIL_KEY = "login_email_key";
    public static final String PREF_LOGIN_NUMBER_KEY = "login_number_key";
    public static final String PREF_LOGIN_TOKEN_KEY = "login_token_key";
    public static final String PREF_RTCID_KEY = "rtcid_key";
    public static final String PREF_PEER_RTCID_KEY = "peer_rtcid_key";
    public static final String PREF_EASY_LOGIN_KEY = "easy_login_key";
    public static final String PREF_SCREEN_ON_OFF_KEY = "keep_screen_on_off";
    public static final String PREF_MODE_KEY = "mode_key";
    public static final String PREF_VIEWER_WILL_CONNECT_MODE_KEY = "viewer_will_connect_mode_key";
    public static final String PREF_OFFER_SEND_DATA = "offer_send_data";
    public static final String PREF_OFFER_SEND_ACK_DATA = "offer_send_ack_data";
    public static final int MODE_BABY_TALK = 1;
    public static final int MODE_CCTV = 3;
    public static final int MODE_SECURE = 5;
    public static final int MODE_VIEWER = 2;
    public static final int MODE_CAMERA = 4;

    public static final int SCREEN_OFF = 0;
    public static final int SCREEN_ON = 1;

    public static final int HANDLER_MODE_START = 10000;
    public static final int HANDLER_MODE_OFFER = 10001;
    public static final int HANDLER_MODE_OFFER_ACK = 10002;
    public static final int HANDLER_MODE_ANSWER = 10003;
    public static final int HANDLER_MODE_CANDIDATE = 10004;
    public static final int HANDLER_MODE_ANSWER_ACK = 10005;
    public static final int HANDLER_MODE_CONFIG_ACK = 10006;
    public static final int HANDLER_MODE_HANGUP = 10007;
    public static final int HANDLER_MODE_HANGUP_ACK = 10008;

//    public static final String STUN_SERVER = "ec2-52-197-115-227.ap-northeast-1.compute.amazonaws.com:7460";
//    public static final String TURN_SERVER = "ec2-52-197-115-227.ap-northeast-1.compute.amazonaws.com:7460:nsturnuser:Tbzm8026";

//    public static final String STUN_SERVER = "pc.ouibot.com:3478";
//    public static final String TURN_SERVER = "pc.ouibot.com:3478:gorst:hero";

    public static final String PARAM_SDP = "sdp";
    public static final String PARAM_SESSION_ID = "sessionid";
    public static final String PARAM_FROM = "from";
    public static final String PARAM_OFFER = "offer";
    public static final String PARAM_OFFER_ACK = "offer_ack";
    public static final String PARAM_HANGUP = "hangup";
    public static final String PARAM_HANGUP_ACK = "hangup_ack";
    public static final String PARAM_ANSWER_ACK = "answer_ack";
    public static final String PARAM_TO = "to";
    public static final String PARAM_MODE = "mode";
    public static final String PARAM_LOGIN_ACK = "login_ack";
    public static final String PARAM_CODE = "code";
    public static final String PARAM_SUCCESS_CODE = "100";
    public static final String PARAM_SUCCESS_DESCRIPTION = "success";
    public static final String PARAM_CHECK_CAMERA_PEER_EXIST = "check_camera_peer_exist";
    public static final String PARAM_PEER_IS_CALLING = "Peer is calling";
    public static final String PARAM_PEER_IS_NOT_LOGIN = "Peer is not login.";
    public static final String PARAM_PEER_IS_CALLING_CODE = "113";
    public static final String PARAM_DESCRIPTION = "description";
    public static final String PARAM_ANSWER = "answer";
    public static final String PARAM_CANDIDATE = "candidate";
    public static final String PARAM_GET_CONFIG_ACK = "getconfig_ack";

    public static int getScreenHeight(Context context) {
        if (sScreenHeight == -1) {
            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            sScreenHeight = size.y;
        }
        return sScreenHeight;
    }

    public static int getScreenHeightInDp(Context context) {
        if (sScreenHeight == -1) {
            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            sScreenHeight = size.y;
        }
        return pixelsToDp(context, sScreenHeight);
    }

    public static int getScreenWidthInDp(Context context) {
        if (sScreenWidthDP == -1) {
            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            sScreenWidthDP = pixelsToDp(context, size.x);
        }
        return sScreenWidthDP;
    }

    public static int getScreenWidth(Context context) {
        if (sScreenWidth == -1) {
            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            sScreenWidth = size.x;
        }

        return sScreenWidth;
    }

    public static float dpToPixels(Context context, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static int pixelsToDp(Context context, float pixels) {
        float density = context.getResources().getDisplayMetrics().densityDpi;
        return Math.round(pixels / (density / 160f));
    }


    public static String milliSecond2Time(String milliSecond) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(Long.parseLong(milliSecond));
        int hr = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);
        String s_hr = String.valueOf(hr);
        String s_min = String.valueOf(min);
        if (hr < 10) {
            s_hr = "0" + hr;
        }
        if (min < 10) {
            s_min = "0" + min;
        }
        return s_hr + ":" + s_min;
    }

    public static String milliSecond2HeaderTime(String milliSecond) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(Long.parseLong(milliSecond));
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        String s_year = String.valueOf(year);
        String s_month = String.valueOf(month);
        String s_day = String.valueOf(day);
        if (month < 10) {
            s_month = "0" + month;
        }
        return s_year + "/" + s_month + "/" + s_day;
    }


    public static String milliSecond2Time(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        int hr = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);
        String s_hr = String.valueOf(hr);
        String s_min = String.valueOf(min);
        if (hr < 10) {
            s_hr = "0" + hr;
        }
        if (min < 10) {
            s_min = "0" + min;
        }
        return s_hr + ":" + s_min;
    }

    public static String getAppVersionName(Context context) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo != null) {
            return packageInfo.versionName;
        } else {
            return "";
        }
    }
}
