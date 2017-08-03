package com.neurareactnative.controller;

import android.content.Context;
import android.preference.PreferenceManager;

import com.neura.standalonesdk.service.NeuraApiClient;
import com.neura.standalonesdk.util.Builder;

public class NeuraManager {

    private static NeuraManager sInstance;
    private NeuraApiClient mNeuraApiClient;
    //Saving authentication time in order to see how much time it took Neura to detect home.
    private static String KEY_AUTHENTICATION_TIME = "KEY_AUTHENTICATION_TIME";

    public static NeuraManager getInstance() {
        if (sInstance == null)
            sInstance = new NeuraManager();
        return sInstance;
    }

    public NeuraApiClient getClient() {
        return mNeuraApiClient;
    }

    public void initNeuraConnection(Context applicationContext, String appUid, String appSecret) {
        Builder builder = new Builder(applicationContext);
        mNeuraApiClient = builder.build();
        mNeuraApiClient.setAppUid(appUid);
        mNeuraApiClient.setAppSecret(appSecret);
        mNeuraApiClient.connect();
    }

    public void setAuthenticationTime(Context context, long timestamp) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().
                putLong(KEY_AUTHENTICATION_TIME, timestamp).commit();
    }

    public long getAuthenticationTime(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(KEY_AUTHENTICATION_TIME, 0);
    }
}
