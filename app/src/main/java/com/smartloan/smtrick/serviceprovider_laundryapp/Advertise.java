package com.smartloan.smtrick.serviceprovider_laundryapp;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by Belal on 2/23/2017.
 */
@IgnoreExtraProperties
public class Advertise {


    public String desc;
    public String name;
    public String url;

    // Default constructor required for calls to
    // DataSnapshot.getValue(User.class)
    public Advertise() {
    }

    public Advertise(String desc, String name, String url) {

        this.desc = desc;
        this.name = name;
        this.url = url;
    }


    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}