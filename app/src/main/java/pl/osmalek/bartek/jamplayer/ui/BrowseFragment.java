package pl.osmalek.bartek.jamplayer.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.Disposable;
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
    private static final String LIST_STATE = "listPosition";
    View parent;
    @BindView(R.id.song_list)
    RecyclerView songList;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    private FileAdapter fileAdapter;
    private String folderId;
    private boolean mHasParent;
    private MediaBrowserCompat mMediaBrowser;
    private Menu menu;
    private Parcelable listState;
    private Disposable mBrowserSubscription;

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
        setHasOptionsMenu(true);
    }

    private void browserReady() {
        folderId = getArguments().getString(FOLDER_ID);
        if (folderId == null) {
            folderId = mMediaBrowser.getRoot();
        }
        prepareMenuVisibilityIfPossible();
        mMediaBrowser.subscribe(folderId, new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children) {
                progressBar.setVisibility(View.GONE);
                fileAdapter.setMediaItems(children);
                songList.getLayoutManager().onRestoreInstanceState(listState);
                //songList.getLayoutManager().scrollToPosition(scrollPosition);
            }
        });
    }

    private void prepareMenuVisibilityIfPossible() {
        if (mMediaBrowser != null && menu != null) {
            String rootId = null;
            if (mMediaBrowser.getExtras() != null)
                rootId = mMediaBrowser.getExtras().getString(MusicService.ROOT_ID);
            menu.findItem(R.id.setDefault).setVisible(!folderId.equals(rootId) && !folderId.equals(mMediaBrowser.getRoot()));
            menu.findItem(R.id.restoreDefault).setVisible(!mMediaBrowser.getRoot().equals(rootId));
        }
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
        if (savedInstanceState != null) {
            listState = savedInstanceState.getParcelable(LIST_STATE);
        }
        mBrowserSubscription = App.get().getBrowserSubject()
                .subscribe(isBrowserReady -> {
                    if (isBrowserReady) {
                        mMediaBrowser = App.get().getMediaBrowser();
                        browserReady();
                    } else {
                        mMediaBrowser = null;
                    }
                });
        progressBar.setVisibility(View.VISIBLE);
        return parent;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(LIST_STATE, songList.getLayoutManager().onSaveInstanceState());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        songList.getLayoutManager().onRestoreInstanceState(listState);
    }

    @Override
    public void onStop() {
        listState = songList.getLayoutManager().onSaveInstanceState();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mBrowserSubscription != null && !mBrowserSubscription.isDisposed()) {
            mBrowserSubscription.dispose();
        }
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        prepareMenuVisibilityIfPossible();
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Bundle bundle = new Bundle();
        bundle.putString(MusicService.CUSTOM_CMD_MEDIA_ID, folderId);
        switch (item.getItemId()) {
            case R.id.setDefault:
                MediaControllerCompat.getMediaController(getActivity()).getTransportControls().sendCustomAction(MusicService.CUSTOM_CMD_SET_MAIN_FOLDER, bundle);
                App.get().reconnectBrowser();
                getFragmentManager().popBackStack(0, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                getFragmentManager().beginTransaction().replace(R.id.file_list_fragment_container, BrowseFragment.newInstance()).commit();
                return true;
            case R.id.restoreDefault:
                MediaControllerCompat.getMediaController(getActivity()).getTransportControls().sendCustomAction(MusicService.CUSTOM_CMD_RESET_MAIN_FOLDER, null);
                App.get().reconnectBrowser();
                getFragmentManager().popBackStack(0, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getFragmentManager().beginTransaction().replace(R.id.file_list_fragment_container, BrowseFragment.newInstance()).commit();
                return true;
            case R.id.addToQueue:
                MediaControllerCompat.getMediaController(getActivity()).getTransportControls()
                        .sendCustomAction(MusicService.CUSTOM_CMD_ADD_TO_QUEUE, bundle);
                Log.i(null, "Add to queue action sent");
                return true;
            case R.id.playFile:
                MediaControllerCompat.getMediaController(getActivity()).getTransportControls()
                        .sendCustomAction(MusicService.CUSTOM_CMD_PLAY, bundle);
                return true;
            case R.id.playNext:
                MediaControllerCompat.getMediaController(getActivity()).getTransportControls()
                        .sendCustomAction(MusicService.CUSTOM_CMD_PLAY_NEXT, bundle);
                Log.i(null, "Play next action sent");
                return true;
            default:
                return false;
        }
    }

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
            Bundle bundle = new Bundle();
            bundle.putString(MusicService.CUSTOM_CMD_MEDIA_ID, mediaItem.getMediaId());
            switch (item.getItemId()) {
                case R.id.addToQueue:
                    MediaControllerCompat.getMediaController(getActivity()).getTransportControls()
                            .sendCustomAction(MusicService.CUSTOM_CMD_ADD_TO_QUEUE, bundle);
                    Log.i(null, "Add to queue action sent");
                    return true;
                case R.id.playFile:
                    MediaControllerCompat.getMediaController(getActivity()).getTransportControls()
                            .sendCustomAction(MusicService.CUSTOM_CMD_PLAY, bundle);
                    return true;
                case R.id.playNext:
                    MediaControllerCompat.getMediaController(getActivity()).getTransportControls()
                            .sendCustomAction(MusicService.CUSTOM_CMD_PLAY_NEXT, bundle);
                    return true;
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
