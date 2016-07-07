package kr.durian.duriancam.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by tbzm on 16. 4. 25.
 */
public class ParkingLotInfoData implements Parcelable {
    String positionX;
    String positionY;
    String parkingState;
    String floorName;
    String lotName;
    String lotData;
    String sensorId;
    String parkingDate;
    String battery;
    String gwid;
    String serverParkingName;
    String address;
    String code;

    public ParkingLotInfoData() {

    }

    public ParkingLotInfoData(Parcel in) {
        readFromParcel(in);
    }

    public String getPositionX() {
        return positionX;
    }

    public void setPositionX(String positionX) {
        this.positionX = positionX;
    }

    public String getPositionY() {
        return positionY;
    }

    public void setPositionY(String positionY) {
        this.positionY = positionY;
    }

    public String getParkingState() {
        return parkingState;
    }

    public void setParkingState(String parkingState) {
        this.parkingState = parkingState;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    public String getLotName() {
        return lotName;
    }

    public void setLotName(String lotName) {
        this.lotName = lotName;
    }

    public String getLotData() {
        return lotData;
    }

    public void setLotData(String lotData) {
        this.lotData = lotData;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getParkingDate() {
        return parkingDate;
    }

    public void setParkingDate(String parkingDate) {
        this.parkingDate = parkingDate;
    }

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    public String getGwid() {
        return gwid;
    }

    public void setGwid(String gwid) {
        this.gwid = gwid;
    }

    public String getServerParkingName() {
        return serverParkingName;
    }

    public void setServerParkingName(String serverParkingName) {
        this.serverParkingName = serverParkingName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(positionX);
        dest.writeString(positionY);
        dest.writeString(parkingState);
        dest.writeString(floorName);
        dest.writeString(lotName);
        dest.writeString(lotData);
        dest.writeString(sensorId);
        dest.writeString(parkingDate);
        dest.writeString(battery);
        dest.writeString(gwid);
        dest.writeString(serverParkingName);
        dest.writeString(address);
        dest.writeString(code);
    }

    public void readFromParcel(Parcel in) {
        positionX = in.readString();
        positionY = in.readString();
        parkingState = in.readString();
        floorName = in.readString();
        lotName = in.readString();
        lotData = in.readString();
        sensorId = in.readString();
        parkingDate = in.readString();
        battery = in.readString();
        gwid = in.readString();
        serverParkingName = in.readString();
        address = in.readString();
        code = in.readString();
    }

    public static final Creator CREATOR
            = new Creator() {
        public ParkingLotInfoData createFromParcel(Parcel in) {
            return new ParkingLotInfoData(in);
        }

        public ParkingLotInfoData[] newArray(int size) {
            return new ParkingLotInfoData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

}
