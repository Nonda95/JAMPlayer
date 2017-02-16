package pl.osmalek.bartek.jamplayer.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import pl.osmalek.bartek.jamplayer.R;

import static pl.osmalek.bartek.jamplayer.SharedPrefConsts.PLAYBACK_SHARED_PREF;
import static pl.osmalek.bartek.jamplayer.SharedPrefConsts.REPEAT_MODE;
import static pl.osmalek.bartek.jamplayer.mediaservice.MusicService.NO_REPEAT;
import static pl.osmalek.bartek.jamplayer.mediaservice.MusicService.REPEAT_ALL;
import static pl.osmalek.bartek.jamplayer.mediaservice.MusicService.REPEAT_ONE;
import static pl.osmalek.bartek.jamplayer.mediaservice.MusicService.SHUFFLE;

/**
 * Created by osmalek on 13.02.2017.
 */

public class RepeatButton extends StateButton {

    private SharedPreferences mSharedPref;

    public RepeatButton(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    protected void initState() {
        mSharedPref = getContext().getSharedPreferences(PLAYBACK_SHARED_PREF, Context.MODE_PRIVATE);
        if(mSharedPref != null) {
            mState = mSharedPref.getInt(REPEAT_MODE, REPEAT_ALL);
        } else {
            mState = REPEAT_ALL;
        }
        updateDrawable();
    }

    @Override
    protected int getMaxStates() {
        return 4;
    }

    @Override
    public void setState(int state) {
        super.setState(state);
        mSharedPref.edit().putInt(REPEAT_MODE, mState).apply();
    }

    @Override
    protected void updateDrawable() {
        switch (mState) {
            case REPEAT_ALL:
                setImageResource(R.drawable.ic_repeat_primary_24dp);
                break;
            case REPEAT_ONE:
                setImageResource(R.drawable.ic_repeat_one_primary_24dp);
                break;
            case NO_REPEAT:
                setImageResource(R.drawable.ic_repeat_disabled_24dp);
                break;
            case SHUFFLE:
                setImageResource(R.drawable.ic_shuffle_primary_24dp);
        }
    }
}
