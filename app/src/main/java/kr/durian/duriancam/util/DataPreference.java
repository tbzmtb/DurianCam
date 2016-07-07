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

    public static boolean getEasyLogin() {
        return PREF != null ? PREF.getBoolean(Config.PREF_EASY_LOGIN_KEY, true) : true;
    }

    public static int getMode() {
        return PREF != null ? PREF.getInt(Config.PREF_MODE_KEY, Config.MODE_NONE) : Config.MODE_NONE;
    }

}
