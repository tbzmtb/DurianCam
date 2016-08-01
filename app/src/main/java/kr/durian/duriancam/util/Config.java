package kr.durian.duriancam.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by tbzm on 16. 4. 18.
 */
public class Config {
    public static boolean GOOGLE_SERVICE_ENABLE_DEVICE = true;

    public static final String SERVER_POST_URL = "http://52.193.65.182/manager/";
    public static final String WEB_SOCKET_URL = "ws://52.193.65.182:7450";
    public static final String STUN_SERVER = "52.193.65.182:3478";
    public static final String TURN_SERVER = "52.193.65.182:3478:nsturnuser:tbzm8026";


    public static final String INSERT_USER_INFO_PHP = "insert_user_info_post.php";
    public static final String GET_PUSH_TOKEN_PHP = "get_push_token_post.php";
    public static final String GET_IN_APP_PAY_MENT_PHP = "get_in_app_pay_ment_post.php";
    public static final String SET_IN_APP_PAY_MENT_PHP = "set_in_app_pay_ment_post.php";

    public static final String PUSH_IMAGE_TIME_INTENT_KEY = "push_image_time_key";

    public static final int NONE_SELECT = 0;
    public static final int SELECT = 1;

    public static final int NONE_DELETE = 0;
    public static final int DELETE = 1;

    public static final String PARAM_RTCID = "rtcid";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_IMAGE_CUT = "imgcut";
    public static final String PARAM_EVENT = "event";
    public static final String PARAM_IMAGE_TIME_KEY = "image_time_key";
    public static final String PARAM_VIDEO_ON_OFF_VALUE = "video_on_off";
    public static final String PARAM_DETECT_SENSITIVITY_VALUE = "detect_sensitivity";
    public static final String PARAM_DETECT_DISPLAY_ON_OFF_VALUE = "display_hide_value";
    public static final String PARAM_DETECT_MODE_ON_OFF_VALUE = "detect_mode_on_off_value";
    public static final String PARAM_SUB_TYPE = "subtype";
    public static final String PARAM_ACCEPT = "accept";
    public static final String PARAM_UUID = "uuid";
    public static final String PARAM_SERIAL_NO = "serial_no";
    public static final String PARAM_EQUALS = "=";
    public static final String PARAM_AND = "&";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_PAYMENT = "payment";
    public static final String PARAM_EMAIL = "email";
    public static final String PARAM_CERT_EMAIL = "cert_email";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_DISABLE = "disable";
    public static final String PARAM_TOKEN = "token";
    public static final String PARAM_CHECK = "check";
    public static final String PARAM_SIZE = "size";
    public static final String PARAM_FINISH_SECURE = "finish_secure";
    public static final String SERVER_PARAM_VERSION = "version";
    public static final String SERVER_PARAM_DEVICE_TYPE = "devicetype";
    public static final String DEVICE_TYPE_ANDROID_VALUE = "2";
    public static final String SERVER_PARAM_LOGIN = "login";

    public static final String BROADCAST_FINISH_SECURE = "broadcast_finish_secure";
    public static final String BROADCAST_CHANGE_SECURE_OPTION = "broadcast_change_secure_option";
    public static final String BROADCAST_SECURE_DETECTED = "broadcast_seucre_detected";
    public static final int DURIAN_CAM_DATA = 1000;
    public static final int INSERT_USER_INFO_HANDLER = DURIAN_CAM_DATA * 0x01;

    public static final String PREF_LOGIN_NAME_KEY = "login_name_key";
    public static final String PREF_IN_APP_PAYMENT_KEY = "in_app_payment_key";
    public static final String PREF_PUSH_ENABLE_KEY = "push_enable_key";
    public static final String PREF_PUSH_TOKEN_KEY = "push_token_key";
    public static final String PREF_VIDEO_RECORDING_ENABLE_KEY = "video_recording_enable_key";
    public static final String PREF_DETECT_SENSITIVITY_KEY = "detect_sensitivity_key";
    public static final String PREF_DISPLAY_HIDE_KEY = "dispaly_hide_key";
    public static final String PREF_IS_SECURING_MODE = "is_secureing_mode";
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

    public static final String VIDEO_RECORDING_OFF = "0";
    public static final String VIDEO_RECORDING_ON = "1";
    public static final String VIDEO_RECORDING_DEFAULT_VALUE = VIDEO_RECORDING_ON;

    public static final String VIDEO_DETECT_SENSITIVITY_DEFAULT_VALUE = "50";
    public static final String DISPLAY_HIDE_OFF = "0";
    public static final String DISPLAY_HIDE_ON = "1";
    public static final String DISPLAY_HIDE_DEFAULT_VALUE = DISPLAY_HIDE_OFF;

    public static final String MOTION_DETECT_MODE_OFF = "0";
    public static final String MOTION_DETECT_MODE_ON = "1";

    public static final int HANDLER_MODE_START = 10000;
    public static final int HANDLER_MODE_OFFER = 10001;
    public static final int HANDLER_MODE_OFFER_ACK = 10002;
    public static final int HANDLER_MODE_ANSWER = 10003;
    public static final int HANDLER_MODE_CANDIDATE = 10004;
    public static final int HANDLER_MODE_ANSWER_ACK = 10005;
    public static final int HANDLER_MODE_CONFIG_ACK = 10006;
    public static final int HANDLER_MODE_HANGUP = 10007;
    public static final int HANDLER_MODE_HANGUP_ACK = 10008;
    public static final int HANDLER_MODE_EVENT = 10009;

