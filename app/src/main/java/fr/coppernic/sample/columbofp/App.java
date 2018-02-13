package fr.coppernic.sample.columbofp;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

import timber.log.Timber;

/**
 * Created by michael on 26/01/18.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
        if (BuildConfig.DEBUG) {
            LeakCanary.install(this);
        }
    }
}
