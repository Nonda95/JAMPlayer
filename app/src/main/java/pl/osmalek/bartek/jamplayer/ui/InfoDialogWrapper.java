package pl.osmalek.bartek.jamplayer.ui;

import android.content.Context;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v7.app.AlertDialog;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import pl.osmalek.bartek.jamplayer.R;

/**
 * Created by osmalek on 15.02.2017.
 */

public class InfoDialogWrapper {
    private final AlertDialog dialog;
    private final Context context;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.author)
    TextView author;
    @BindView(R.id.trackNo)
    TextView trackNo;
    @BindView(R.id.album)
    TextView album;
    @BindView(R.id.year)
    TextView year;
    @BindView(R.id.source)
    TextView source;
    @BindView(R.id.duration)
    TextView duration;

    public InfoDialogWrapper(Context context) {
        this.context = context;
        FrameLayout v = new FrameLayout(context);
        dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.about_song))
                .setView(v)
                .setNeutralButton(context.getString(R.string.OK), (d, w) -> d.dismiss())
                .create();
        dialog.getLayoutInflater().inflate(R.layout.song_info, v);
        ButterKnife.bind(this, v);
    }

    public void show(MediaMetadataCompat metadata) {
        title.setText(String.format(Locale.getDefault(), context.getString(R.string.title), metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)));
        author.setText(String.format(Locale.getDefault(), context.getString(R.string.author), metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)));
        trackNo.setText(String.format(Locale.getDefault(), context.getString(R.string.trackNo), Long.toString(metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER))));
        album.setText(String.format(Locale.getDefault(), context.getString(R.string.album), metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)));
        year.setText(String.format(Locale.getDefault(), context.getString(R.string.year), Long.toString(metadata.getLong(MediaMetadataCompat.METADATA_KEY_YEAR))));
        source.setText(String.format(Locale.getDefault(), context.getString(R.string.source), metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)));
        long duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) / 1000;
        this.duration.setText(String.format(Locale.getDefault(), context.getString(R.string.duration_info), duration / 60, duration % 60));
        dialog.show();
    }
}
