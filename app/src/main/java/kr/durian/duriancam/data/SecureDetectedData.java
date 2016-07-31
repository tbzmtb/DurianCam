package kr.durian.duriancam.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by tbzm on 15. 10. 19.
 */
public class SecureDetectedData implements Parcelable {
    private String rtcid;
    private String filePath;
    private long time;
    private int isSelected = 0;

    public SecureDetectedData(String rtcid, String filePath, long time, int isSelected) {
        this.rtcid = rtcid;
        this.filePath = filePath;
        this.time = time;
        this.isSelected = isSelected;
    }

    public String getRtcid() {
        return rtcid;
    }

    public String getImagePath() {
        return filePath;
    }

    public long getTime() {
        return time;
    }

    public int getSelected() {
        return isSelected;
    }

    public void setSelected(int data) {
        isSelected = data;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(rtcid);
        dest.writeString(filePath);
        dest.writeLong(time);
        dest.writeInt(isSelected);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SecureDetectedData> CREATOR = new Creator<SecureDetectedData>() {
        @Override
        public SecureDetectedData createFromParcel(Parcel source) {
            String rtcid = source.readString();
            String filePath = source.readString();
            long time = source.readLong();
            int isSelected = source.readInt();
            return new SecureDetectedData(rtcid, filePath, time, isSelected);
        }

        @Override
        public SecureDetectedData[] newArray(int size) {
            return new SecureDetectedData[size];
        }
    };
}
