package club.bobfilm.app;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class ProgressDialog extends android.app.Dialog {

    Context context;
    View view;
    View backView;
    String title;
    TextView titleTextView;

    int progressColor = -1;

    public ProgressDialog(Context context, String title) {
        super(context, android.R.style.Theme_Translucent);
        this.title = title;
        this.context = context;
    }

    public ProgressDialog(Context context, String title, int progressColor) {
        super(context, android.R.style.Theme_Translucent);
        this.title = title;
        this.progressColor = progressColor;
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(club.bobfilm.app.R.layout.progress_bar);

        view = findViewById(club.bobfilm.app.R.id.contentDialog);
        backView = findViewById(club.bobfilm.app.R.id.dialog_rootView);
        backView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getX() < view.getLeft()
                        || event.getX() > view.getRight()
                        || event.getY() > view.getBottom()
                        || event.getY() < view.getTop()) {
                    //dismiss();
                }
                return false;
            }
        });

        titleTextView = (TextView) findViewById(club.bobfilm.app.R.id.progress_title);
        setTitle(title);
        if (progressColor != -1) {
            ProgressBarCircularIndeterminate progressBarCircularIndeterminate = (ProgressBarCircularIndeterminate) findViewById(club.bobfilm.app.R.id.progressBarCircularIndetermininate);
            progressBarCircularIndeterminate.setBackgroundColor(progressColor);
        }


    }

    @Override
    public void show() {
        try {
            super.show();
            // set dialog enter animations
            view.startAnimation(AnimationUtils.loadAnimation(context, club.bobfilm.app.R.anim.dialog_main_show_animation));
            backView.startAnimation(AnimationUtils.loadAnimation(context, club.bobfilm.app.R.anim.dialog_root_show_amin));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // GETERS & SETTERS

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        if (title == null)
            titleTextView.setVisibility(View.GONE);
        else {
            titleTextView.setVisibility(View.VISIBLE);
            titleTextView.setText(title);
        }
    }

    public TextView getTitleTextView() {
        return titleTextView;
    }

    public void setTitleTextView(TextView titleTextView) {
        this.titleTextView = titleTextView;
    }

    @Override
    public void dismiss() {
        Animation anim = AnimationUtils.loadAnimation(context, club.bobfilm.app.R.anim.dialog_main_hide_animation);
        anim.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        club.bobfilm.app.ProgressDialog.super.dismiss();
                    }
                });

            }
        });
        Animation backAnim = AnimationUtils.loadAnimation(context, club.bobfilm.app.R.anim.dialog_root_hide_amin);
        if (view != null) {
            view.startAnimation(anim);
        }
        if (backView != null) {
            backView.startAnimation(backAnim);
        }
    }

    public void dismissDelayedDialog(int delay, final ViewFlipper viewFlipper, final int flipperChild) {
        Handler mDelayer = new Handler();
        mDelayer.postDelayed(new Runnable() {
            public void run() {
                dismiss();
                viewFlipper.setDisplayedChild(flipperChild);
            }
        }, delay);  // milliseconds
    }
}
