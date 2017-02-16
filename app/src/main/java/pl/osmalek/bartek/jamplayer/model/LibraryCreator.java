package pl.osmalek.bartek.jamplayer.model;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.LinkedList;


class LibraryCreator {
    private static final String track_id = MediaStore.Audio.Media._ID;
    private static final String track_no = MediaStore.Audio.Media.TRACK;
    private static final String track_name = MediaStore.Audio.Media.TITLE;
    private static final String artist = MediaStore.Audio.Media.ARTIST;
    private static final String duration = MediaStore.Audio.Media.DURATION;
    private static final String songAlbumId = MediaStore.Audio.Media.ALBUM_ID;
    private static final String year = MediaStore.Audio.Media.YEAR;
    private static final String path = MediaStore.Audio.Media.DATA;
    private static final String album_id = MediaStore.Audio.Albums._ID;
    private static final String album = MediaStore.Audio.Media.ALBUM;

    static synchronized Folder prepareStore(Context context) {
        Folder folder = new Folder("start", "start");
        LinkedList<MusicFile> files = getTracks(context);
        for (MusicFile song : files) {
            addSong(song, folder);
        }
        return removeUnnecessaryFolders(folder);
    }

    private static Folder removeUnnecessaryFolders(Folder folder) {
        if (folder.folders.size() == 1 && folder.files.size() == 0) {
            folder = folder.folders.first();
            folder.parent = null;
            return removeUnnecessaryFolders(folder);
        }
        return folder;
    }

    private static Cursor getTracksCursor(Context context) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        ContentResolver cr = context.getContentResolver();
        final String[] columns = {track_id, track_no, artist, track_name, songAlbumId, duration, path, year, album};
        final String selection = MediaStore.Audio.Media.IS_MUSIC + " <> 0";
        return cr.query(uri, columns, selection, null, track_no + " ASC, " + path + " ASC");
    }

    private static Cursor getAlbumsCursor(Context context) {
        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;

        ContentResolver cr = context.getContentResolver();
        final String[] columns = {album_id, album};
        return cr.query(uri, columns, null, null, null);
    }

    private static LinkedList<MusicFile> getTracks(Context context) {
        Cursor cursor = getTracksCursor(context);
        LinkedList<MusicFile> files = new LinkedList<>();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            long track_id = cursor.getLong(cursor.getColumnIndex(LibraryCreator.track_id));
            Uri path = Uri.parse(cursor.getString(cursor.getColumnIndex(LibraryCreator.path)));
            String filename = path.getLastPathSegment();
            String artist = cursor.getString(cursor.getColumnIndex(LibraryCreator.artist));
            String title = cursor.getString(cursor.getColumnIndex(LibraryCreator.track_name));
            long albumId = cursor.getLong(cursor.getColumnIndex(LibraryCreator.songAlbumId));
            String album = cursor.getString(cursor.getColumnIndex(LibraryCreator.album));
            long duration = cursor.getLong(cursor.getColumnIndex(LibraryCreator.duration));
            String trackNumber = cursor.getString(cursor.getColumnIndex(LibraryCreator.track_no));
            int year = cursor.getInt(cursor.getColumnIndex(LibraryCreator.year));

            String albumArt = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString();
            MusicFile file = new MusicFile(path.toString(), track_id, filename, path, artist, title, albumId, duration, trackNumber, album, albumArt, year);
            files.add(file);
        }
        return files;
    }

//    private static Dictionary<Long, String> getAlbums(Context context) {
//        Dictionary<Long, String> albums = new Hashtable<>();
//        Cursor cursor = getAlbumsCursor(context);
//        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
//            String album = cursor.getString(cursor.getColumnIndex(LibraryCreator.album));
//            Long albumId = cursor.getLong(cursor.getColumnIndex(album_id));
//            if (album != null) {
//                albums.put(albumId, album);
//            }
//        }
//        return albums;
//    }

    private static boolean addSong(MusicFile song, Folder rootFolder) {
        Uri path = song.getPath();
        String songFilename = song.getFilename();
        Folder previousFolder = rootFolder;
        StringBuilder folderPath = new StringBuilder();
        for (String filename : path.getPathSegments()) {
            folderPath.append("/");
            folderPath.append(filename);
            BaseFile file = previousFolder.getFile(filename);
            if (file == null) {
                if (filename.equals(songFilename)) {
                    song.parent = previousFolder;
                    previousFolder.addFile(song);
                } else {
                    Folder folder = new Folder(folderPath.toString(), filename);
                    folder.parent = previousFolder;
                    previousFolder.addFile(folder);
                    previousFolder = folder;
                }
            } else if (file.isFolder()) {
                previousFolder = (Folder) file;
            } else {
                return false;
            }
        }
        return true;
    }


}
