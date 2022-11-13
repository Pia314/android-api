package com.example.apicalltest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.apicalltest.ui.main.MainFragment;

import org.opencv.android.OpenCVLoader;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow();
        }

        if (OpenCVLoader.initDebug()) {
            Log.d("myTag", "OpenCV loaded");
        }
        Log.d("myTag", ":)");

    }

    String USERNAME = "green";

    public void checkUsername(String username){
        // TODO check if username entered is accepted by API (ie is a color)
    }

    public void postMessageFromMeTo(String myUsername, String destUsername, String message){
        Call<Result> call = RetrofitClient.getInstance().getMyApi().sendMessage(destUsername, myUsername, message);
        Log.d("d", call.request().toString());
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(Call<Result> call, Response<Result> response) {
                Result result = response.body();
                Log.d("d", response.toString());
                try {
                    myViewer("Message uploaded.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                myViewer("ERROR on upload.");
            }
        });

    }

    public void retrieveMessageForMe(String username){
        Call<Result> call = RetrofitClient.getInstance().getMyApi().getMessages(username);
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(Call<Result> call, Response<Result> response) {
                Result result = response.body();
                try {
                    String msg = result.get();
                    if (msg == "No message found") {
                        //TODO add a sucess int in the response to check instead of the message
                    }else {
                        myViewer(result.get());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                myViewer("ERROR");
            }
        });
    }

    public void retrieveTestCall(){
        Call<Result> call = RetrofitClient.getInstance().getMyApi().getHello();
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(Call<Result> call, Response<Result> response) {
                Result result = response.body();
                try {
                    myViewer(result.get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                myViewer("ERROR");
            }

        });
    }

    public void myViewer(String str){
        TextView textView = (TextView) findViewById(R.id.message2);
        textView.setText(str);
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
    }

    /** Called when the user touches the button */
    public void callApi(View view) {
        TextView textView = (TextView) findViewById(R.id.message2);
        textView.setText("cool");
        retrieveTestCall();
    }

    public void testSend(View view) {
        postMessageFromMeTo("green", "blue", "hi");
    }

    public void testRetrieve(View view){
        retrieveMessageForMe("blue");
    }
}

