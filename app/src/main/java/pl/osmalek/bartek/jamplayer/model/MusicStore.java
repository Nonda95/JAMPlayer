package pl.osmalek.bartek.jamplayer.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by osmalek on 07.10.2016.
 */

public class MusicStore implements Serializable {

    final String track_id = MediaStore.Audio.Media._ID;
    final String track_no =MediaStore.Audio.Media.TRACK;
    final String track_name =MediaStore.Audio.Media.TITLE;
    final String artist = MediaStore.Audio.Media.ARTIST;
    final String duration = MediaStore.Audio.Media.DURATION;
    final String songAlbumId = MediaStore.Audio.Media.ALBUM_ID;
    final String year = MediaStore.Audio.Media.YEAR;
    final String path = MediaStore.Audio.Media.DATA;
    final String album_id = MediaStore.Audio.Albums._ID;
    final String album = MediaStore.Audio.Albums.ALBUM;
    final String album_art = MediaStore.Audio.Albums.ALBUM_ART;

    private static  MusicStore musicStore = new MusicStore();
    private boolean ready;

    private Folder mainFolder;

    private MusicStore() {
        this.mainFolder = new Folder("main");
        ready = false;
    }

    public synchronized void prepareStore(Context context) {
        ready = true;
        LinkedList<MusicFile> files = getTracks(context);
        for (MusicFile song : files) {
            addSong(song);
        }
        removeUnnecessaryFolders();
    }

    private void removeUnnecessaryFolders() {
        if(mainFolder.folders.size() == 1 && mainFolder.files.size() == 0)
        {
            mainFolder = mainFolder.folders.first();
            mainFolder.parent = null;
            removeUnnecessaryFolders();
        }
    }

    public static MusicStore getInstance() {
        return musicStore;
    }

    public Folder getMainFolder() {
        return mainFolder;
    }

    public BaseFile getFileFromUri(String path) {
        String[] pathArray = path.split("/");
        int i = 0;
        while(i < pathArray.length && !pathArray[i++].equals(mainFolder.getFilename()));
        return searchForFile(pathArray, i, mainFolder);
    }

    private BaseFile searchForFile(String[] pathArray, int position, Folder folder) {
        if(position < pathArray.length) {
            BaseFile file = folder.getFile(pathArray[position++]);
            if(file != null) {
                if(file instanceof Folder && position < pathArray.length) {
                    return searchForFile(pathArray, position, (Folder) file);
                } else {
                    return file;
                }
            }
        }
        return null;
    }

    public Cursor getTracksCursor(Context context) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        ContentResolver cr =  context.getContentResolver();
        final String[]columns={track_id, track_no, artist, track_name, songAlbumId, duration, path};
        final String selection = MediaStore.Audio.Media.IS_MUSIC + " <> 0";
        Cursor cursor = cr.query(uri,columns,selection,null,track_no + " ASC, " + path + " ASC");
        return cursor;
    }

    public Cursor getAlbumsCursor(Context context) {
        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;

        ContentResolver cr =  context.getContentResolver();
        final String[]columns={album_id, album_art, album};
        Cursor cursor = cr.query(uri,columns,null,null,null);
        return cursor;
    }

    public LinkedList<MusicFile> getTracks(Context context) {
        if(ready) {
            Cursor cursor = getTracksCursor(context);
            Dictionary<Long, String> albumsArt = getAlbumsArt(context);
            LinkedList<MusicFile> files = new LinkedList<>();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(this.album_id));
                Uri path = Uri.parse(cursor.getString(cursor.getColumnIndex(this.path)));
                String filename = path.getLastPathSegment();
                String artist = cursor.getString(cursor.getColumnIndex(this.artist));
                String title = cursor.getString(cursor.getColumnIndex(this.track_name));
                long album = cursor.getLong(cursor.getColumnIndex(this.songAlbumId));
                long duration = cursor.getLong(cursor.getColumnIndex(this.duration));
                String trackNumber = cursor.getString(cursor.getColumnIndex(this.track_no));
                MusicFile file = new MusicFile(id, filename, path, artist, title, album, duration, trackNumber, albumsArt.get(album));
                files.add(file);
            }
            return files;
        } else {
            throw new IllegalStateException("Music store not prepared");
        }
    }

    private Dictionary<Long, String> getAlbumsArt(Context context) {
        Dictionary<Long, String> albumsArt = new Hashtable<>();
        Cursor cursor = getAlbumsCursor(context);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String albumArt = cursor.getString(cursor.getColumnIndex(this.album_art));
            Long albumId = cursor.getLong(cursor.getColumnIndex(this.album_id));
            if(albumId != null && albumArt != null) {
                albumsArt.put(albumId, albumArt);
            }
        }
        return albumsArt;
    }

    public boolean addSong(MusicFile song) {
        Uri path = song.getPath();
        String songFilename = song.getFilename();
        Folder previousFolder = mainFolder;
        for(String filename : path.getPathSegments()) {
            BaseFile file = previousFolder.getFile(filename);
            if (file == null) {
                if(filename.equals(songFilename)) {
                    song.parent = previousFolder;
                    previousFolder.addFile(song);
                } else {
                    Folder folder = new Folder(filename);
                    folder.parent = previousFolder;
                    previousFolder.addFile(folder);
                    previousFolder = folder;
                }
            } else if(file.isFolder()) {
                previousFolder = (Folder) file;
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean isReady() {
        return ready;
    }

    public List<MediaSessionCompat.QueueItem> getBasicQueueForItem(String mediaId) {
        BaseFile file = getFileFromUri(mediaId);
        return addToQueue(new ArrayList<MediaSessionCompat.QueueItem>(), file instanceof Folder ? (Folder) file : file.getParent());
    }

    public List<MediaSessionCompat.QueueItem> addToQueue(@NonNull List<MediaSessionCompat.QueueItem> queue, Folder folder) {
        int i = queue.size();
        for(MusicFile file : folder.files) {
            queue.add(new MediaSessionCompat.QueueItem(file.getAsMediaMetadata().getDescription(), i++));
        }
        return queue;
    }

    public List<MediaSessionCompat.QueueItem> getAllSongsQueue() {
        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
        LinkedList<MusicFile> songs = mainFolder.getAllSongs();
        int i = 0;
        for(MusicFile song : songs) {
            queue.add(new MediaSessionCompat.QueueItem(song.getAsMediaMetadata().getDescription(), i++));
        }
        return queue;
    }

    public MediaMetadataCompat getMediaItem(String mediaId) {
        BaseFile file = getFileFromUri(mediaId);
        return file instanceof MusicFile ? ((MusicFile)file).getAsMediaMetadataWithArt() : file.getAsMediaMetadata();
    }
}
