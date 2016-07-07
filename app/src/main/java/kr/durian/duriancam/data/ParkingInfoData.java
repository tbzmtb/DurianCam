package kr.durian.duriancam.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by tbzm on 16. 5. 12.
 */
public class ParkingInfoData implements Parcelable {
    private String startTitle;
    private String endTitle;
    private String startDate;
    private String endDate;
    private String startDay;
    private String endDay;
    private String startTime;
    private String endTime;
    private String totalParkingTime;
    private String totalParkingMinute;
    private String totalParkingSecond;
    private String parkingCurrentFee;
    private String ParkingTotalFee;

    public ParkingInfoData() {

    }

    public ParkingInfoData(Parcel in) {
        readFromParcel(in);
    }

    public String getStartTitle() {
        return startTitle;
    }

    public void setStartTitle(String startTitle) {
        this.startTitle = startTitle;
    }

    public String getEndTitle() {
        return endTitle;
    }

    public void setEndTitle(String endTitle) {
        this.endTitle = endTitle;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndtDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStartDay() {
        return startDay;
    }

    public void setStartDay(String startDay) {
        this.startDay = startDay;
    }

    public String getEndDay() {
        return endDay;
    }

    public void setEndDay(String endDay) {
        this.endDay = endDay;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getTotalParkingTime(){
        return totalParkingTime;
    }

    public void setTotalParkingTime(String totalParkingTime){
        this.totalParkingTime = totalParkingTime;
    }

    public String getTotalParkingMinute(){
        return totalParkingMinute;
    }

    public void setTotalParkingMinute(String totalParkingMinute){
        this.totalParkingMinute = totalParkingMinute;
    }

    public String getTotalParkingSecond(){
        return totalParkingSecond;
    }

    public void setTotalParkingSecond(String totalParkingSecond){
        this.totalParkingSecond = totalParkingSecond;
    }
    public String getParkingCurrentFee(){
        return parkingCurrentFee;
    }

    public void setParkingCurrentFee(String parkingCurrentFee){
        this.parkingCurrentFee = parkingCurrentFee;
    }

    public String getParkingTotalFee(){
        return ParkingTotalFee;
    }

    public void setParkingTotalFee(String ParkingTotalFee){
        this.ParkingTotalFee = ParkingTotalFee;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(startTitle);
        dest.writeString(endTitle);
        dest.writeString(startDate);
        dest.writeString(endDate);
        dest.writeString(startDay);
        dest.writeString(endDay);
        dest.writeString(startTime);
        dest.writeString(endTime);
        dest.writeString(totalParkingTime);
        dest.writeString(totalParkingMinute);
        dest.writeString(totalParkingSecond);
        dest.writeString(parkingCurrentFee);
        dest.writeString(ParkingTotalFee);
    }

    public void readFromParcel(Parcel in) {
        startTitle = in.readString();
        endTitle = in.readString();
        startDate = in.readString();
        endDate = in.readString();
        startDay = in.readString();
        endDay = in.readString();
        startTime = in.readString();
        endTime = in.readString();
        totalParkingTime = in.readString();
        totalParkingMinute = in.readString();
        parkingCurrentFee = in.readString();
        ParkingTotalFee = in.readString();
        totalParkingSecond = in.readString();
    }

    public static final Creator CREATOR
            = new Creator() {
        public ParkingInfoData createFromParcel(Parcel in) {
            return new ParkingInfoData(in);
        }

        public ParkingInfoData[] newArray(int size) {
            return new ParkingInfoData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}

