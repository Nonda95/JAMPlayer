package pl.osmalek.bartek.jamplayer;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;

import java.io.FileNotFoundException;

import pl.osmalek.bartek.jamplayer.ui.MainActivity;

/**
 * Helper APIs for constructing MediaStyle notifications
 */
class MediaStyleHelper {
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of {@link MediaMetadataCompat#getDescription()} to extract the appropriate information.
     *
     * @param context      Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */
    public static NotificationCompat.Builder from(
            Context context, MediaSessionCompat mediaSession) {
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        Bitmap bitmap = null;
        if (description.getIconUri() != null) {
            try {
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(description.getIconUri(), "r");
                if (pfd != null)
                    bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (bitmap == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_album_white_48dp);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.SHOW_PLAYING_NOW, true);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(bitmap)
                .setContentIntent(PendingIntent.getActivity(context, 1, intent, 0))
                .setDeleteIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT);
        return builder;
    }
}