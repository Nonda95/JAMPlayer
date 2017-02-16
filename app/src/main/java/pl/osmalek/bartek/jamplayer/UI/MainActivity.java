package pl.osmalek.bartek.jamplayer.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.github.florent37.glidepalette.GlidePalette;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.Disposable;
import pl.osmalek.bartek.jamplayer.App;
import pl.osmalek.bartek.jamplayer.PlayingNowSheetCallback;
import pl.osmalek.bartek.jamplayer.R;
import pl.osmalek.bartek.jamplayer.adapters.QueueListAdapter;


public class MainActivity extends AppCompatActivity //        implements LoaderManager.LoaderCallbacks<MainPresenter>
{
    public static final String SHOW_PLAYING_NOW = "pl.osmalek.bartek.ui.playing_now";
    private static final int LOADER_ID = 11;
    private static final String PLAYING_NOW_EXPANDED = "playingNowExpandded";
    @BindView(R.id.bottom_sheet)
    View bottomSheet;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    //    @BindView(R.id.bs_content)
//    LinearLayout bs_content;
    @BindView(R.id.queueList)
    ListView queueList;
    @BindView(R.id.drawer)
    DrawerLayout drawerLayout;
    @BindView(R.id.title_playing_now)
    TextView title;
    @BindView(R.id.cover)
    ImageView cover;
    @BindView(R.id.seekBar)
    SeekBar seekBar;
    @BindView(R.id.playingQueueButton)
    ImageButton playingQueueButton;
    @BindView(R.id.closeSheetButton)
    ImageButton closeSheetButton;
    @BindView(R.id.subtitle_playing_now)
    TextView artist;
    @BindView(R.id.duration)
    TextView duration;
    @BindView(R.id.position)
    TextView position;

    private BottomSheetBehavior mBottomSheetBehavior;
    private QueueListAdapter mQueueAdapter;
    private PlayingNowSheetCallback mSheetCallback;
    private PlaybackStateCompat mLastState;
    private final MediaControllerCallback mControllerCallback = new MediaControllerCallback();
    private MediaBrowserCompat mBrowser;
    private MediaControllerCompat mController;
    private Disposable mBrowserSubscription;

    private final Handler mHandler = new Handler();
    private final Runnable mUpdateProgressTask = this::updateProgress;
    private InfoDialogWrapper mInfoDialogWrapper;

    private void updateProgress() {
        long progress = SystemClock.elapsedRealtime() - mLastState.getLastPositionUpdateTime();
        updateProgress((int) (mLastState.getPosition() + progress) / 1000);
    }

    private final ScheduledExecutorService mExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> mScheduleFuture;

