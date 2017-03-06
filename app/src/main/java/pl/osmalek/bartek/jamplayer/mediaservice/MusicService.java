package pl.osmalek.bartek.jamplayer.mediaservice;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import pl.osmalek.bartek.jamplayer.MediaNotificationManager;
import pl.osmalek.bartek.jamplayer.SharedPrefConsts;
import pl.osmalek.bartek.jamplayer.model.BaseFile;
import pl.osmalek.bartek.jamplayer.model.Folder;
import pl.osmalek.bartek.jamplayer.model.MusicFile;
import pl.osmalek.bartek.jamplayer.model.MusicProvider;
import pl.osmalek.bartek.jamplayer.ui.MainActivity;

import static pl.osmalek.bartek.jamplayer.SharedPrefConsts.CURRENT_TRACK;
import static pl.osmalek.bartek.jamplayer.SharedPrefConsts.CURRENT_TRACK_POSITION;
import static pl.osmalek.bartek.jamplayer.SharedPrefConsts.PLAYBACK_SHARED_PREF;
import static pl.osmalek.bartek.jamplayer.SharedPrefConsts.REPEAT_MODE;
import static pl.osmalek.bartek.jamplayer.SharedPrefConsts.SHARED_PREF;

public class MusicService extends MediaBrowserServiceCompat implements Playback.Callback, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String ACTION_CMD = "pl.osmalek.bartek.jamplayer.musicservice.ACTION_CMD";
    public static final String CMD_NAME = "CMD_NAME";
    public static final String CMD_PAUSE = "CMD_PAUSE";
    public static final String CUSTOM_CMD_PLAY = "play";
    public static final String CUSTOM_CMD_ADD_TO_QUEUE = "add_to_queue";
    public static final String CUSTOM_CMD_PLAY_NEXT = "play_next";
    public static final String CUSTOM_CMD_MEDIA_ID = "mediaId";
    public static final String CUSTOM_CMD_SET_MAIN_FOLDER = "set_default";
    public static final String CUSTOM_CMD_RESET_MAIN_FOLDER = "reset_default";
    public static final String ROOT_ID = "rootId";
    public static final int REPEAT_ALL = 0;
    public static final int REPEAT_ONE = 1;
    public static final int NO_REPEAT = 2;
    public static final int SHUFFLE = 3;

    private MediaSessionCompat mSession;
    private List<MediaSessionCompat.QueueItem> mPlayingQueue;
    private int mCurrentIndexOnQueue;
    private MediaNotificationManager mNotificationManager;
    private boolean mServiceStarted;
    private Playback mPlayback;
    private MusicProvider mMusicProvider;
    private int mLastUpdatedIndex;
    private int mRepeatMode;

    @Override
    public void onCreate() {
        super.onCreate();
        mLastUpdatedIndex = -1;

        mNotificationManager = new MediaNotificationManager(this);

        mMusicProvider = new MusicProvider(getApplicationContext());
        mPlayingQueue = new ArrayList<>();
        mMusicProvider.prepare();
        restoreFromSharedPref();
        mSession = new MediaSessionCompat(this, getPackageName());
        mSession.setQueue(mPlayingQueue);
        mSession.setCallback(new MediaSessionCallback());

        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        Intent mediaButtonIntent = new Intent(this, MediaButtonReceiver.class);
        PendingIntent buttonPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setMediaButtonReceiver(buttonPendingIntent);
        setSessionToken(mSession.getSessionToken());

        Context context = getApplicationContext();
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setSessionActivity(pendingIntent);

        mPlayback.setState(PlaybackStateCompat.STATE_NONE);
        mPlayback.setCallback(this);
        final SharedPreferences playbackSharedPreferences = getSharedPreferences(PLAYBACK_SHARED_PREF, MODE_PRIVATE);
        mRepeatMode = playbackSharedPreferences.getInt(REPEAT_MODE, REPEAT_ALL);
        playbackSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        Completable.fromAction(this::updateMetadata)
                .subscribeOn(Schedulers.single())
                .subscribe();
    }

    private void restoreFromSharedPref() {
        Type type = new TypeToken<List<String>>() {
        }.getType();
        SharedPreferences pref = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        String queue = pref.getString(SharedPrefConsts.QUEUE, null);
        mCurrentIndexOnQueue = pref.getInt(CURRENT_TRACK, 0);
        int currentPosition = pref.getInt(CURRENT_TRACK_POSITION, 0);
        Gson gson = new Gson();
        if (queue != null) {
            Object o = gson.fromJson(queue, type);
            if (o != null && o instanceof List) {
                mPlayingQueue = mMusicProvider.getQueueFromIds((List<String>) o);
            }
        }
        if (mCurrentIndexOnQueue < mPlayingQueue.size()) {
            mPlayback = new PlaybackManager(this, mPlayingQueue.get(mCurrentIndexOnQueue), currentPosition);
        } else {
            mPlayback = new PlaybackManager(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            String command = intent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action) && CMD_PAUSE.equals(command) && mPlayback != null && mPlayback.isPlaying()) {
                handlePauseRequest();
            } else {
                MediaButtonReceiver.handleIntent(mSession, intent);
            }
        }
        return START_STICKY;
    }

    private void handlePauseRequest() {
        Completable.fromAction(() -> {
            saveToSharedPref(false);
            mPlayback.pause();
        }).subscribeOn(Schedulers.single()).subscribe();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        final Bundle bundle = new Bundle();
        bundle.putString(ROOT_ID, mMusicProvider.getRootFolder().getMediaId());
        return new BrowserRoot(mMusicProvider.getMainFolder().getMediaId(), bundle);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Folder folder = (Folder) mMusicProvider.getFileFromUri(parentId);
        result.sendResult(folder.getFilesAsMediaItem());
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result, @NonNull Bundle options) {
        super.onLoadChildren(parentId, result, options);
    }

    @Override
    public void onDestroy() {
        getSharedPreferences(PLAYBACK_SHARED_PREF, MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(this);
        handleStopRequest();
        mSession.release();
        super.onDestroy();
    }

    private void saveToSharedPref(boolean withQueue) {
        SharedPreferences.Editor pref = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
        if (withQueue) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<String>>() {
            }.getType();
            String queue = gson.toJson(mMusicProvider.getIdsFromQueue(mPlayingQueue), type);
            pref.putString(SharedPrefConsts.QUEUE, queue);
        }
        pref.putInt(CURRENT_TRACK, mCurrentIndexOnQueue);
        pref.putInt(CURRENT_TRACK_POSITION, mPlayback.getCurrentStreamPosition());
        pref.apply();
    }

    @Override
    public void onCompletion() {
        if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
            if (mRepeatMode == NO_REPEAT && mCurrentIndexOnQueue == mPlayingQueue.size() - 1) {
                handlePauseRequest();
                Completable.fromAction(() -> mPlayback.seekTo(0))
                        .subscribeOn(Schedulers.single())
                        .subscribe();
                return;
            }
            if (mRepeatMode == SHUFFLE) {
                shuffle();
            } else if (mRepeatMode != REPEAT_ONE) {
                mCurrentIndexOnQueue++;
            }
            if (mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            handlePlayRequest(true);
        } else {
            handleStopRequest();
        }
    }

    private void shuffle() {
        Random random = new Random();
        mCurrentIndexOnQueue = (mCurrentIndexOnQueue + random.nextInt(mPlayingQueue.size() - 1) + 1) % mPlayingQueue.size();
    }

    private void handleStopRequest() {
        Completable.fromAction(() -> {
            mPlayback.stop(true);
            stopSelf();
            mServiceStarted = false;
        }).subscribeOn(Schedulers.single()).subscribe();
    }

    private void handlePlayRequest(boolean fromStart) {
        if (!mServiceStarted) {
            startService(new Intent(getApplicationContext(), MusicService.class));
            mServiceStarted = true;
        }

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }
        Completable.fromAction(() -> {
            updateMetadata();
            mPlayback.play(mPlayingQueue.get(mCurrentIndexOnQueue), fromStart);
            saveToSharedPref(false);
        }).subscribeOn(Schedulers.single()).subscribe();
    }

    private void updateMetadata() {
        if (mPlayingQueue != null && !mPlayingQueue.isEmpty() && mCurrentIndexOnQueue != mLastUpdatedIndex) {
            mLastUpdatedIndex = mCurrentIndexOnQueue;
            MediaSessionCompat.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            MediaMetadataCompat metadataCompat = mMusicProvider.getMediaItem(item.getDescription().getMediaId());
            mSession.setMetadata(metadataCompat);
        }
    }

    @Override
    public void onPlaybackStatusChanged(PlaybackStateCompat state) {
        mSession.setPlaybackState(state);
        mNotificationManager.update(state);

    }

    public MediaSessionCompat getSession() {
        return mSession;
    }

    public void setQueue() {
        mSession.setQueue(mPlayingQueue);
        mSession.setQueueTitle("Playing Now");
        mLastUpdatedIndex = -1;
        saveToSharedPref(true);
    }

    public List<MediaSessionCompat.QueueItem> getQueue() {
        return mPlayingQueue;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(REPEAT_MODE)) {
            mRepeatMode = sharedPreferences.getInt(REPEAT_MODE, REPEAT_ALL);
        }
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
                mPlayingQueue = mMusicProvider.getAllSongsQueue();
                setQueue();

                mCurrentIndexOnQueue = 0;
            }

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                handlePlayRequest(false);
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            mPlayingQueue = mMusicProvider.getBasicQueueForItem(mediaId);
            setQueue();
            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                mCurrentIndexOnQueue = mMusicProvider.getIndexForQueue(mPlayingQueue, mediaId);
                handlePlayRequest(true);
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            if (CUSTOM_CMD_PLAY.equals(action) || CUSTOM_CMD_ADD_TO_QUEUE.equals(action) || CUSTOM_CMD_PLAY_NEXT.equals(action)) {
                String mediaId = extras.getString(CUSTOM_CMD_MEDIA_ID);
                BaseFile file = mMusicProvider.getFileFromUri(mediaId);
                List<MusicFile> filesToAdd;
                if (file instanceof Folder) {
                    filesToAdd = ((Folder) file).getAllSongs();
                } else {
                    filesToAdd = new ArrayList<>();
                    filesToAdd.add((MusicFile) file);
                }
                if (CUSTOM_CMD_PLAY.equals(action) || mPlayingQueue == null) {
                    mPlayingQueue = new ArrayList<>();
                    mCurrentIndexOnQueue = 0;
                    mMusicProvider.addToQueue(mPlayingQueue, filesToAdd);
                    setQueue();
                    handlePlayRequest(true);
                } else {
                    if (CUSTOM_CMD_ADD_TO_QUEUE.equals(action)) {
                        mMusicProvider.addToQueue(mPlayingQueue, filesToAdd);
                    } else {
                        mMusicProvider.addNextToQueue(mPlayingQueue, mCurrentIndexOnQueue, filesToAdd);
                    }
                    setQueue();
                }

            } else if (CUSTOM_CMD_SET_MAIN_FOLDER.equals(action) || CUSTOM_CMD_RESET_MAIN_FOLDER.equals(action)) {
                Folder folder = mMusicProvider.getRootFolder();
                if (CUSTOM_CMD_SET_MAIN_FOLDER.equals(action)) {
                    String mediaId = extras.getString(CUSTOM_CMD_MEDIA_ID);
                    folder = ((Folder) mMusicProvider.getFileFromUri(mediaId));
                }
                mMusicProvider.setMainFolder(folder);
            }
        }

        @Override
        public void onSkipToQueueItem(long id) {
            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                mCurrentIndexOnQueue = (int) id;
                handlePlayRequest(true);
            }
        }

        @Override
        public void onSkipToNext() {
            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                if (mRepeatMode == SHUFFLE) {
                    shuffle();
                } else {
                    mCurrentIndexOnQueue = (mCurrentIndexOnQueue + 1) % mPlayingQueue.size();
                }
                handlePlayRequest(true);
            }
        }

        @Override
        public void onSkipToPrevious() {
            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                if (mRepeatMode == SHUFFLE) {
                    shuffle();
                } else {
                    mCurrentIndexOnQueue = mCurrentIndexOnQueue > 0 ? mCurrentIndexOnQueue - 1 : mPlayingQueue.size() - 1;
                }
                handlePlayRequest(true);
            }
        }

        @Override
        public void onSeekTo(long pos) {
            Completable.fromAction(() -> mPlayback.seekTo((int) pos))
                    .subscribeOn(Schedulers.single())
                    .subscribe();
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