    public static final int SET_IN_APP_PAY_MENT_HANDLER_KEY = 5001;
    public static final int GET_IN_APP_PAY_MENT_HANDLER_KEY = 5002;
    public static final String PARAM_SDP = "sdp";
    public static final String PARAM_SESSION_ID = "sessionid";
    public static final String PARAM_FROM = "from";
    public static final String PARAM_OFFER = "offer";
    public static final String PARAM_OFFER_ACK = "offer_ack";
    public static final String PARAM_HANGUP = "hangup";
    public static final String PARAM_HANGUP_ACK = "hangup_ack";
    public static final String PARAM_ANSWER_ACK = "answer_ack";
    public static final String PARAM_TO = "to";
    public static final String PARAM_FILE_NAME = "imagepath";
    public static final String PARAM_TIME = "time";
    public static final String PARAM_MODE = "mode";
    public static final String PARAM_LOGIN_ACK = "login_ack";
    public static final String PARAM_CODE = "code";
    public static final String PARAM_SUCCESS_CODE = "100";
    public static final String PARAM_SUCCESS_DESCRIPTION = "success";
    public static final String PARAM_CHECK_CAMERA_PEER_EXIST = "check_camera_peer_exist";
    public static final String PARAM_SECURE_IMAGE_REQUEST = "secure_image_request";
    public static final String PARAM_CHECK_CAMERA_PEER_EXIST_FOR_SECURE = "check_camera_peer_exist_for_secure";
    public static final String PARAM_GET_TOTAL_SECURE_IMAGE_DATA = "get_total_secure_image_data";
    public static final String PARAM_SECURE_CHANGE_OPTION = "secure_change_option";
    public static final String PARAM_PEER_IS_CALLING = "Peer is calling";
    public static final String PARAM_PEER_IS_SECURING = "Peer is securing";
    public static final String PARAM_PEER_IS_NOT_LOGIN = "Peer is not login.";
    public static final String PARAM_PEER_IS_CALLING_CODE = "113";
    public static final String PARAM_DESCRIPTION = "description";
    public static final String PARAM_CONFIG = "config";
    public static final String PARAM_ANSWER = "answer";
    public static final String PARAM_CANDIDATE = "candidate";
    public static final String PARAM_GET_CONFIG_ACK = "getconfig_ack";
    public static final String IMAGE_FILE_EXTENTION = ".jpg";
    public static final String VIDEO_FILE_EXTENTION = ".3gp";
    public static final int MAX_BITMAP_SIZE = 1280;
    public final static long SIZE_KB = 1024L;
    public final static long SIZE_MB = SIZE_KB * SIZE_KB;

    public static String getSaveImageFileExternalDirectory() {
        File fileRoot = new File(Environment.getExternalStorageDirectory() + File.separator + "DurianCam");
        if (!fileRoot.exists()) {
            fileRoot.mkdir();
        }
        return fileRoot.getPath() + File.separator;
    }

    public static long getSDCardAvailableSpaceInMB() {
        return getExternalAvailableSpaceInBytes() / SIZE_MB;
    }

    public static long getExternalAvailableSpaceInBytes() {
        try {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
            return file.getFreeSpace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getRoot() {
        File fileRoot = new File(Environment.getExternalStorageDirectory() + File.separator + "DurianDetectedMedia");
        if (!fileRoot.exists()) {
            fileRoot.mkdir();
        }
        return fileRoot.getPath() + File.separator;
    }

    public static String getDirectory() {
        File file = new File(getRoot() + getDate());
        if (!file.exists()) {
            file.mkdir();
        }
        return file.getPath() + File.separator;
    }

    public static String getDate() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date(System.currentTimeMillis()));
    }

    public static String getByteStringForSecureImage(File file) {
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
        return Base64.encodeToString(getBytesFromBitmap(resizeBitmapImageFn(bitmap, MAX_BITMAP_SIZE)),
                Base64.NO_WRAP);
    }

    public static String getFileName(String path) {
        int count = path.split(File.separator).length;
        String filename = "";
        if (count > 1) {
            filename = path.split(File.separator)[count - 1];
        }
        return filename;
    }

    public static byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        return stream.toByteArray();
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

    public static Bitmap resizeBitmapImageFn(Bitmap bmpSource, int maxResolution) {
        int iWidth = bmpSource.getWidth();      //비트맵이미지의 넓이
        int iHeight = bmpSource.getHeight();     //비트맵이미지의 높이
        int newWidth = iWidth;
        int newHeight = iHeight;
        float rate;

        //이미지의 가로 세로 비율에 맞게 조절
        if (iWidth > iHeight) {
            if (maxResolution < iWidth) {
                rate = maxResolution / (float) iWidth;
                newHeight = (int) (iHeight * rate);
                newWidth = maxResolution;
            }
        } else {
            if (maxResolution < iHeight) {
                rate = maxResolution / (float) iHeight;
                newWidth = (int) (iWidth * rate);
                newHeight = maxResolution;
            }
        }

        return Bitmap.createScaledBitmap(
                bmpSource, newWidth, newHeight, true);
    }
}
