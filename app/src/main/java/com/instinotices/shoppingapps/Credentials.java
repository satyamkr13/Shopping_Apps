package com.instinotices.shoppingapps;

import android.os.Parcel;
import android.os.Parcelable;

public class Credentials implements Parcelable {
    public static final Creator<Credentials> CREATOR = new Creator<Credentials>() {
        @Override
        public Credentials createFromParcel(Parcel in) {
            return new Credentials(in);
        }

        @Override
        public Credentials[] newArray(int size) {
            return new Credentials[size];
        }
    };
    public String awsAccessKey, secretKey, FAId, FToken, AAssociateTag;

    public Credentials() {

    }

    protected Credentials(Parcel in) {
        awsAccessKey = in.readString();
        secretKey = in.readString();
        FAId = in.readString();
        FToken = in.readString();
        AAssociateTag = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(awsAccessKey);
        parcel.writeString(secretKey);
        parcel.writeString(FAId);
        parcel.writeString(FToken);
        parcel.writeString(AAssociateTag);
    }
}
