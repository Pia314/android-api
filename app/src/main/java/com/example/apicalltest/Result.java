package com.example.apicalltest;

import com.google.gson.annotations.SerializedName;

public class Result {

    @SerializedName("message")
    private String name;


    public Result(String name) {
        this.name = name;
    }

    public String get() {
        return name;
    }
}