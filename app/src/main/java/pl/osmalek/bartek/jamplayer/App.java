package pl.osmalek.bartek.jamplayer;

import android.app.Application;
import android.content.ComponentName;
import android.support.v4.media.MediaBrowserCompat;

import io.reactivex.subjects.ReplaySubject;
import pl.osmalek.bartek.jamplayer.mediaservice.MusicService;


public class App extends Application {

    private static App instance;
    private ReplaySubject<MediaBrowserCompat> mBrowserSubject;
    private MediaBrowserCompat mBrowser;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mBrowserSubject = ReplaySubject.createWithSize(1);
    }

    public static App get() {
        return instance;
    }

    public ReplaySubject<MediaBrowserCompat> getBrowserSubject() {
        return mBrowserSubject;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mBrowser.disconnect();
    }

    public void permissionGranted() {
        mBrowser = new MediaBrowserCompat(this, new ComponentName(this, MusicService.class), new MediaBrowserCompat.ConnectionCallback() {
            @Override
            public void onConnected() {
                mBrowserSubject.onNext(mBrowser);
                mBrowserSubject.onComplete();
            }
        }, null);
        mBrowser.connect();
    }
}
