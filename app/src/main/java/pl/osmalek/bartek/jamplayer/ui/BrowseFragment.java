package pl.osmalek.bartek.jamplayer.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import pl.osmalek.bartek.jamplayer.App;
import pl.osmalek.bartek.jamplayer.R;
import pl.osmalek.bartek.jamplayer.adapters.FileAdapter;
import pl.osmalek.bartek.jamplayer.mediaservice.MusicService;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BrowseFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BrowseFragment extends Fragment implements FileAdapter.OnFileClickListener {

    private static final String FOLDER_ID = "folderId";
    private static final int LOADER_ID = 11;
    private static final String HAS_PARENT = "hasParent";
    View parent;
    @BindView(R.id.song_list)
    RecyclerView songList;
    private FileAdapter fileAdapter;
    private String folderId;
    private boolean mHasParent;
    private MediaBrowserCompat mMediaBrowser;

    public BrowseFragment() {
        // Required empty public constructor
    }

    public static BrowseFragment newInstance(String folderId, boolean hasParent) {
        final Bundle bundle = new Bundle();
        bundle.putString(FOLDER_ID, folderId);
        bundle.putBoolean(HAS_PARENT, hasParent);
        BrowseFragment fragment = new BrowseFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static BrowseFragment newInstance() {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(HAS_PARENT, false);
        BrowseFragment fragment = new BrowseFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setHasOptionsMenu(true);
    }

    private void browserReady() {
        folderId = getArguments().getString(FOLDER_ID);
        if (folderId == null) {
            folderId = mMediaBrowser.getRoot();
        }
        mMediaBrowser.subscribe(folderId, new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children) {
                fileAdapter.setMediaItems(children);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        parent = inflater.inflate(R.layout.fragment_browse, container, false);
        ButterKnife.bind(this, parent);
        songList.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        songList.setLayoutManager(layoutManager);
        mHasParent = getArguments().getBoolean(HAS_PARENT);
        fileAdapter = new FileAdapter(null, this, getContext(), mHasParent);
        songList.setAdapter(fileAdapter);
        App.get().getBrowserSubject()
                .subscribe(mediaBrowserCompat -> {
                    mMediaBrowser = mediaBrowserCompat;
                    browserReady();
                });
        return parent;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    //    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        menu.findItem(R.id.setDefault).setVisible(folder != mMusicProvider.getMainFolder());
//        menu.findItem(R.id.restoreDefault).setVisible(mMusicProvider.getMainFolder() != mMusicProvider.getRootFolder());
//        super.onCreateOptionsMenu(menu, inflater);
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.setDefault:
//                final Bundle bundle = new Bundle();
//                bundle.putString(MusicService.CUSTOM_CMD_MEDIA_ID, folderId);
//                MediaControllerCompat.getMediaController(getActivity()).getTransportControls().sendCustomAction(MusicService.CUSTOM_CMD_SET_MAIN_FOLDER, bundle);
//                ((MainActivity) getActivity()).initFragment();
//                break;
//            case R.id.restoreDefault:
//                MediaControllerCompat.getMediaController(getActivity()).getTransportControls().sendCustomAction(MusicService.CUSTOM_CMD_RESET_MAIN_FOLDER, null);
//                ((MainActivity) getActivity()).initFragment();
//                break;
//            default:
//                return false;
//        }
//        return true;
//    }

    @Override
    public void onUpClick() {
        handleBackPressed();
    }

//    private void replaceWithParent() {
//        BrowseFragment fragment = BrowseFragment.newInstance(folder.getParent(), mMusicProvider);
//        getFragmentManager().beginTransaction().setCustomAnimations(
//                android.R.anim.slide_in_left, android.R.anim.slide_out_left)
//                .replace(R.id.file_list_fragment_container, fragment).commit();
//    }

    @Override
    public void onFolderClick(MediaDescriptionCompat folder) {
        BrowseFragment fragment = BrowseFragment.newInstance(folder.getMediaId(), true);
        getFragmentManager().beginTransaction().addToBackStack(null).setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.file_list_fragment_container, fragment).commit();
    }

    @Override
    public void onSongClick(MediaDescriptionCompat song) {
        MediaControllerCompat.getMediaController(getActivity()).getTransportControls().playFromMediaId(song.getMediaId(), null);
    }

    @Override
    public void onMenuClick(View view, ImageButton button) {
        int position = songList.getChildLayoutPosition(view);
        if (mHasParent) {
            if (position == 0) {
                return;
            }
            position--;
        }
        final MediaBrowserCompat.MediaItem mediaItem = fileAdapter.getMediaItems().get(position);
        Context wrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme_PopupMenu);
        PopupMenu menu = new PopupMenu(wrapper, button);
        prepareMenu(menu, mediaItem);

        menu.show();
    }

    private void prepareMenu(PopupMenu menu, MediaBrowserCompat.MediaItem mediaItem) {
        menu.setGravity(Gravity.TOP | Gravity.END);
        menu.getMenuInflater().inflate(R.menu.menu_more, menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.addToQueue || item.getItemId() == R.id.playFile) {
                Bundle bundle = new Bundle();
                bundle.putString(MusicService.CUSTOM_CMD_MEDIA_ID, mediaItem.getMediaId());
                MediaControllerCompat.getMediaController(getActivity()).getTransportControls()
                        .sendCustomAction(item.getItemId() == R.id.addToQueue ?
                                MusicService.CUSTOM_CMD_ADD_TO_QUEUE : MusicService.CUSTOM_CMD_PLAY, bundle);
                Log.i(null, "Custom action sent");
            }
            return false;
        });
    }

    public boolean handleBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            return true;
        }
        return false;
    }
}
