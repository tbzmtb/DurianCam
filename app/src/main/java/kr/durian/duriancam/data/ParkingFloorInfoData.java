package kr.durian.duriancam.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by tbzm on 16. 4. 25.
 */
public class ParkingFloorInfoData implements Parcelable {
    private String parkingName;
    private String maxPositionX;
    private String maxPositionY;
    private String floorTotalCount;
    private String floorName;
    private ArrayList<ParkingLotInfoData> parkingLotInfoDataList;

    public ParkingFloorInfoData() {

    }

    public ParkingFloorInfoData(Parcel in) {
        readFromParcel(in);
    }


    public String getParkingName() {
        return parkingName;
    }

    public void setParkingName(String parkingName) {
        this.parkingName = parkingName;
    }

    public String getMaxPositionX() {
        return maxPositionX;
    }

    public void setMaxPositionX(String maxPositionX) {
        this.maxPositionX = maxPositionX;
    }

    public String getMaxPositionY() {
        return maxPositionY;
    }

    public void setMaxPositionY(String maxPositionY) {
        this.maxPositionY = maxPositionY;
    }

    public String getFloorTotalCount() {
        return floorTotalCount;
    }

    public void setFloorTotalCount(String floorTotalCount) {
        this.floorTotalCount = floorTotalCount;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    public void setLotDataObject(ArrayList<ParkingLotInfoData> parkingLotInfoDataList) {
        this.parkingLotInfoDataList = parkingLotInfoDataList;
    }

    public ArrayList<ParkingLotInfoData> getLotDataObject() {
        return parkingLotInfoDataList;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(parkingName);
        dest.writeString(maxPositionX);
        dest.writeString(maxPositionY);
        dest.writeString(floorTotalCount);
        dest.writeString(floorName);
    }

    public void readFromParcel(Parcel in) {
        parkingName = in.readString();
        maxPositionX = in.readString();
        maxPositionY = in.readString();
        floorTotalCount = in.readString();
        floorName = in.readString();
    }

    public static final Creator CREATOR
            = new Creator() {
        public ParkingFloorInfoData createFromParcel(Parcel in) {
            return new ParkingFloorInfoData(in);
        }

        public ParkingFloorInfoData[] newArray(int size) {
            return new ParkingFloorInfoData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
