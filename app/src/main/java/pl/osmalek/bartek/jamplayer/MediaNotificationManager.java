package pl.osmalek.bartek.jamplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;

import pl.osmalek.bartek.jamplayer.mediaservice.MusicService;


public class MediaNotificationManager {

    private static final int NOTIFICATION_ID = 512;

    private final MusicService mService;
    private final NotificationManager mNotificationManager;
    private boolean mStarted;

    public MediaNotificationManager(MusicService musicService) {
        mService = musicService;
        mNotificationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }

    public void update(PlaybackStateCompat playbackState) {
        if (playbackState == null || playbackState.getState() == PlaybackStateCompat.STATE_STOPPED ||
                playbackState.getState() == PlaybackStateCompat.STATE_NONE) {
            if (playbackState == null || playbackState.getState() == PlaybackStateCompat.STATE_STOPPED) {
                mService.stopForeground(true);
                mService.stopSelf();
                mStarted = false;
            }
            return;
        }
        boolean isPlaying = playbackState.getState() == PlaybackStateCompat.STATE_PLAYING;

        NotificationCompat.Builder builder = MediaStyleHelper.from(mService, mService.getSession());
        builder.setSmallIcon(R.drawable.ic_album_white_48dp)
                .setColor(ContextCompat.getColor(mService, R.color.colorPrimaryDark));
        builder.addAction(new NotificationCompat.Action(R.drawable.ic_skip_previous_white_48dp, mService.getString(R.string.prev), MediaButtonReceiver.buildMediaButtonPendingIntent(mService, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
                .addAction(new NotificationCompat.Action(isPlaying ? R.drawable.ic_pause_white_48dp : R.drawable.ic_play_arrow_white_48dp, isPlaying ? mService.getString(R.string.pause) : mService.getString(R.string.play),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(mService, isPlaying ? PlaybackStateCompat.ACTION_PAUSE : PlaybackStateCompat.ACTION_PLAY)))
                .addAction(new NotificationCompat.Action(R.drawable.ic_skip_next_white_48dp, mService.getString(R.string.next), MediaButtonReceiver.buildMediaButtonPendingIntent(mService, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));

        builder.setStyle(new NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1, 2)
                .setMediaSession(mService.getSessionToken()));
        Notification notification = builder.build();
        if (isPlaying) {
            if (!mStarted) {
                mService.startService(new Intent(mService.getApplicationContext(), MusicService.class));
                mStarted = true;
            }
            mService.startForeground(NOTIFICATION_ID, notification);
        } else {
            mService.stopForeground(false);
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        }


    }
}
