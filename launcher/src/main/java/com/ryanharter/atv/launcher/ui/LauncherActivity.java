package com.ryanharter.atv.launcher.ui;

import com.ryanharter.atv.launcher.LauncherAppState;
import com.ryanharter.atv.launcher.R;
import com.ryanharter.atv.launcher.ui.AppListFragment.Callbacks;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class LauncherActivity extends Activity
        implements Callbacks {

    ViewGroup mContainer;
    View mCollapsedBackground, mExpandedBackground;

    private static final int STATE_COLLAPSED = 0;
    private static final int STATE_EXPANDED = 1;

    private int mState = STATE_EXPANDED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        // TODO Move this to the Application subclass
        LauncherAppState.setApplicationContext(getApplicationContext());

        mContainer = (ViewGroup) findViewById(R.id.container);
        mCollapsedBackground = findViewById(R.id.collapsed_bg);
        mExpandedBackground = findViewById(R.id.expanded_bg);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new AppListFragment())
                    .commit();
        }
    }

    private void collapse() {
        int height = mContainer.getMeasuredHeight();

        List<Animator> animators = new ArrayList<>();
        animators.add(ObjectAnimator.ofFloat(mContainer, View.TRANSLATION_Y, height - 220));
        animators.add(ObjectAnimator.ofFloat(mCollapsedBackground, View.ALPHA, 0, 1));
        animators.add(ObjectAnimator.ofFloat(mExpandedBackground, View.ALPHA, 1, 0));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        set.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mState = STATE_COLLAPSED;
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        set.start();
    }

    private void expand() {
        List<Animator> animators = new ArrayList<>();
        animators.add(ObjectAnimator.ofFloat(mContainer, View.TRANSLATION_Y, 0));
        animators.add(ObjectAnimator.ofFloat(mCollapsedBackground, View.ALPHA, 1, 0));
        animators.add(ObjectAnimator.ofFloat(mExpandedBackground, View.ALPHA, 0, 1));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        set.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mState = STATE_EXPANDED;
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        set.start();
    }

    @Override
    public void onExpandButtonClick() {
        switch (mState) {
            case STATE_COLLAPSED:
                expand();
                break;
            case STATE_EXPANDED:
                collapse();
                break;
        }
    }
}