    public void updateProgress(int progress) {
        setPosition(progress);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            seekBar.setProgress(progress, true);
        } else {
            seekBar.setProgress(progress);
        }
    }

    private final SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            setPosition(i);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().seekTo(seekBar.getProgress()*1000);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportFragmentManager().findFragmentById(R.id.file_list_fragment_container) == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.file_list_fragment_container, BrowseFragment.newInstance()).commit();
        }
        mInfoDialogWrapper = new InfoDialogWrapper(this);
        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setBottomSheetCallback(mSheetCallback = new PlayingNowSheetCallback(this, title, fab, playingQueueButton, closeSheetButton, artist));
        queueList.setAdapter(mQueueAdapter = new QueueListAdapter(this, new ArrayList<>()));
        queueList.setOnItemClickListener((adapterView, view, i, l) -> MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().skipToQueueItem(l));
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(PLAYING_NOW_EXPANDED, false)) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                mSheetCallback.onStateChanged(bottomSheet, BottomSheetBehavior.STATE_EXPANDED);
            }
        } else {
            showPlayingNowIfNeeded(getIntent());
        }
        title.setSelected(true);
        mBrowserSubscription = App.get().getBrowserSubject()
                .subscribe(isBrowserReady -> {
                    if (isBrowserReady) {
                        mBrowser = App.get().getMediaBrowser();
                        browserReady();
                    } else {
                        mBrowser = null;
                    }
                });
    }


    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(PLAYING_NOW_EXPANDED, mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        showPlayingNowIfNeeded(intent);
    }

    private void showPlayingNowIfNeeded(Intent intent) {
        if (intent != null && intent.getBooleanExtra(SHOW_PLAYING_NOW, false)) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            mSheetCallback.onStateChanged(bottomSheet, BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    public void setPlayButton(boolean isPlaying) {
        fab.setImageResource(isPlaying ? R.drawable.ic_pause_white_48dp : R.drawable.ic_play_arrow_white_48dp);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        getMenuInflater().inflate(R.menu.menu_more, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.queueDrawer:
                openPlayingQueue(null);
                return true;
//            case R.id.action_settings:
//                startActivity(new Intent(this, SettingsActivity.class));
//                return true;
            default:
                return false;
        }
    }

    public void openPlayingQueue(View v) {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    @Override
    protected void onDestroy() {
        if (mBrowserSubscription != null && !mBrowserSubscription.isDisposed())
            mBrowserSubscription.dispose();
        mController.unregisterCallback(mControllerCallback);
        stopSeekBarUpdate();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            closeBottomSheet(null);
        } else {
            if (!((BrowseFragment) getSupportFragmentManager().findFragmentById(R.id.file_list_fragment_container)).handleBackPressed())
                super.onBackPressed();
        }
    }

    public void closeBottomSheet(View view) {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mSheetCallback.onStateChanged(bottomSheet, BottomSheetBehavior.STATE_COLLAPSED);
//        playingQueueButton.setVisibility(View.INVISIBLE);
//        closeSheetButton.setVisibility(View.GONE);
//        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) title.getLayoutParams();
//        params.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.titleEndMargin));
//        params.setMarginStart(getResources().getDimensionPixelSize(R.dimen.titleStartMargin));
//        title.setLayoutParams(params);
    }

    public void prevSong(View view) {
        MediaControllerCompat.getMediaController(this).getTransportControls().skipToPrevious();
    }

    public void nextSong(View view) {
        MediaControllerCompat.getMediaController(this).getTransportControls().skipToNext();
    }

    public void showTrackInfo(View view) {
        MediaMetadataCompat metadata = mController.getMetadata();
        mInfoDialogWrapper.show(metadata);
    }

    public void onBottomSheetClick(View v) {
        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        Log.d("BottomSheetClick", "Clicked!!");
    }

    @OnClick(R.id.fab)
    public void onFabClick(View view) {
        if (MediaControllerCompat.getMediaController(MainActivity.this).getPlaybackState() != null && MediaControllerCompat.getMediaController(MainActivity.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
            MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().pause();
        } else {
            MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().play();
        }
    }

    public void setSongTitle(CharSequence songTitle) {
        title.setText(songTitle);
    }

    public void setSongArtist(CharSequence songArtist) {
        artist.setText(songArtist);
    }

    public void setDuration(int duration) {
        seekBar.setMax(duration);
        this.duration.setText(String.format(Locale.getDefault(), getString(R.string.duration), duration / 60, duration % 60));
    }

    public void setPlayingQueue(List<MediaSessionCompat.QueueItem> queueAdapter) {

        mQueueAdapter.setQueue(queueAdapter);
    }

    public void browserReady() {
        try {
            mController = new MediaControllerCompat(MainActivity.this, mBrowser.getSessionToken());
            mController.registerCallback(mControllerCallback);
            MediaControllerCompat.setMediaController(this, mController);
            mControllerCallback.onQueueChanged(mController.getQueue());
            mControllerCallback.onMetadataChanged(mController.getMetadata());
            mControllerCallback.onPlaybackStateChanged(mController.getPlaybackState());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private void scheduleSeekBarUpdate() {
        stopSeekBarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    () -> mHandler.post(mUpdateProgressTask), 100,
                    200, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekBarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    private void loadCover(MediaMetadataCompat metadata) {
        Glide.with(this).load(metadata.getDescription().getIconUri())
                .apply(RequestOptions.errorOf(AppCompatResources.getDrawable(this, R.drawable.ic_album_primary_48dp)))
                .listener(GlidePalette.with(metadata.getDescription().getIconUri() != null ? metadata.getDescription().getIconUri().toString() : null)
                        .use(GlidePalette.Profile.MUTED_LIGHT)
                        .crossfade(true)
                        .intoBackground(cover))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(cover);
    }

    public void setPosition(int position) {
        this.position.setText(String.format(Locale.getDefault(), "%-5s", String.format(Locale.getDefault(), "%d:%02d", position / 60, position % 60)));
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state != null) {
                mLastState = state;
                setPlayButton(mLastState.getState() == PlaybackStateCompat.STATE_PLAYING);
                updateProgress((int) state.getPosition() / 1000);
                mQueueAdapter.setCurrentMediaIndex(state.getActiveQueueItemId());
                if (!drawerLayout.isDrawerOpen(GravityCompat.END))
                    queueList.smoothScrollToPosition((int) state.getActiveQueueItemId());
                if (state.getState() == PlaybackStateCompat.STATE_PLAYING)
                    scheduleSeekBarUpdate();
                else
                    stopSeekBarUpdate();
            }
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            if (queue != null) {
                setPlayingQueue(queue);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                setSongTitle(metadata.getDescription().getTitle());
                setSongArtist(metadata.getDescription().getSubtitle());
                setDuration((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) / 1000);
                updateProgress(0);
                if (!isDestroyed())
                    loadCover(metadata);
            }
        }

    }

}
