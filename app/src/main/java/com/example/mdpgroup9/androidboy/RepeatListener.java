package com.example.mdpgroup9.androidboy;

/**
 * Created by lofma on 6/9/2017.
 */

import android.app.Activity;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.TextView;


public class RepeatListener implements OnTouchListener{


    public Activity activity;




    private TextView textViewStatus;
    private Handler handler = new Handler();

    private int initialInterval;
    private final int normalInterval;
    private final OnClickListener clickListener;

    private Runnable handlerRunnable = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this, normalInterval);
            clickListener.onClick(downView);
        }
    };

    private View downView;


    /**
     * @param initialInterval The interval after first click event
     * @param normalInterval The interval after second and subsequent click
     *       events
     * @param clickListener The OnClickListener, that will be called
     *       periodically
     */
    public RepeatListener(int initialInterval, int normalInterval,
                          OnClickListener clickListener , Activity _activity) {
        if (clickListener == null)
            throw new IllegalArgumentException("null runnable");
        if (initialInterval < 0 || normalInterval < 0)
            throw new IllegalArgumentException("negative interval");

        this.initialInterval = initialInterval;
        this.normalInterval = normalInterval;
        this.clickListener = clickListener;
        this.activity = _activity;
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handler.removeCallbacks(handlerRunnable);
                handler.postDelayed(handlerRunnable, initialInterval);
                downView = view;
                downView.setPressed(true);
                clickListener.onClick(view);
                return true;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(handlerRunnable);
                downView = null;

                TextView tv = (TextView)this.activity.findViewById(R.id.textViewStatus);
                tv.setText("Stop");
                break;
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(handlerRunnable);
                downView.setPressed(false);
                downView = null;
                return true;
        }

        return false;
    }
}
