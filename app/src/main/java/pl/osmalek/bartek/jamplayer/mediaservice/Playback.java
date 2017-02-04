package pl.osmalek.bartek.jamplayer.mediaservice;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

interface Playback {
    boolean isPlaying();

    MediaSessionCompat.QueueItem getCurrentMedia();
    String getCurrentMediaId();
    int getCurrentStreamPosition();
    void play(MediaSessionCompat.QueueItem metadata, boolean fromStart);
    void pause();
    void stop(boolean callback);
    void setState(int state);
    int getState();
    void seekTo(int position);
    void setCallback(Callback callback);
    interface Callback {
        /**
         * On current music completed.
         */
        void onCompletion();
        /**
         * on Playback status changed
         * Implementations can use this callback to update
         * playback state on the media sessions.
         */
        void onPlaybackStatusChanged(PlaybackStateCompat state);

    }
}
