package pl.osmalek.bartek.jamplayer.presenter;

import android.content.Context;

interface Presenter<V> {
    void onViewAttached(V view);
    void onViewDetached();
    void onDestroyed();

    void onCreated(Context context);
}
