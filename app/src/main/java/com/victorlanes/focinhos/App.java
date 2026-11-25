package com.victorlanes.focinhos;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Trava.instalar(this);
    }
}
