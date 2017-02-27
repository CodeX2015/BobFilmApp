package club.bobfilm.app.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import club.bobfilm.app.Application;
import club.bobfilm.app.R;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 19.09.2016.
 */
public class TestImageZoomActivity extends AppCompatActivity implements View.OnClickListener {

    private Logger log = LoggerFactory.getLogger(TestImageZoomActivity.class);
    private ImageView mPoster;
    private boolean mZoomInImage = false;
    private LinearLayout mExpandedLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Application.setCurrentActivity(this);
        setContentView(R.layout.image_zoom_activity);
        mPoster = (ImageView) findViewById(R.id.iv_details_poster);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Utils.setImageViewBitmap(this,
                "http://fs176.www.ex.ua/show/272265208/272265208.jpg?1600".replace("?1600", "?200"),
                mPoster, null);
        mPoster.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_details_poster:
                //showDialogPreview();
                String path = "http://fs176.www.ex.ua/show/272265208/272265208.jpg?1600".replace("?1600", "?400");
                try {
                    mZoomInImage = true;
                    zoomImageFromThumb(v, path);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            case R.id.btn_change_size_1:
                changeViewSize(mPoster, 250, 200, v.getId());
                break;
            case R.id.btn_change_size_2:
                changeViewSize(mPoster, 300, 250, v.getId());
                break;
            case R.id.btn_change_size_3:
                changeViewSize(mPoster, 350, 300, v.getId());
                break;
        }
    }

    private void changeViewSize(View view, int height, int width, int btnId) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = height;
        params.width = width;

        switch (btnId) {
            case R.id.btn_change_size_1:
                view.setLayoutParams(params);
                break;
            case R.id.btn_change_size_2:
                view.requestLayout();
                break;
            case R.id.btn_change_size_3:
                view.invalidate();
                break;
        }
    }


    /**
     * Hold a reference to the current animator, so that it can be canceled mid-way.
     */
    private Animator mCurrentAnimator;

    /**
     * The system "short" animation time duration, in milliseconds. This duration is ideal for
     * subtle animations or animations that occur very frequently.
     */
    private int mShortAnimationDuration = 200;

    /**
     * "Zooms" in a thumbnail view by assigning the high resolution image to a hidden "zoomed-in"
     * image view and animating its bounds to fit the entire activity content area. More
     * specifically:
     * <p>
     * <ol>
     * <li>Assign the high-res image to the hidden "zoomed-in" (expanded) image view.</li>
     * <li>Calculate the starting and ending bounds for the expanded view.</li>
     * <li>Animate each of four positioning/sizing properties (X, Y, SCALE_X, SCALE_Y)
     * simultaneously, from the starting bounds to the ending bounds.</li>
     * <li>Zoom back out by running the reverse animation on click.</li>
     * </ol>
     *
     * @param thumbView The thumbnail view to zoom in.
     * @param imageUrl  Url of the high-resolution version of the image represented by the thumbnail.
     */
    private void zoomImageFromThumb(final View thumbView, String imageUrl) {
        // If there's an animation in progress, cancel it immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        mExpandedLayout = (LinearLayout) findViewById(R.id.ll_expanded);


        // Calculate the starting and ending bounds for the zoomed-in image. This step
        // involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail, and the
        // final bounds are the global visible rectangle of the container view. Also
        // set the container view's offset as the origin for the bounds, since that's
        // the origin for the positioning animation properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.ll_parent).getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            finalBounds.bottom = (int) (finalBounds.width() * 0.75);
        } else {
            finalBounds.bottom = (int) (finalBounds.width() / 0.75);
        }

        // Load the high-resolution "zoomed-in" image.
        final ImageView expandedImageView = (ImageView) findViewById(R.id.expanded_image);
        log.debug("img before height{}, width{}, finHeight{} , startLeft {}",
                expandedImageView.getHeight(), expandedImageView.getWidth(),
                finalBounds.height(), startBounds.left);
        float offsetX = 0;
        float offsetY = 0;
        ViewGroup.LayoutParams params = expandedImageView.getLayoutParams();
        if (expandedImageView.getHeight() > finalBounds.height()
                || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.height = (finalBounds.height() - (startBounds.top * 2));
            params.width = ((int) (finalBounds.height() * 0.75));
        } else {
            params.width = (finalBounds.width() - (startBounds.left * 10));
            params.height = (int) (params.width / 0.75);
            log.warn("{} = {} - ({}*6)", params.width, finalBounds.width(), startBounds.left);
        }
        offsetX = (finalBounds.width() - params.width) / 2;
        offsetY = (finalBounds.height() - params.height) / 2;

        log.debug("params: h={},w={}", params.height, params.width);
        expandedImageView.setLayoutParams(params);
        expandedImageView.requestLayout();

        log.debug("img result height{}, width{}, ofX={}, ofY={}",
                expandedImageView.getHeight(), expandedImageView.getWidth(), offsetX, offsetY);
        Utils.setImageViewBitmap(this, imageUrl, expandedImageView, null/*vfPreviewChanger*/);
        //expandedImageView.setImageResource(imageUrl);


        log.info("Anim: before startBounds: \nWidth: {} Height: {}" +
                        "\nLeft: {} Right: {}\nTop: {} Bottom: {}\n",
                startBounds.width(), startBounds.height(), startBounds.left, startBounds.right,
                startBounds.top, startBounds.bottom);

        log.info("Anim: before finalBounds: \nWidth: {} Height: {}" +
                        "\nLeft: {} Right: {}\nTop: {} Bottom: {}\n",
                finalBounds.width(), finalBounds.height(), finalBounds.left, finalBounds.right,
                finalBounds.top, finalBounds.bottom);

        // Adjust the start bounds to be the same aspect ratio as the final bounds using the
        // "center crop" technique. This prevents undesirable stretching during the animation.
        // Also calculate the start scaling factor (the end scaling factor is always 1.0).
        float startScale;
        float finalResult = finalBounds.width() / finalBounds.height();
        float startResult = startBounds.width() / startBounds.height();
        log.info("startResult: {}, finalResult: {}", startResult, finalResult);
        if (finalResult > startResult) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            log.info("horizontalBounds: startScale: {}, startWidth: {}, deltaWidth: {}",
                    startScale, startWidth, deltaWidth);
            //startBounds.left -= deltaWidth;
            //startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            log.info("verticalBounds: startScale: {}, startHeight: {}, deltaHeight: {}",
                    startScale, startHeight, deltaHeight);
            //startBounds.top -= deltaHeight;
            //startBounds.bottom += deltaHeight;
        }

        log.info("Anim: after startBounds: \nWidth: {} Height: {}" +
                        "\nLeft: {} Right: {}\nTop: {} Bottom: {}\n",
                startBounds.width(), startBounds.height(), startBounds.left, startBounds.right,
                startBounds.top, startBounds.bottom);

        log.info("Anim: after finalBounds: \nWidth: {} Height: {}" +
                        "\nLeft: {} Right: {}\nTop: {} Bottom: {}\n",
                finalBounds.width(), finalBounds.height(), finalBounds.left, finalBounds.right,
                finalBounds.top, finalBounds.bottom);

        // Hide the thumbnail and show the zoomed-in view. When the animation begins,
        // it will position the zoomed-in view in the place of the thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);
        mExpandedLayout.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations to the top-left corner of
        // the zoomed-in view (the default is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and scale properties
        // (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        log.info("set ZoomIN: \n1) {}, {}, {}\n2) {}, {}, {}\n3) {}, {}, {}\n4) {}, {}, {}\n",
                View.X, startBounds.left, finalBounds.left,
                View.Y, startBounds.top, finalBounds.top,
                View.SCALE_X, startScale, 1f,
                View.SCALE_Y, startScale, 1f);

        log.debug("x: {}, y: {}, fullX={}, ImgX={}, fullY={}, ImgY={}",
                offsetX, offsetY, finalBounds.width(), expandedImageView.getWidth(),
                finalBounds.height(), expandedImageView.getHeight());
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left,
                        /*finalBounds.left*/offsetX))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top,
                        /*finalBounds.top*/offsetY))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, /*startScale + 0.09f*/0.5f, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, /*startScale + 0.09f*/0.5f, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down to the original bounds
        // and show the thumbnail instead of the expanded image.
        final float startScaleFinal = startScale;
//        expandedImageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
        mExpandedLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel, back to their
                // original values.
                AnimatorSet set = new AnimatorSet();
                log.info("set ZoomOUT: \n1) {}, {}, {}\n2) {}, {}, {}\n3) {}, {}\n4) {}, {}\n",
                        View.X, startBounds.left, finalBounds.left,
                        View.Y, startBounds.top, finalBounds.top,
                        View.SCALE_X, startScaleFinal,
                        View.SCALE_Y, startScaleFinal);
                set
                        .play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView, View.SCALE_X, /*startScaleFinal + 0.09f*/0.5f))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView, View.SCALE_Y, /*startScaleFinal + 0.09f*/0.5f));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mExpandedLayout.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                        mZoomInImage = false;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }
                });
                set.start();
                mCurrentAnimator = set;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mZoomInImage && mExpandedLayout != null) {
            mExpandedLayout.callOnClick();
        } else {
            super.onBackPressed();
        }

    }
}
