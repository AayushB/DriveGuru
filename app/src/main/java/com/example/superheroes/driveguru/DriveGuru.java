package com.example.superheroes.driveguru;

import android.app.Application;

import io.relayr.RelayrSdk;

/**
 * Created by Aayush, Cyril, Genevieve on 3/14/15.
 */
public class DriveGuru extends Application
{

    @Override
    public void onCreate() {
        super.onCreate();
        RelayrSdk.init(this);
    }

}

