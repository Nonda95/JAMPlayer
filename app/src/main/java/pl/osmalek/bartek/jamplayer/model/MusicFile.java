package pl.osmalek.bartek.jamplayer.model;

import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;

import com.google.gson.annotations.Expose;

public class MusicFile extends BaseFile {
    @Expose
    long trackId;
    @Expose
    String artist;
    @Expose
    String title;
    @Expose
    long albumId;
    @Expose
    long duration;
    @Expose
    String trackNumber;
    @Expose
    private String path;
    @Expose
    String albumArt;

    public MusicFile(String mediaId, long track_id, String filename, Uri path, String artist, String title, long albumId, long duration, String trackNumber, String albumArt) {
        super(mediaId, filename);
        this.trackId = track_id;
        this.path = path.toString();
        this.artist = artist;
        this.title = title;
        this.albumId = albumId;
        this.duration = duration;
        this.trackNumber = trackNumber;
        this.albumArt = albumArt;
    }

    public String getArtist() {
        return artist;
    }

    public Uri getAlbumArt() {
        return albumArt == null ? null : Uri.parse(albumArt);
    }

    public String getTitle() {
        return title;
    }

    public Uri getPath() {
        return Uri.parse("file://" + path);
    }

    public long getAlbumId() {
        return albumId;
    }

    public long getTrackId() {
        return trackId;
    }

    @Override
    public MediaBrowserCompat.MediaItem getAsMediaItem() {
        return new MediaBrowserCompat.MediaItem(getAsMediaMetadata().getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    @Override
    public MediaMetadataCompat getAsMediaMetadata() {
        return getMetadataBuilder().build();
    }

    private MediaMetadataCompat.Builder getMetadataBuilder() {
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, path)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title != null ? title : getFilename())
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, Long.valueOf(trackNumber))
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, path);
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    public MediaMetadataCompat getAsMediaMetadataWithArt() {
        return getMetadataBuilder()
//                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeFile(albumArt))
                .build();
    }
}
