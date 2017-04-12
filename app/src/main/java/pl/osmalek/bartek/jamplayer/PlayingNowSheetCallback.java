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
    private ConstraintLayout layout;
    private boolean isExpandedLayout;
    private FloatingActionButton mFab;
    private AccelerateDecelerateInterpolator interpolator;

    public PlayingNowSheetCallback(AppCompatActivity context, ConstraintLayout layout, FloatingActionButton fab) {
        this.layout = layout;
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
            if (!isExpandedLayout) {
                TransitionManager.beginDelayedTransition(layout);
                expandedSet.applyTo(layout);
                isExpandedLayout = true;
            }
        } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {

            if (isExpandedLayout) {
                TransitionManager.beginDelayedTransition(layout);
                collapsedSet.applyTo(layout);
                isExpandedLayout = false;
            }
        } else if (newState == BottomSheetBehavior.STATE_DRAGGING) {
            if (isExpandedLayout) {
                TransitionManager.beginDelayedTransition(layout);
                collapsedSet.applyTo(layout);
                isExpandedLayout = false;
            }
        }
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        float offset = interpolator.getInterpolation(slideOffset);

        View fabParent = (View) mFab.getParent();
        CoordinatorLayout.LayoutParams fabParams = (CoordinatorLayout.LayoutParams) mFab.getLayoutParams();
        if (fabParams.gravity != Gravity.BOTTOM) {
            mFab.setTranslationX(((fabParent.getWidth() - mFab.getWidth()) / 2 - mFab.getX() + mFab.getTranslationX()) * offset);
        } else {
            mFab.setTranslationX(((fabParent.getWidth() - fabParams.width) / 2 - fabParams.getMarginEnd()) * (1 - offset));
        }
    }
}
