package pl.osmalek.bartek.jamplayer;

import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.transition.TransitionManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class PlayingNowSheetCallback extends android.support.design.widget.BottomSheetBehavior.BottomSheetCallback {
    private final ConstraintSet collapsedSet;
    private final ConstraintSet expandedSet;
    private AppCompatActivity mContext;
    private ConstraintLayout layout;
    private boolean isExpandedLayout;
    private FloatingActionButton mFab;
    private AccelerateDecelerateInterpolator interpolator;

    public PlayingNowSheetCallback(AppCompatActivity context, ConstraintLayout layout, FloatingActionButton fab) {
        mContext = context;
        this.layout = layout;
//        mContent = content;
        mFab = fab;
        isExpandedLayout = false;
        interpolator = new AccelerateDecelerateInterpolator();
        collapsedSet = new ConstraintSet();
        collapsedSet.clone(context, R.layout.collapsed_sheet);
        expandedSet = new ConstraintSet();
        expandedSet.clone(context, R.layout.expanded_sheet);
    }

    @Override
    public void onStateChanged(@NonNull View bottomSheet, int newState) {
        CoordinatorLayout.LayoutParams fabParams = (CoordinatorLayout.LayoutParams) mFab.getLayoutParams();
        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
            fabParams.gravity = Gravity.BOTTOM;
            mFab.setLayoutParams(fabParams);
            mFab.setTranslationX(0);
//            mPlayingQueueButton.setVisibility(View.VISIBLE);
//            mCloseSheetButton.setVisibility(View.VISIBLE);
//            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mTitle.getLayoutParams();
//            params.setMarginEnd(mContext.getResources().getDimensionPixelSize(R.dimen.titleExpandedMargin));
//            params.setMarginStart(mContext.getResources().getDimensionPixelSize(R.dimen.titleExpandedMargin));
//            mTitle.setLayoutParams(params);
//            mArtist.setVisibility(View.VISIBLE);
            if (!isExpandedLayout) {
                TransitionManager.beginDelayedTransition(layout);
                expandedSet.applyTo(layout);
                isExpandedLayout = true;
            }
        } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
//            mPlayingQueueButton.setVisibility(View.INVISIBLE);
//            mCloseSheetButton.setVisibility(View.GONE);
//            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)mTitle.getLayoutParams();
//            params.setMarginEnd(mContext.getResources().getDimensionPixelSize(R.dimen.titleEndMargin));
//            params.setMarginStart(mContext.getResources().getDimensionPixelSize(R.dimen.titleStartMargin));
//            mTitle.setLayoutParams(params);

            if (isExpandedLayout) {
                TransitionManager.beginDelayedTransition(layout);
                collapsedSet.applyTo(layout);
                isExpandedLayout = false;
            }
//            mPlayingQueueButton.setVisibility(View.INVISIBLE);
//            mCloseSheetButton.setVisibility(View.GONE);
//            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mTitle.getLayoutParams();
//            params.setMarginEnd(mContext.getResources().getDimensionPixelSize(R.dimen.titleEndMargin));
//            params.setMarginStart(mContext.getResources().getDimensionPixelSize(R.dimen.titleStartMargin));
//            mTitle.setLayoutParams(params);
//            fabParams.gravity = Gravity.END | Gravity.BOTTOM;
//            mFab.setLayoutParams(fabParams);
//            mFab.setTranslationX(0);
//            mArtist.setVisibility(View.GONE);


//            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)mTitle.getLayoutParams();
//            params.horizontalBias = 0;
//            mTitle.setLayoutParams(params);
        } else if (newState == BottomSheetBehavior.STATE_DRAGGING) {
            if (isExpandedLayout) {
                TransitionManager.beginDelayedTransition(layout);
                collapsedSet.applyTo(layout);
                isExpandedLayout = false;
            }
//            mPlayingQueueButton.setVisibility(View.INVISIBLE);
//            mCloseSheetButton.setVisibility(View.GONE);
//            mArtist.setVisibility(View.GONE);
//            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mTitle.getLayoutParams();
//            params.setMarginEnd(mContext.getResources().getDimensionPixelSize(R.dimen.titleEndMargin));
//            params.setMarginStart(mContext.getResources().getDimensionPixelSize(R.dimen.titleStartMargin));
//            mTitle.setLayoutParams(params);
        }
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        float offset = interpolator.getInterpolation(slideOffset);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (slideOffset > 0.99) {
//                mContext.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
//                mContext.getWindow().setStatusBarColor(Color.rgb(224, 224, 224));
//            } else {
//                mContext.getWindow().getDecorView().setSystemUiVisibility(0);
//                mContext.getWindow().setStatusBarColor(ContextCompat.getColor(mContext, R.color.colorPrimaryDark));
//            }
//        }
//        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mContent.getLayoutParams();
//        int offset = Math.round(mContext.getResources().getDimension(R.dimen.statusbar_margin)*slideOffset);
//        params.setMargins(params.leftMargin, offset, params.rightMargin, Math.round(mContext.getResources().getDimension(R.dimen.statusbar_margin)) - offset);
//        mContent.setLayoutParams(params);

        View fabParent = (View) mFab.getParent();
        CoordinatorLayout.LayoutParams fabParams = (CoordinatorLayout.LayoutParams) mFab.getLayoutParams();
        if (fabParams.gravity != Gravity.BOTTOM) {
            mFab.setTranslationX(((fabParent.getWidth() - mFab.getWidth()) / 2 - mFab.getX() + mFab.getTranslationX()) * offset);
        } else {
            mFab.setTranslationX(((fabParent.getWidth() - fabParams.width) / 2 - fabParams.getMarginEnd()) * (1 - offset));
        }
//        View parent = (View) mTitle.getParent();
//        int parentWidth = parent.getWidth();
//        mTitle.setTranslationX(((parentWidth - mTitle.getWidth()) / 2 - mTitle.getX() + mTitle.getTranslationX()) * offset);
//        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)mTitle.getLayoutParams();
//        params.horizontalBias = offset/2;
//        mTitle.setLayoutParams(params);
    }
}
