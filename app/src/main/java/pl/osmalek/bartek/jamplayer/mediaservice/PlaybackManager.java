package pl.osmalek.bartek.jamplayer.mediaservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.io.IOException;


class PlaybackManager implements Playback, AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnPreparedListener {
    private static final float VOLUME_DUCK = 0.2f;
    private static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED = 2;

    private final MusicService mService;
    private int mState;
    private boolean mPlayOnFocusGain;
    private volatile MediaSessionCompat.QueueItem mCurrentMedia;
    private volatile int mCurrentPosition;

    private MediaPlayer mMediaPlayer;

    private Callback mCallback;
    private final AudioManager mAudioManager;
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private volatile boolean mAudioNoisyReceiverRegistered;

    private IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (isPlaying()) {
                    Intent i = new Intent(context, MusicService.class);
                    i.setAction(MusicService.ACTION_CMD);
                    i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
                    mService.startService(i);
                }
            }
        }
    };
    private PlaybackStateCompat.Builder mStateBuilder;

    PlaybackManager(MusicService mService) {
        this.mService = mService;
        this.mAudioManager = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
    }

    PlaybackManager(MusicService mService, MediaSessionCompat.QueueItem mCurrentMedia, int mCurrentPosition) {
        this.mService = mService;
        this.mCurrentMedia = mCurrentMedia;
        this.mCurrentPosition = mCurrentPosition;
        this.mAudioManager = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (mCallback != null)
            mCallback.onCompletion();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            mAudioFocus = AUDIO_FOCUSED;
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;
            mPlayOnFocusGain = mState == PlaybackStateCompat.STATE_PLAYING && !canDuck;
        }
        configMediaPlayerState();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        configMediaPlayerState();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        mCurrentPosition = mediaPlayer.getCurrentPosition();
        if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            mMediaPlayer.start();
            mState = PlaybackStateCompat.STATE_PLAYING;
        }
        updatePlaybackState();
    }

    @Override
    public boolean isPlaying() {
        return mPlayOnFocusGain || (mMediaPlayer != null && mMediaPlayer.isPlaying());
    }

    @Override
    public MediaSessionCompat.QueueItem getCurrentMedia() {
        return mCurrentMedia;
    }

    @Override
    public String getCurrentMediaId() {
        return mCurrentMedia.getDescription().getMediaId();
    }

    @Override
    public int getCurrentStreamPosition() {
        return mMediaPlayer != null ? mMediaPlayer.getCurrentPosition() : mCurrentPosition;
    }

    @Override
    public void play(MediaSessionCompat.QueueItem queueItem, boolean fromStart) {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        String mediaId = queueItem.getDescription().getMediaId();
        boolean mediaHasChanged = mCurrentMedia == null || (mCurrentMedia != null && !getCurrentMediaId().equals(mediaId));
        if (mediaHasChanged || fromStart) {
            mCurrentPosition = 0;
            mCurrentMedia = queueItem;
        }
        if (mState == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && mMediaPlayer != null) {
            configMediaPlayerState();
        } else {
            mState = PlaybackStateCompat.STATE_STOPPED;
            relaxResources(false);
            Uri source = queueItem.getDescription().getMediaUri();
            try {
                createMediaPlayerIfNeeded();

                mState = PlaybackStateCompat.STATE_BUFFERING;

                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(mService, source);
                mMediaPlayer.prepare();
                onPrepared(mMediaPlayer);


            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void relaxResources(boolean releasePlayer) {
        mService.stopForeground(true);
        if (releasePlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void createMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setWakeMode(mService, PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setOnCompletionListener(this);
//            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
        } else {
            mMediaPlayer.reset();
        }
    }

    private void configMediaPlayerState() {
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                pause();
            }
        } else {
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK);
            } else {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL);
                }
            }
            if (mPlayOnFocusGain) {
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                    if (mCurrentPosition == mMediaPlayer.getCurrentPosition()) {
                        mMediaPlayer.start();
                        mState = PlaybackStateCompat.STATE_PLAYING;
                    } else {
                        mMediaPlayer.seekTo(mCurrentPosition);
                        mState = PlaybackStateCompat.STATE_BUFFERING;
                    }
                }
                mPlayOnFocusGain = false;
            }
        }
        updatePlaybackState();
    }

    private void tryToGetAudioFocus() {
        if (mAudioFocus != AUDIO_FOCUSED) {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUSED;
            }
        }
    }

    private void giveUpAudioFocus() {
        if (mAudioFocus == AUDIO_FOCUSED) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            }
        }
    }

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mService.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mService.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    @Override
    public void pause() {
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
            }
            relaxResources(false);
            giveUpAudioFocus();
        }
        mState = PlaybackStateCompat.STATE_PAUSED;
        updatePlaybackState();
        unregisterAudioNoisyReceiver();
    }

    @Override
    public void stop(boolean callback) {
        mState = PlaybackStateCompat.STATE_STOPPED;
        if (callback)
            updatePlaybackState();
        mCurrentPosition = getCurrentStreamPosition();
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        relaxResources(true);
    }

    @Override
    public void setState(int state) {
        mState = state;
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public void seekTo(int position) {
        if (mMediaPlayer == null) {
            mCurrentPosition = position;
        } else {
            if (mMediaPlayer.isPlaying()) {
                mState = PlaybackStateCompat.STATE_BUFFERING;
            }
            mMediaPlayer.seekTo(position);
            updatePlaybackState();
        }
    }

    @Override
    public void setCallback(Callback callback) {
        mCallback = callback;
        updatePlaybackState();
    }

    private void updatePlaybackState() {
        if (mCallback == null) {
            return;
        }
        if (mStateBuilder == null) {
            mStateBuilder = new PlaybackStateCompat.Builder().setActions(
                    PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                            PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE |
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                            PlaybackStateCompat.ACTION_STOP);
        }
        mStateBuilder.setState(mState, getCurrentStreamPosition(), 1.0f, SystemClock.elapsedRealtime());
        mStateBuilder.setActiveQueueItemId(mCurrentMedia != null ? mCurrentMedia.getQueueId() : 0);
        mCallback.onPlaybackStatusChanged(mStateBuilder.build());

    }
}
