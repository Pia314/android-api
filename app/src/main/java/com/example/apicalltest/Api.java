package com.example.apicalltest;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import com.example.apicalltest.APIStructures.Message;

public interface Api {

    String BASE_URL = "https://ar-herokuapp.herokuapp.com/";
    @GET("/")
    Call<APIStructures.StringOut> getHello();

    @GET("retrieveMessages/{username}")
    Call<APIStructures.MessageOut> getMessages(@Path("username") String username,
                                               @Query("requester") String requester);

    @POST("sendMessageTo/{username}")
    Call<Message> sendMessage(
            @Path("username") String destUsername,
            @Query("sender") String senderUsername,
            @Query("msg") String message,
            @Query("openableBy") String openableBy);

    @POST("sendMessageTo")
    Call<Message> sendMessageToEveryone(
            @Query("sender") String senderUsername,
            @Query("msg") String message,
            @Query("openableBy") String openableBy);
}