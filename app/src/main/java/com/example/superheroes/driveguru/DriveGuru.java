package com.example.superheroes.driveguru;

import android.app.Application;
import android.content.Context;

import io.relayr.RelayrSdk;

/**
 * Created by Aayush, Cyril, Genevieve on 3/14/15.
 */
public class DriveGuru extends Application
{

    @Override
    public void onCreate() {
        super.onCreate();
        RelayrSdkInitializer.initSdk(this);
    }


}

 abstract class RelayrSdkInitializer {

    static void initSdk(Context context) {
        new RelayrSdk.Builder(context).inMockMode(false).build();
    }

    public static boolean isDebug() {
        return false;
    }
}