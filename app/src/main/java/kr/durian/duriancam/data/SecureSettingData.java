package kr.durian.duriancam.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by tbzm on 16. 5. 12.
 */
public class SecureSettingData implements Parcelable {
    private String mSecureOnOffValue;


    public SecureSettingData() {

    }

    public SecureSettingData(Parcel in) {
        readFromParcel(in);
    }

    public String getmSecureOnOffValue() {
        return mSecureOnOffValue;
    }

    public void setmSecureOnOffValue(String mSecureOnOffValue) {
        this.mSecureOnOffValue = mSecureOnOffValue;
    }



    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSecureOnOffValue);

    }

    public void readFromParcel(Parcel in) {
        mSecureOnOffValue = in.readString();

    }

    public static final Creator CREATOR
            = new Creator() {
        public SecureSettingData createFromParcel(Parcel in) {
            return new SecureSettingData(in);
        }

        public SecureSettingData[] newArray(int size) {
            return new SecureSettingData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}

