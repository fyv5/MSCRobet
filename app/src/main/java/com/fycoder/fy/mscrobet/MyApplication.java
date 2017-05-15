package com.fycoder.fy.mscrobet;

import android.app.Application;

import com.iflytek.cloud.SpeechUtility;

/**
 * Created by fy on 2016/11/21.
 */
public class MyApplication extends Application {

    String appid = "583041bc";

    @Override
    public void onCreate() {
        SpeechUtility.createUtility(MyApplication.this, "appid=" + appid);
        super.onCreate();
    }

}
