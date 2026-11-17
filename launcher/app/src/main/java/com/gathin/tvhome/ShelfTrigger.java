package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

/**
 * An invisible view in Home's layout: resting on the Netflix tile opens {@link ShelfActivity}. Stays out of
 * the way until a companion app has written a shelf, so Home is unchanged without one.
 */
public class ShelfTrigger extends View {
    private static final long DELAY_MS = 5500;
    private static final String APP_LABEL = "Netflix";

    private final Runnable open = this::open;
    private final ViewTreeObserver.OnGlobalFocusChangeListener focus = (before, now) -> {
        removeCallbacks(open);
        // No "before" means Home is handing focus back, as it does on return from the shelf.
        if (before != null && now != null && before != now && now.getId() == R.id.card) postDelayed(open, DELAY_MS);
    };

    public ShelfTrigger(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalFocusChangeListener(focus);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(open);
        getViewTreeObserver().removeOnGlobalFocusChangeListener(focus);
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) removeCallbacks(open);
    }

    private void open() {
        TextView name = getRootView().findViewById(R.id.heroName);
        if (name == null || !APP_LABEL.contentEquals(name.getText())) return;
        if (!ShelfActivity.file(getContext()).isFile()) return;
        getContext().startActivity(new Intent(getContext(), ShelfActivity.class));
    }
}
