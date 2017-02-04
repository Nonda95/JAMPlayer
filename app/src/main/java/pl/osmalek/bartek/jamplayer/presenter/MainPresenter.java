package pl.osmalek.bartek.jamplayer.presenter;

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pl.osmalek.bartek.jamplayer.mediaservice.MusicService;
import pl.osmalek.bartek.jamplayer.ui.MainActivity;

public class MainPresenter implements Presenter<MainActivity> {
    private MainActivity mActivity;
    private MediaBrowserCompat mMediaBrowser;
    private boolean registered = false;
    private final MediaControllerCallback mControllerCallback = new MediaControllerCallback();

    private final Handler mHandler = new Handler();
    private final Runnable mUpdateProgressTask = this::updateProgress;

    private final ScheduledExecutorService mExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> mScheduleFuture;
    private List<MediaSessionCompat.QueueItem> mQueue;
    private MediaControllerCompat mController;
    private MediaMetadataCompat mCurrentMedia;
    private PlaybackStateCompat mLastState;
    private boolean mBrowserReady;

    private void scheduleSeekBarUpdate() {
        stopSeekBarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    () -> mHandler.post(mUpdateProgressTask), 100,
                    200, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekBarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    private void updateProgress() {
        if (mActivity != null) {
            long progress = SystemClock.elapsedRealtime() - mLastState.getLastPositionUpdateTime();
            mActivity.updateProgress((int) (mLastState.getPosition() + progress));
        }
    }

    @Override
    public void onViewAttached(MainActivity view) {
        mActivity = view;
        updateActivityMediaMetadata();
        updateActivityPlayingQueue();
        updateActivityPlayingState();
        if (mBrowserReady) {
            mActivity.browserReady();
        }
    }

    private void updateActivityPlayingQueue() {
        if (mQueue != null)
            mActivity.setPlayingQueue(mQueue);
    }

    private void updateActivityMediaMetadata() {
        if (mCurrentMedia != null) {
            int max = (int) mCurrentMedia.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            mActivity.setMaxProgress(max);
            mActivity.setSongTitle(mCurrentMedia.getDescription().getTitle());
            mActivity.updateProgress(0);
        }
    }

    private void updateActivityPlayingState() {
        if (mLastState != null) {
            mActivity.setPlayButton(mLastState.getState() == PlaybackStateCompat.STATE_PLAYING);
            int lastPosition = (int) mLastState.getPosition();
            mActivity.updateProgress(lastPosition);
        }
    }

    @Override
    public void onViewDetached() {
        mActivity = null;
    }

    @Override
    public void onDestroyed() {
        if (registered)
            mController.unregisterCallback(mControllerCallback);
        mMediaBrowser.disconnect();
        mBrowserReady = false;
        mActivity = null;
    }

    @Override
    public void onCreated(Context context) {
        mBrowserReady = false;
        mMediaBrowser = new MediaBrowserCompat(context, new ComponentName(context, MusicService.class),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        try {
                            MediaSessionCompat.Token token =
                                    mMediaBrowser.getSessionToken();
                            // This is what gives us access to everything
                            mController =
                                    new MediaControllerCompat(context, token);
                            if (!registered) {
                                mController.registerCallback(mControllerCallback);
                                registered = true;
                            }
                            if (mActivity != null) {
                                mActivity.browserReady();
                            } else {
                                mBrowserReady = true;
                            }
                        } catch (RemoteException e) {
                            Log.e(MainActivity.class.getSimpleName(),
                                    "Error creating controller", e);
                        }

                    }
                }, null);
        mMediaBrowser.connect();
    }


    public String getRoot() {
        return mMediaBrowser.getRoot();
    }

    public MediaBrowserCompat getMediaBrowser() {
        return mMediaBrowser;
    }

    public MediaControllerCompat getController() {
        return mController;
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state != null) {
                mLastState = state;
                if (mActivity != null) {
                    updateActivityPlayingState();
                }
                if (state.getState() == PlaybackStateCompat.STATE_PLAYING)
                    scheduleSeekBarUpdate();
                else
                    stopSeekBarUpdate();
            }
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            if (queue != null) {
                mQueue = queue;
                if (mActivity != null) {
                    updateActivityPlayingQueue();
                }
            }

        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mCurrentMedia = metadata;
            if (mActivity != null) {
                updateActivityMediaMetadata();
            }
        }

    }

}
