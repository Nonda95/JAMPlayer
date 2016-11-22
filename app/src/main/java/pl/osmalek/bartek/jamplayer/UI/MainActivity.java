package pl.osmalek.bartek.jamplayer.UI;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import butterknife.ButterKnife;
import pl.osmalek.bartek.jamplayer.MusicService;
import pl.osmalek.bartek.jamplayer.R;
import pl.osmalek.bartek.jamplayer.model.MusicStore;


public class MainActivity extends AppCompatActivity {
    MusicStore musicStore;
    private MediaBrowserCompat mMediaBrowser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        musicStore = MusicStore.getInstance();
        if (!musicStore.isReady()) {
            musicStore.prepareStore(this);
        }
        FileListFragment fileListFragment = FileListFragment.newInstance(musicStore.getMainFolder());
        getFragmentManager().beginTransaction().add(R.id.file_list_fragment_container, fileListFragment).commit();

        mMediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MusicService.class),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        try {
                            MediaSessionCompat.Token token =
                                    mMediaBrowser.getSessionToken();
                            // This is what gives us access to everything
                            MediaControllerCompat controller =
                                    new MediaControllerCompat(MainActivity.this, token);
                            setSupportMediaController(controller);
                        } catch (RemoteException e) {
                            Log.e(MainActivity.class.getSimpleName(),
                                    "Error creating controller", e);
                        }

                    }

                    @Override
                    public void onConnectionSuspended() {
                        super.onConnectionSuspended();
                    }

                    @Override
                    public void onConnectionFailed() {
                        super.onConnectionFailed();
                    }
                }, null);
        mMediaBrowser.connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        mMediaBrowser.disconnect();
        super.onDestroy();
    }
}
