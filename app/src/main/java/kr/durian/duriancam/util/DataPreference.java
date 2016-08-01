package kr.durian.duriancam.util;

import android.content.SharedPreferences;

public class DataPreference {

    public static SharedPreferences PREF = null;
    public final static String TAG = "DataPreference";

    public static void setLoginName(String value) {
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putString(Config.PREF_LOGIN_NAME_KEY, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setInAppPayment(boolean isPay){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putBoolean(Config.PREF_IN_APP_PAYMENT_KEY, isPay);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setPushToken(String token){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putString(Config.PREF_PUSH_TOKEN_KEY, token);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setPushEnable(boolean enable){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putBoolean(Config.PREF_PUSH_ENABLE_KEY, enable);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setVideoRecordingEnable(String value){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putString(Config.PREF_VIDEO_RECORDING_ENABLE_KEY, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setSecureDisplayEnable(String value){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putString(Config.PREF_DISPLAY_HIDE_KEY, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setDetectSensitivity(String value){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putString(Config.PREF_DETECT_SENSITIVITY_KEY, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setSecuringMode(boolean value){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putBoolean(Config.PREF_IS_SECURING_MODE, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setLoginEmail(String value) {
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putString(Config.PREF_LOGIN_EMAIL_KEY, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setLoginNumber(String value){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putString(Config.PREF_LOGIN_NUMBER_KEY, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setLoginToken(String value){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putString(Config.PREF_LOGIN_TOKEN_KEY, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setRtcid(String value){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putString(Config.PREF_RTCID_KEY, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setPeerRtcid(String value){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putString(Config.PREF_PEER_RTCID_KEY, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setEasyLogin(boolean check){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putBoolean(Config.PREF_EASY_LOGIN_KEY, check);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setMode(int mode){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putInt(Config.PREF_MODE_KEY, mode);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setViewerWillConnectMode(int mode){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putInt(Config.PREF_VIEWER_WILL_CONNECT_MODE_KEY, mode);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setOfferData(String value){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putString(Config.PREF_OFFER_SEND_DATA, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setOfferAckData(String value){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putString(Config.PREF_OFFER_SEND_ACK_DATA, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static void setScreenOnOff(boolean value){
        if (PREF != null) {
            SharedPreferences.Editor editor = PREF.edit();
            editor.putBoolean(Config.PREF_SCREEN_ON_OFF_KEY, value);
            editor.commit();
        } else {
            Logger.d(TAG, "PREF == " + PREF);
        }
    }

    public static String getLoginName() {
        return PREF != null ? PREF.getString(Config.PREF_LOGIN_NAME_KEY, null) : null;
    }
    public static String getLoginEmail() {
        return PREF != null ? PREF.getString(Config.PREF_LOGIN_EMAIL_KEY, null) : null;
    }

    public static String getLoginNumber() {
        return PREF != null ? PREF.getString(Config.PREF_LOGIN_NUMBER_KEY, null) : null;
    }

    public static String getLoginToken() {
        return PREF != null ? PREF.getString(Config.PREF_LOGIN_TOKEN_KEY, null) : null;
    }

    public static String getRtcid(){
        return PREF != null ? PREF.getString(Config.PREF_RTCID_KEY, null) : null;
    }

    public static String getPeerRtcid(){
        return PREF != null ? PREF.getString(Config.PREF_PEER_RTCID_KEY, null) : null;
    }

    public static boolean getEasyLogin() {
        return PREF != null ? PREF.getBoolean(Config.PREF_EASY_LOGIN_KEY, false) : false;
    }

    public static boolean getKeepSceenOn(){
        return PREF != null ? PREF.getBoolean(Config.PREF_SCREEN_ON_OFF_KEY, true) : true;
    }

    public static int getMode() {
        return PREF != null ? PREF.getInt(Config.PREF_MODE_KEY, Config.MODE_VIEWER) : Config.MODE_VIEWER;
    }

    public static int getViewerWillConnectMode() {
        return PREF != null ? PREF.getInt(Config.PREF_VIEWER_WILL_CONNECT_MODE_KEY, Config.MODE_BABY_TALK) : Config.MODE_BABY_TALK;
    }

    public static String getOfferSendData(){
        return PREF != null ? PREF.getString(Config.PREF_OFFER_SEND_DATA, null) : null;
    }

    public static String getOfferSendAckData(){
        return PREF != null ? PREF.getString(Config.PREF_OFFER_SEND_ACK_DATA, null) : null;
    }

    public static boolean getSecuringMode(){
        return PREF != null ? PREF.getBoolean(Config.PREF_IS_SECURING_MODE, false) : false;
    }

    public static String getVideoRecordingEnable(){
        return PREF != null ? PREF.getString(Config.PREF_VIDEO_RECORDING_ENABLE_KEY, Config.VIDEO_RECORDING_DEFAULT_VALUE) : Config.VIDEO_RECORDING_DEFAULT_VALUE;
    }

    public static String getDetectSensitivity(){
        return PREF != null ? PREF.getString(Config.PREF_DETECT_SENSITIVITY_KEY, Config.VIDEO_DETECT_SENSITIVITY_DEFAULT_VALUE) : Config.VIDEO_DETECT_SENSITIVITY_DEFAULT_VALUE;

    }

    public static String getSecureDisplayEnable(){
        return PREF != null ? PREF.getString(Config.PREF_DISPLAY_HIDE_KEY, Config.DISPLAY_HIDE_DEFAULT_VALUE) : Config.DISPLAY_HIDE_DEFAULT_VALUE;

    }

    public static boolean getPushEnable(){
        return PREF != null ? PREF.getBoolean(Config.PREF_PUSH_ENABLE_KEY, true) : true;

    }

    public static String getPushToken(){
        return PREF != null ? PREF.getString(Config.PREF_PUSH_TOKEN_KEY, null) : null;
    }

    public static boolean getInAppPayment(){
        return PREF != null ? PREF.getBoolean(Config.PREF_IN_APP_PAYMENT_KEY, false) : false;
    }
}
