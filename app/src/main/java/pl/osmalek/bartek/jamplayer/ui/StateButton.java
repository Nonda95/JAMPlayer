package pl.osmalek.bartek.jamplayer.ui;

import android.content.Context;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;

/**
 * Created by osmalek on 13.02.2017.
 */

public abstract class StateButton extends AppCompatImageButton {
    protected int mState;

    public StateButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(v -> {
            int nextState = (mState + 1) % getMaxStates();
            setState(nextState);
        });
        initState();
    }

    protected abstract void initState();

    protected abstract int getMaxStates();

    public void setState(int state) {
        mState = state;
        updateDrawable();
    }

    protected abstract void updateDrawable();
}
