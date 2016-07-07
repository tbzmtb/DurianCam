package kr.durian.duriancam.data;

import android.os.Parcel;
import android.os.Parcelable;

public class ParkingServerData implements Parcelable {
    private String sensorId;
    private String parkingState;
    private String parkingDate;
    private String battery;
    private String name;
    private String gwid;
    private ParkingLotInfoData parkingLotInfoData;

    public ParkingServerData() {

    }

    public ParkingServerData(Parcel in) {
        readFromParcel(in);
    }

    public String getSensorId() {
        return sensorId;
    }

    public String getParkingState() {
        return parkingState;
    }

    public String getParkingDate() {
        return parkingDate;
    }

    public String getBattery() {
        return battery;
    }

    public String getLotName() {
        return name;
    }

    public String getGwid() {
        return gwid;
    }

    public ParkingLotInfoData getParkingLotInfoData() {
        return parkingLotInfoData;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public void setParkingState(String parkingState) {
        this.parkingState = parkingState;
    }

    public void setParkingDate(String parkingDate) {
        this.parkingDate = parkingDate;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    public void setLotName(String name) {
        this.name = name;
    }

    public void setGwid(String gwid) {
        this.gwid = gwid;
    }

    public void setLotInfoData(ParkingLotInfoData parkingLotInfoData) {
        this.parkingLotInfoData = parkingLotInfoData;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sensorId);
        dest.writeString(parkingState);
        dest.writeString(parkingDate);
        dest.writeString(battery);
        dest.writeString(name);
        dest.writeString(gwid);
    }

    public void readFromParcel(Parcel in) {
        sensorId = in.readString();
        parkingState = in.readString();
        parkingDate = in.readString();
        battery = in.readString();
        name = in.readString();
        gwid = in.readString();
    }

    public static final Creator CREATOR
            = new Creator() {
        public ParkingServerData createFromParcel(Parcel in) {
            return new ParkingServerData(in);
        }

        public ParkingServerData[] newArray(int size) {
            return new ParkingServerData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
