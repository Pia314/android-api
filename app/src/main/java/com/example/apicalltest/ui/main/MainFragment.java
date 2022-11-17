package com.example.apicalltest.ui.main;

import androidx.lifecycle.ViewModelProvider;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.example.apicalltest.R;

import org.w3c.dom.Text;

import java.util.Objects;

public class MainFragment extends Fragment {

    private MainViewModel mViewModel;

    public static MainFragment newInstance() {
        return new MainFragment();
    }
    String username;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);

    }


    @Override
    public void onResume()
    {
        super.onResume();
        refreshText(getView());
    }

    public void refreshText(View view){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        username = preferences.getString("username", "DEFAULT");
        TextView textView = view.findViewById(R.id.message4);
        if (Objects.equals(username, "DEFAULT")){
            textView.setText("PAIR YOUR PROFILE IN THE PROFILE VIEW");
        }else{
            textView.setText("You are " + username);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        refreshText(view);
        return view;
    }

}