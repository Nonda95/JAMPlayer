package pl.osmalek.bartek.jamplayer.model;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;

import com.google.gson.annotations.Expose;

import java.io.Serializable;


public abstract class BaseFile implements Serializable {
    @Expose
    private String filename;

    @Expose
    private String mediaId;
    Folder parent;

    private BaseFile(String mediaId, String filename, Folder parent) {
        this.mediaId = mediaId;
        this.filename = filename;
        this.parent = parent;
    }

    public BaseFile(String mediaId, String filename) {
        this(mediaId, filename, null);
    }

    public String getMediaId() {
        return mediaId;
    }

    public String getFilename() {
        return filename;
    }

    public Folder getParent() {
        return parent;
    }

    public abstract MediaBrowserCompat.MediaItem getAsMediaItem();

    public abstract MediaMetadataCompat getAsMediaMetadata();

    public abstract boolean isFolder();


}
