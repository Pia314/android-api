package com.example.apicalltest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface Api {

    String BASE_URL = "https://ar-herokuapp.herokuapp.com/";
    @GET("/")
    Call<Result> getHello();

    @GET("retrieveMessages/{username}")
    Call<Result> getMessages(@Path("username") String username);

    @POST("sendMessageTo/{username}")
    Call<Result> sendMessage(
            @Path("username") String destUsername,
            @Query("sender") String senderUsername,
            @Query("msg") String message);
}