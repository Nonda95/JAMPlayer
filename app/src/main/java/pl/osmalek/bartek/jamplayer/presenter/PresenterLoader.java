package pl.osmalek.bartek.jamplayer.presenter;

import android.content.Context;
import android.support.v4.content.Loader;

public class PresenterLoader<T extends Presenter> extends Loader<T> {
    private T mPresenter;
    private PresenterFactory<T> mPresenterFactory;

    /**
     * Stores away the application context associated with context.
     * Since Loaders can be used across multiple activities it's dangerous to
     * store the context directly; always use {@link #getContext()} to retrieve
     * the Loader's Context, don't use the constructor argument directly.
     * The Context returned by {@link #getContext} is safe to use across
     * Activity instances.
     *
     * @param context used to retrieve the application context.
     */
    public PresenterLoader(Context context, PresenterFactory<T> presenterFactory) {
        super(context);
        mPresenterFactory = presenterFactory;
    }

    @Override
    protected void onStartLoading() {
        if(mPresenter != null) {
            deliverResult(mPresenter);
            return;
        }
        forceLoad();
    }

    @Override
    protected void onForceLoad() {
        mPresenter = mPresenterFactory.create();
        mPresenter.onCreated(getContext());
        deliverResult(mPresenter);
    }

    @Override
    protected void onReset() {
        mPresenter.onDestroyed();
        mPresenter = null;
    }

    public T getPresenter() {
        return mPresenter;
    }
}
