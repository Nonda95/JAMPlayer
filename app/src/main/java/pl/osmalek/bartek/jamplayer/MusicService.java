package pl.osmalek.bartek.jamplayer;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.ArrayList;
import java.util.List;

import pl.osmalek.bartek.jamplayer.UI.MainActivity;
import pl.osmalek.bartek.jamplayer.model.Folder;
import pl.osmalek.bartek.jamplayer.model.MusicStore;

public class MusicService extends MediaBrowserServiceCompat implements Playback.Callback {
    public static final String ACTION_CMD = "pl.osmalek.bartek.jamplayer.musicservice.ACTION_CMD";
    public static final String CMD_NAME = "CMD_NAME";
    public static final String CMD_PAUSE = "CMD_PAUSE";

    private MediaSessionCompat mSession;
    private List<MediaSessionCompat.QueueItem> mPlayingQueue;
    private int mCurrentIndexOnQueue;
    private MediaNotificationManager mNotificationManager;
    private boolean mServiceStarted;
    private Playback mPlayback;
    private MusicStore mMusicProvider;

    @Override
    public void onCreate() {
        super.onCreate();

        mMusicProvider = MusicStore.getInstance();
        if(!mMusicProvider.isReady()) {
            mMusicProvider.prepareStore(this);
        }

        mPlayingQueue = new ArrayList<>();

        mSession = new MediaSessionCompat(this, getPackageName());
        mSession.setCallback(new MediaSessionCallback());
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        Intent mediaButtonIntent = new Intent(this, MediaButtonReceiver.class);
        PendingIntent buttonPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setMediaButtonReceiver(buttonPendingIntent);
        setSessionToken(mSession.getSessionToken());

        mPlayback = new PlaybackManager(this);
        mPlayback.setState(PlaybackStateCompat.STATE_NONE);
        mPlayback.setCallback(this);

        Context context = getApplicationContext();
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setSessionActivity(pendingIntent);

        mNotificationManager = new MediaNotificationManager(this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            String action = intent.getAction();
            String command = intent.getStringExtra(CMD_NAME);
            if(ACTION_CMD.equals(action) && CMD_PAUSE.equals(command) && mPlayback != null && mPlayback.isPlaying())
            {
                handlePauseRequest();
            } else {
                MediaButtonReceiver.handleIntent(mSession, intent);
            }
        }
        return START_STICKY;
    }

    private void handlePauseRequest() {
        mPlayback.pause();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {

        return new BrowserRoot(mMusicProvider.getMainFolder().getFilename(), null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Folder folder = (Folder) mMusicProvider.getFileFromUri(parentId);
        result.sendResult(folder.getFilesAsMediaItem());
    }

    @Override
    public void onDestroy() {
        handleStopRequest();
        mSession.release();
        super.onDestroy();
    }

    @Override
    public void onCompletion() {
        if(mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
            mCurrentIndexOnQueue++;
            if(mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            handlePlayRequest();
        } else {
            handleStopRequest();
        }
    }

    private void handleStopRequest() {
        mPlayback.stop(true);
        stopSelf();
        mServiceStarted = false;

    }

    private void handlePlayRequest() {
        if(!mServiceStarted) {
            startService(new Intent(getApplicationContext(), MusicService.class));
            mServiceStarted = true;
        }

        if(!mSession.isActive()) {
            mSession.setActive(true);
        }

        updateMetadata();
        mPlayback.play(mPlayingQueue.get(mCurrentIndexOnQueue));
    }

    private void updateMetadata(){
        MediaSessionCompat.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
        MediaMetadataCompat metadata = mMusicProvider.getMediaItem(item.getDescription().getMediaId());
        mSession.setMetadata(metadata);
    }

    @Override
    public void onPlaybackStatusChanged(PlaybackStateCompat state) {
        mSession.setPlaybackState(state);
        mNotificationManager.update(state);
    }

    public MediaSessionCompat getSession() {
        return mSession;
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            if(mPlayingQueue == null || mPlayingQueue.isEmpty()) {
                mPlayingQueue = mMusicProvider.getAllSongsQueue();
                mSession.setQueue(mPlayingQueue);
                mSession.setQueueTitle("Random");

                mCurrentIndexOnQueue = 0;
            }

            if(mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                handlePlayRequest();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            mPlayingQueue = mMusicProvider.getBasicQueueForItem(mediaId);
            mSession.setQueue(mPlayingQueue);
            mSession.setQueueTitle("Playing Now");
            if(mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                mCurrentIndexOnQueue = 0;
                handlePlayRequest();
            }
        }

        @Override
        public void onSkipToQueueItem(long id) {
            if(mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                mCurrentIndexOnQueue = (int) id;
                handlePlayRequest();
            }
        }

        @Override
        public void onSkipToNext() {
            if(mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                mCurrentIndexOnQueue = (mCurrentIndexOnQueue + 1) % mPlayingQueue.size();
                handlePlayRequest();
            }
        }

        @Override
        public void onSkipToPrevious() {
            if(mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                mCurrentIndexOnQueue = mCurrentIndexOnQueue > 0 ? mCurrentIndexOnQueue - 1 : mPlayingQueue.size() - 1;
                handlePlayRequest();
            }
        }

        @Override
        public void onPause() {
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            handleStopRequest();
        }
    }
}
