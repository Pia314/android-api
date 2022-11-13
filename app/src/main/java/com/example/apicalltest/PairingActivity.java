package com.example.apicalltest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SharedMemory;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PairingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);
    }

    public void selectUsername(View view){
        EditText editText = (EditText) findViewById(R.id.textView3);
        if (editText.getText().toString().equals("")){
            Toast.makeText(getApplicationContext(), "INVALID USERNAME", Toast.LENGTH_LONG).show();
            return;
        }
        String new_username = editText.getText().toString();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("username", new_username);
        editor.apply();
    }
}