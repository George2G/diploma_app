package g2g.diploma.gps_realtime.UserUsage;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {

    public  String fName, email, phone,user_id, isFullAccess;

    public  User(){

    }


    public User(String fName, String email, String phone, String user_id, String isFullAccess) {
        this.fName = fName;
        this.email = email;
        this.phone = phone;
        this.user_id = user_id;
        this.isFullAccess = isFullAccess;
    }

    protected User(Parcel in) {
        fName = in.readString();
        email = in.readString();
        phone = in.readString();
        user_id = in.readString();
        isFullAccess = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getfName() {
        return fName;
    }

    public void setfName(String fName) {
        this.fName = fName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getIsFullAccess() {
        return isFullAccess;
    }

    public void setIsFullAccess(String isFullAccess) {
        this.isFullAccess = isFullAccess;
    }

    @Override
    public String toString() {
        return "User{" +
                "fName='" + fName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", user_id='" + user_id + '\'' +
                ", isAdmin='" + isFullAccess + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(fName);
        parcel.writeString(email);
        parcel.writeString(phone);
        parcel.writeString(user_id);
    }
}
