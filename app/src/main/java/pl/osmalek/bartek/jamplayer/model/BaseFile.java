package pl.osmalek.bartek.jamplayer.model;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

/**
 * Created by osmalek on 07.10.2016.
 */

public abstract class BaseFile implements Serializable {
    @Expose
    private String filename;
    Folder parent;

    public BaseFile(String filename, Folder parent) {
        this.filename = filename;
        this.parent = parent;
    }

    public BaseFile(String filename) {
        this.filename = filename;
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
