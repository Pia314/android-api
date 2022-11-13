package com.example.apicalltest;

import android.app.Application;

public class ARApplication extends Application {
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String u) {
        this.username = u;
    }
}
