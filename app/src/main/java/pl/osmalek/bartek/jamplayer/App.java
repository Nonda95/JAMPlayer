package pl.osmalek.bartek.jamplayer;

import android.app.Application;
import android.content.ComponentName;
import android.support.v4.media.MediaBrowserCompat;

import io.reactivex.subjects.ReplaySubject;
import pl.osmalek.bartek.jamplayer.mediaservice.MusicService;


public class App extends Application {

    private static App instance;
    private ReplaySubject<Boolean> mBrowserSubject;
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

    public ReplaySubject<Boolean> getBrowserSubject() {
        if(mBrowser == null) {
            createAndConnectBrowser();
        }
        return mBrowserSubject;
    }

    public MediaBrowserCompat getMediaBrowser() {
        return mBrowser;
    }

    public void reconnectBrowser() {
        mBrowserSubject.onNext(false);
        mBrowser.disconnect();
        /*
            Due to reconnect bug it has to be reinitialized
         */
        createAndConnectBrowser();
    }

    private void createAndConnectBrowser() {
        mBrowser = new MediaBrowserCompat(this, new ComponentName(this, MusicService.class), new MediaBrowserCompat.ConnectionCallback() {
            @Override
            public void onConnected() {
                mBrowserSubject.onNext(true);
            }
        }, null);
        mBrowser.connect();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mBrowser.disconnect();
    }

}
