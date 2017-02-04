package pl.osmalek.bartek.jamplayer.model;

import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

public class Folder extends BaseFile implements Comparable<Folder> {
    @Expose
    SortedSet<Folder> folders;
    @Expose
    LinkedHashSet<MusicFile> files;

    Folder(String mediaId, String filename, LinkedHashSet<MusicFile> files, SortedSet<Folder> folders) {
        super(mediaId, filename);
        this.files = files;
        this.folders = folders;
    }

    Folder(String mediaId, String filename) {
        this(mediaId, filename, new LinkedHashSet<>(), new TreeSet<>());
    }

    @Override
    public MediaBrowserCompat.MediaItem getAsMediaItem() {
        return new MediaBrowserCompat.MediaItem(getAsMediaMetadata().getDescription(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    @Override
    public MediaMetadataCompat getAsMediaMetadata() {
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, getMediaId())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getFilename())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, String.format(Locale.getDefault(), "%d elements", files.size() + folders.size()))
                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, files.size())
                .build();
    }

    public List<MediaBrowserCompat.MediaItem> getFilesAsMediaItem() {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        for(BaseFile file : getFiles()) {
            mediaItems.add(file.getAsMediaItem());
        }
        return mediaItems;
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    public BaseFile getFile(String filename) {
        for (MusicFile file : files) {
            if (file.getFilename().equals(filename)) {
                return file;
            }
        }
        for (Folder file : folders) {
            if (file.getFilename().equals(filename)) {
                return file;
            }
        }
        return null;
    }

    public LinkedList<BaseFile> getFiles() {
        LinkedList<BaseFile> allFiles = new LinkedList<>();
        allFiles.addAll(folders);
        allFiles.addAll(files);
        return allFiles;
    }

    public LinkedList<MusicFile> getAllSongs() {
        LinkedList<MusicFile> songs = new LinkedList<>();
        for(Folder folder : folders) {
            songs.addAll(folder.getAllSongs());
        }
        songs.addAll(files);
        return songs;
    }

    public boolean addFile(BaseFile file) {
        return file.isFolder() ? folders.add((Folder) file) : files.add((MusicFile) file);
    }

    @Override
    public int compareTo(@NonNull Folder folder) {
        return getFilename().compareTo(folder.getFilename());
    }
}
