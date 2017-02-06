package pl.osmalek.bartek.jamplayer.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static pl.osmalek.bartek.jamplayer.SharedPrefConsts.MAIN_FOLDER_ID;
import static pl.osmalek.bartek.jamplayer.SharedPrefConsts.SHARED_PREF;

/**
 * Created by osmalek on 07.10.2016.
 */

public class MusicProvider implements Serializable {
    private boolean ready;

    private Folder mainFolder;
    private Folder rootFolder;
    private Context mContext;

    public MusicProvider(Context context) {
        mContext = context;
        this.rootFolder = new Folder("start", "start");
        ready = false;
    }


    public synchronized void prepare() {
        if(!ready) {
            ready = true;
            rootFolder = LibraryCreator.prepareStore(mContext);
            restoreMainFolder();
        }
    }

    private void restoreMainFolder() {
        SharedPreferences pref = mContext.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        String mainFolderId = pref.getString(MAIN_FOLDER_ID, rootFolder.getMediaId());
        Folder mainFolder = (Folder) getFileFromUri(mainFolderId);
        if (mainFolder != null) {
            setMainFolder(mainFolder);
        }
    }

    public void setMainFolder(Folder mainFolder) {
        this.mainFolder = mainFolder;
        SharedPreferences.Editor pref = mContext.getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
        pref.putString(MAIN_FOLDER_ID, mainFolder.getMediaId());
        pref.apply();
    }

    public Folder getMainFolder() {
        return mainFolder != null ? mainFolder : rootFolder;
    }

    public BaseFile getFileFromUri(String path) {
        String[] pathArray = path.split("/");
        int i = 0;
        while (i < pathArray.length && !pathArray[i++].equals(getRootFolder().getFilename())) ;
        if (i == pathArray.length && pathArray[i - 1].equals(getRootFolder().getFilename())) {
            return getRootFolder();
        }
        return searchForFile(pathArray, i, getRootFolder());
    }

    private BaseFile searchForFile(String[] pathArray, int position, Folder folder) {
        if (position < pathArray.length) {
            BaseFile file = folder.getFile(pathArray[position++]);
            if (file != null) {
                if (file instanceof Folder && position < pathArray.length) {
                    return searchForFile(pathArray, position, (Folder) file);
                } else {
                    return file;
                }
            }
        }
        return null;
    }

    public List<MediaSessionCompat.QueueItem> getBasicQueueForItem(String mediaId) {
        BaseFile file = getFileFromUri(mediaId);
        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
        addToQueue(queue, (file instanceof Folder ? (Folder) file : file.getParent()).files);
        return queue;
    }

    public List<MediaSessionCompat.QueueItem> addToQueue(@NonNull List<MediaSessionCompat.QueueItem> queue, Collection<MusicFile> songs) {
        int i = queue.size();
        for (MusicFile file : songs) {
            queue.add(new MediaSessionCompat.QueueItem(file.getAsMediaMetadata().getDescription(), i++));
        }
        return queue;
    }

    public List<String> getIdsFromQueue(@NonNull List<MediaSessionCompat.QueueItem> queue) {
        List<String> result = new ArrayList<>();
        for (MediaSessionCompat.QueueItem item : queue) {
            result.add(item.getDescription().getMediaId());
        }
        return result;
    }

    public List<MediaSessionCompat.QueueItem> getQueueFromIds(@NonNull List<String> mediaIds) {
        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
        int i = 0;
        for (String mediaId : mediaIds) {
            BaseFile file = getFileFromUri(mediaId);
            if (file != null) {
                queue.add(new MediaSessionCompat.QueueItem(file.getAsMediaMetadata().getDescription(), i++));
            }
        }
        return queue;
    }

    public List<MediaSessionCompat.QueueItem> getAllSongsQueue() {
        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
        LinkedList<MusicFile> songs = getMainFolder().getAllSongs();
        int i = 0;
        for (MusicFile song : songs) {
            queue.add(new MediaSessionCompat.QueueItem(song.getAsMediaMetadata().getDescription(), i++));
        }
        return queue;
    }

    public MediaMetadataCompat getMediaItem(String mediaId) {
        BaseFile file = getFileFromUri(mediaId);
        return file instanceof MusicFile ? ((MusicFile) file).getAsMediaMetadataWithArt(mContext) : file.getAsMediaMetadata();
    }

    public int getIndexForQueue(List<MediaSessionCompat.QueueItem> queue, @NonNull String mediaId) {
        for (int i = 0; i < queue.size(); i++) {
            if (mediaId.equals(queue.get(i).getDescription().getMediaId())) {
                return i;
            }
        }
        return 0;
    }

    public Folder getRootFolder() {
        return rootFolder;
    }

    public interface OnPreparedCallback {
        void onPrepared(boolean success);
    }
}
