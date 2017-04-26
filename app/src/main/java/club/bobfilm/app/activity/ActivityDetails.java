package club.bobfilm.app.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import club.bobfilm.app.Application;
import club.bobfilm.app.R;
import club.bobfilm.app.entity.Film;
import club.bobfilm.app.entity.FilmDetails;
import club.bobfilm.app.entity.FilmFile;
import club.bobfilm.app.helpers.BobFilmParser;
import club.bobfilm.app.helpers.DBHelper;
import club.bobfilm.app.service.DownloadService;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 24.04.2016.
 */
public class ActivityDetails extends BaseActivity implements View.OnClickListener {
    private static final int FILE_ACTION_SET = 0;
    private static final int FILE_ACTION_DOWNLOAD = 1;
    private static final int FILE_ACTION_PLAY = 2;
    private static final int FILE_ACTION_CHANGE = 3;
    private static final String LOG_TAG = "ActivityDetails";
    private Logger log = LoggerFactory.getLogger(getClass());
    private ViewFlipper mViewFlipper;
    private TextView mTVerror;
    private TextView mFilmTitle;
    private TextView mFilmCreateDate;
    private TextView mFilmReviewsCount;
    private LinearLayout mFilmReviews;
    private LinearLayout mAddBookmark;
    private ImageView ivBookmark;
    private ImageView mPoster;
    private TextView mFilmDetailsHTML;
    private LinearLayout svParentLayout;
    private LinearLayout mFilesLayout = null;
    private Film mFilm;
    private FilmDetails mFilmDetails;
    private List<FilmFile> mDataFromDB;
    private List<FilmFile> mFiles;
    private LinearLayout mShareBtn;
    private int mFileIndex;
    private LinearLayout mExpandedLayout;
    private boolean mZoomInImage;

    @Override
    protected void onResume() {
        super.onResume();
        Application.setCurrentActivity(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        mViewFlipper = (ViewFlipper) findViewById(R.id.vf_layout_changer);
        mTVerror = (TextView) findViewById(R.id.tv_error);
//        svParentLayout = (LinearLayout) findViewById(R.id.ll_parent);
        mFilesLayout = (LinearLayout) findViewById(R.id.ll_list_files);
        mFilmTitle = (TextView) findViewById(R.id.tv_details_title);
        mFilmCreateDate = (TextView) findViewById(R.id.tv_details_create_date);
        mFilmReviews = (LinearLayout) findViewById(R.id.ll_read_quotes);
        mFilmReviewsCount = (TextView) findViewById(R.id.tv_details_reviews_count);
        mFilmDetailsHTML = (TextView) findViewById(R.id.tv_details_html);
        mPoster = (ImageView) findViewById(R.id.iv_details_poster);
        mAddBookmark = (LinearLayout) findViewById(R.id.ll_add_bookmark);
        ivBookmark = (ImageView) findViewById(R.id.iv_bookmark_state);
        mShareBtn = (LinearLayout) findViewById(R.id.ll_share_details);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            //svParentLayout.setVisibility(View.INVISIBLE);
        }
        getData();
    }

    private void getData() {
        mViewFlipper.setDisplayedChild(0);
        Intent data = getIntent();
        if (data != null) {
            mFilm = (Film) data.getExtras().getSerializable(Utils.ARG_FILM_DETAILS);
            try {
                if (mFilm != null) {
                    getFilmDetailsByUrl(mFilm.getFilmUrl());
                } else {
                    String detailsUrl = data.getStringExtra(Utils.ARG_FILM_URL);
                    getFilmDetailsByUrl(detailsUrl);
                }
            } catch (Exception error) {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), error);
            }
        }
    }

    private void getFilmDetailsByUrl(final String urlQuery) {
        BobFilmParser.loadSite(urlQuery, BobFilmParser.ACTION_FILM_DETAILS, null,
                new BobFilmParser.LoadListener() {
                    @Override
                    public void OnLoadComplete(final Object result) {
                        mFilmDetails = (FilmDetails) result;
                        if (mFilmDetails != null) {
                            mFilmDetails = (FilmDetails) result;
                            if (mFilm != null) {
                                mFilmDetails.setPosterUrl(mFilm.getPosterUrl());
                                mFilmDetails.setFilmUrl(mFilm.getFilmUrl());
                                mFilmDetails.setBookmarked(mFilm.isBookmarked());
                            } else {
                                mFilm = new Film(
                                        mFilmDetails.getFilmTitle(),
                                        urlQuery,
                                        mFilmDetails.getBigPosterUrl().replace("?1600", "?200"),
                                        false
                                );
                            }
                            setData();
                        }
                    }

                    @Override
                    public void OnLoadError(final Exception ex) {
//                ex.printStackTrace();
                        if (!ex.getMessage().equalsIgnoreCase(getString(R.string.msg_connection_failed))) {
                            log.error(Utils.getErrorLogHeader() + new Object() {
                            }.getClass().getEnclosingMethod().getName(), ex);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                changeViewForError(ex.getMessage());
                            }
                        });
                    }
                });
    }

    private void changeViewForError(String msg) {
        if (mTVerror != null) {
            mTVerror.setText(msg);
            mTVerror.setTextColor(ContextCompat.getColor(this, R.color.text_color_error));
            mViewFlipper.setDisplayedChild(1);
        }
    }

    @SuppressLint("SetTextI18n")
    private void fillFilesAds() {
        mFilesLayout.removeAllViews();
        for (final FilmFile file : mFiles) {
            if (!file.getFileName().equalsIgnoreCase("") && Utils.isVideo(file.getFileName())) {
                setParentFilmData(file);
                final int position = mFiles.indexOf(file);
                final View view = getLayoutInflater().inflate(R.layout.item_list_file, mFilesLayout, false);
                TextView tvFileName = (TextView) view.findViewById(R.id.tv_file_name);
                tvFileName.setText(file.getFileName());
                ImageView ivActionDone = (ImageView) view.findViewById(R.id.iv_action_done);
                ivActionDone.setTag(position);
                ivActionDone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeFileState((int) v.getTag(), FILE_ACTION_CHANGE);
                    }
                });
                ImageView ivDownloadFile = (ImageView) view.findViewById(R.id.iv_file_download);
                ivDownloadFile.setTag(position);
                ivDownloadFile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        Toast.makeText(ActivityDetails.this, R.string.notice_download_start, Toast.LENGTH_SHORT).show();
                        FilmFile file = mFiles.get(mFileIndex);
                        changeFileState(mFileIndex, FILE_ACTION_DOWNLOAD);
                        if (!DownloadService.isDownloading(file)) {
                            Toast.makeText(ActivityDetails.this, R.string.notice_download_start, Toast.LENGTH_SHORT).show();
                            DownloadService.intentDownload(ActivityDetails.this, file);
                        } else {
                            Toast.makeText(ActivityDetails.this, R.string.notice_already_download, Toast.LENGTH_SHORT).show();
                        }
//                        showPopup(v, (int) v.getTag(), mPopupDownloadListener);
                    }
                });
                ImageView ivPlayFile = (ImageView) view.findViewById(R.id.iv_file_play);
                ivPlayFile.setTag(position);
                //registerForContextMenu(ivPlayFile);
                ivPlayFile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Toast.makeText(ActivityDetails.this, "ic_file_play", Toast.LENGTH_SHORT).show();
                        String urlVideoFile;
                        changeFileState(mFileIndex, FILE_ACTION_PLAY);
                        urlVideoFile = mFiles.get(mFileIndex).getFileUrl();
                        log.info("normal play {}", urlVideoFile);
                        Utils.playVideo(urlVideoFile, ActivityDetails.this);
//                        showPopup(v, (int) v.getTag(), mPopupPlayListener);
                    }
                });
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(0, 4, 0, 0);
                mFilesLayout.addView(view);
                mFilesLayout.requestLayout();
                changeFileState(position, FILE_ACTION_SET);
            } else {
                log.debug("Video: title {}, url {}, file {} isn't match file",
                        mFilm.getFilmTitle(), mFilm.getFilmUrl(), file.getFileName());
            }
        }
    }

    //Listener popup menu play button
    PopupMenu.OnMenuItemClickListener mPopupPlayListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            String urlVideoFile;
            changeFileState(mFileIndex, FILE_ACTION_PLAY);
            switch (item.getItemId()) {
                case R.id.popup_normal:
                    urlVideoFile = mFiles.get(mFileIndex).getFileUrl();
                    log.info("normal play {}", urlVideoFile);
                    break;
                case R.id.popup_light:
                    urlVideoFile = mFiles.get(mFileIndex).getLightFileUrl();
                    log.info("light play {}", urlVideoFile);
                    if (urlVideoFile == null) {
                        Toast toast = Toast.makeText(ActivityDetails.this,
                                R.string.msg_no_selected_video, Toast.LENGTH_SHORT);
                        //toast.setGravity(Gravity.CENTER_VERTICAL, 5, 5);
                        toast.show();
                        return false;
                    }
                    break;
                default:
                    return false;
            }
//            if (BuildConfig.DEBUG) {
//                return false;
//            }
            Utils.playVideo(urlVideoFile, ActivityDetails.this);
            return true;
        }
    };

    //Listener popup menu play button
    PopupMenu.OnMenuItemClickListener mPopupDownloadListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            FilmFile file = mFiles.get(mFileIndex);
            changeFileState(mFileIndex, FILE_ACTION_DOWNLOAD);

            switch (item.getItemId()) {
                case R.id.popup_normal:
                    log.info("normal download {}\nurl: {}\n", file.getFileName(), file.getFileUrl());
                    break;
                case R.id.popup_light:

                    log.info("light download {}\nurl: {}\n", file.getFileName(), file.getLightFileUrl());
                    if (file.getLightFileUrl() != null) {
                        file.setLightVersionChoice(true);
                        file.setFileName(file.getLightFileName());
                        file.setFileUrl(file.getLightFileUrl());
                    } else {
                        file.setLightVersionChoice(false);
                        Toast toast = Toast.makeText(ActivityDetails.this,
                                R.string.msg_no_selected_video, Toast.LENGTH_SHORT);
                        //toast.setGravity(Gravity.CENTER_VERTICAL, 5, 5);
                        toast.show();
                        return false;
                    }
                    break;
                default:
                    return false;
            }

//            if (!BuildConfig.DEBUG) {
            if (!DownloadService.isDownloading(file)) {
                Toast.makeText(ActivityDetails.this, R.string.notice_download_start, Toast.LENGTH_SHORT).show();
                DownloadService.intentDownload(ActivityDetails.this, file);
            } else {
                Toast.makeText(ActivityDetails.this, R.string.notice_already_download, Toast.LENGTH_SHORT).show();
            }
//            }
            return true;
        }
    };

    private void showPopup(View v, final int position,
                           PopupMenu.OnMenuItemClickListener mPopupListener) {
        mFileIndex = position;
        PopupMenu popup = new PopupMenu(this, v);
        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(mPopupListener);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.popup_menu_file, popup.getMenu());
        popup.show();
    }

    private void setParentFilmData(FilmFile file) {
        file.setFilmUrl(mFilm.getFilmUrl());
        file.setFilmLogoUrl(mFilm.getPosterUrl());
        file.setFilmTitle(mFilm.getFilmTitle());
        file.setFilmBookmarked(mFilm.isBookmarked());
    }

    private void changeFileState(int viewTag, int clickAction) {
        FilmFile file = mFiles.get(viewTag);
        if ((clickAction != FILE_ACTION_SET && !file.isViewed()) || clickAction == FILE_ACTION_CHANGE) {
            file.setViewed(!file.isViewed());
        }
        boolean fileState = file.isViewed();
        ImageView btnFileState = (ImageView) mFilesLayout.findViewWithTag(viewTag);
        if (btnFileState == null) {
            log.debug("btnFilState=null");
            return;
        }
        if (fileState) {
            btnFileState.setImageResource(R.drawable.ic_checkmark_on);
//            button.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_checkmark_on, null));
            if (clickAction != FILE_ACTION_SET) {
                if (clickAction == FILE_ACTION_CHANGE) {
                    log.info("changeFileState: ACTION_CHANGE");
                    DBHelper.getInstance(ActivityDetails.this).dbWorker(DBHelper.ACTION_ADD,
                            DBHelper.FN_HISTORY, file, null);
                }
                if (clickAction == FILE_ACTION_PLAY) {
                    log.info("changeFileState: ACTION_PLAY");
                    DBHelper.getInstance(ActivityDetails.this).dbWorker(DBHelper.ACTION_ADD,
                            DBHelper.FN_HISTORY, file, null);
                }
                if (clickAction == FILE_ACTION_DOWNLOAD) {
                    log.info("changeFileState: ACTION_DOWNLOAD");
//                    DBHelper.getInstance(ActivityDetails.this).dbWorker(DBHelper.ACTION_ADD,
//                                              DBHelper.FN_DOWNLOADS, file, null);
                }
            }
        } else {
            log.info("changeFileState: isACTION_SET {}", (clickAction == FILE_ACTION_SET));
//            button.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_checkmark_off, null));
            btnFileState.setImageResource(R.drawable.ic_checkmark_off);
//            DBHelper.getInstance(ActivityDetails.this).deleteHistoryFile(file);
            if (clickAction != FILE_ACTION_SET) {
                DBHelper.getInstance(ActivityDetails.this).dbWorker(DBHelper.ACTION_DELETE,
                        DBHelper.FN_HISTORY, file, null);
            }
        }
        mFilesLayout.invalidate();
    }

    @Override
    public void onBackPressed() {
        createResult();
        if (mZoomInImage && mExpandedLayout != null) {
            mExpandedLayout.callOnClick();
        } else {
            super.onBackPressed();
        }
    }

    private void createResult() {
        Intent myIntent = new Intent(this, ActivityTabMain.class);
        myIntent.putExtra(ActivityTabMain.EXTRA_FILM_DETAILS, mFilm);
        setResult(RESULT_OK, myIntent);
    }

    private void getDataFromDB() {
        DBHelper.getInstance(this).dbWorker(
                DBHelper.ACTION_GET,
                DBHelper.FN_HISTORY,
                null,
                new DBHelper.OnDBOperationListener() {
                    @Override
                    public void onSuccess(Object result) {
                        //noinspection unchecked
                        mDataFromDB = (List<FilmFile>) result;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                changeViewsStateOfButton();
                                fillFilesAds();
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception ex) {
                        ex.printStackTrace();
                        log.error(Utils.getErrorLogHeader() + new Object() {
                        }.getClass().getEnclosingMethod().getName(), ex);
                    }
                }
        );
    }

    private void changeViewsStateOfButton() {
        for (FilmFile file : mFiles) {
            file.setViewed(false);
            for (FilmFile fileHistory : mDataFromDB) {
                if (file.getFileName().equalsIgnoreCase(fileHistory.getFileName())) {
                    file.setViewed(!file.isViewed());
                }
            }
        }
    }

    private void setData() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mFiles = mFilmDetails.getFilmFiles();
                    if (mFiles != null && mFiles.size() > 0) {
                        getDataFromDB();
                    }
                    //svParentLayout.setVisibility(View.VISIBLE);
                    fillData();
                    mViewFlipper.setDisplayedChild(2);
                } catch (Exception error) {
                    log.error(Utils.getErrorLogHeader() + new Object() {
                    }.getClass().getEnclosingMethod().getName(), error);
                    error.printStackTrace();
                    changeViewForError(error.getMessage());
                }
            }
        });
    }

    private void fillData() {
        mFilmTitle.setText(mFilmDetails.getFilmTitle());
        mFilmCreateDate.setText(mFilmDetails.getFilmCreateDate());
        mFilmReviewsCount.setText(mFilmDetails.getFilmReviews());
        mFilmDetailsHTML.setText(Html.fromHtml(mFilmDetails.getFilmDetailsHTML()));
        String path = (mFilm.getPosterUrl() == null) ? mFilmDetails.getBigPosterUrl() : mFilm.getPosterUrl();
        Utils.setImageViewBitmap(this, path, mPoster, null);

        if (mFilmDetails.isBookmarked()) {
            ivBookmark.setImageResource(R.drawable.ic_bookmark_checked);
        }
        mAddBookmark.setOnClickListener(this);
        mFilmReviews.setOnClickListener(this);
        mShareBtn.setOnClickListener(this);
        mPoster.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_details_poster:
                //showDialogPreview();
                String path = mFilmDetails.getBigPosterUrl() != null ?
                        mFilmDetails.getBigPosterUrl().replace("?1600", "?400") : mFilm.getPosterUrl();
                zoomImageFromThumb(v, path);
                break;
            case R.id.ll_share_details:
                String shareString = mFilmDetails.getFilmTitle() + "\n\n" + mFilmDetails.getFilmUrl();
                Utils.shareTextUrl(ActivityDetails.this, getString(R.string.action_send_to), shareString);
                break;
            case R.id.ll_read_quotes:
//                todo need update comments
//                ActivityDetails.this.isCommentsExists(mFilmDetails.getFilmReviews(), mFilmDetails.getFilmReviewsUrl());
                Toast.makeText(this, "Under construction", Toast.LENGTH_SHORT).show();
                break;
            case R.id.ll_add_bookmark:
                setBookmark();
                break;
            case R.id.tv_error_repeat:
                getData();
                break;
            default:
                log.debug("{}: unknown id:{} clicked", getClass().getSimpleName(), v.getId());
                break;
        }
    }

    private void applyAnimation(final View startView, final View finishView, long duration) {
        float scalingFactor = ((float) finishView.getHeight()) / ((float) startView.getHeight());

        ScaleAnimation scaleAnimation = new ScaleAnimation(1f, scalingFactor,
                1f, scalingFactor,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        scaleAnimation.setDuration(duration);
        scaleAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

        Display display = getWindowManager().getDefaultDisplay();

        int H;

        if (Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            H = size.y;
        } else {
            H = display.getHeight();
        }

        float h = ((float) finishView.getHeight());

        float verticalDisplacement = (-(H / 2) + (3 * h / 4));

        TranslateAnimation translateAnimation = new TranslateAnimation(Animation.ABSOLUTE, 0,
                Animation.ABSOLUTE, 0,
                Animation.ABSOLUTE, 0,
                Animation.ABSOLUTE, verticalDisplacement);

        translateAnimation.setDuration(duration);
        translateAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimationSet animationSet = new AnimationSet(false);
        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(translateAnimation);
        animationSet.setFillAfter(false);

        startView.startAnimation(animationSet);
    }


    Animator mZoomAnimator;

    private void zoomImageAnimation(final View thumbView,
                                    final ImageView expandedImageView,
                                    final View expandedLayout,
                                    final int animationDuration) {

        if (mZoomAnimator != null) {
            mZoomAnimator.cancel();
        }

//        final FrameLayout expandedLayout = (FrameLayout) findViewById(R.id.fl_expanded);

        // Load the high-resolution "zoomed-in" image.
        //final ImageView expandedImageView = (ImageView) findViewById(R.id.expanded_image);
        // /Utils.setImageViewBitmap(this, imageUrl, expandedImageView, null/*vfPreviewChanger*/);
        //expandedImageView.setImageResource(imageUrl);

        // Calculate the starting and ending bounds for the zoomed-in image. This step
        // involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail, and the
        // final bounds are the global visible rectangle of the container view. Also
        // set the container view's offset as the origin for the bounds, since that's
        // the origin for the positioning animation properties (X, Y).
        mPoster.getGlobalVisibleRect(startBounds);
        findViewById(R.id.ll_expanded).getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        log.info("Anim: before startBounds: {}\nWidth: {}\nHeight: {}" +
                        "\nLeft: {}\nRight: {}\n",
                startBounds.width(), startBounds.height(), startBounds.left, startBounds.right);

        log.info("Anim: before finalBounds: {}\nWidth: {}\nHeight: {}" +
                        "\nLeft: {}\nRight: {}\n",
                finalBounds.width(), finalBounds.height(), finalBounds.left, finalBounds.right);

        // Adjust the start bounds to be the same aspect ratio as the final bounds using the
        // "center crop" technique. This prevents undesirable stretching during the animation.
        // Also calculate the start scaling factor (the end scaling factor is always 1.0).
        float startScale;
        float startWidth = 0;
        float deltaWidth = 0;
        float startHeight = 0;
        float deltaHeight = 0;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            startWidth = startScale * finalBounds.width();
            deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            startHeight = startScale * finalBounds.height();
            deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        log.info("Anim: startScale: {}\nstartWidth: {}\ndeltaWidth: {}" +
                        "\nstartHeight: {}\n" + "deltaHeight: {}\n",
                startScale, startWidth, deltaWidth, startHeight, deltaHeight);

        log.info("Anim: after startBounds: {}\nWidth: {}\nHeight: {}" +
                        "\nLeft: {}\nRight: {}\n",
                startBounds.width(), startBounds.height(), startBounds.left, startBounds.right);

        log.info("Anim: after finalBounds: {}\nWidth: {}\nHeight: {}" +
                        "\nLeft: {}\nRight: {}\n",
                finalBounds.width(), finalBounds.height(), finalBounds.left, finalBounds.right);

        // Hide the thumbnail and show the zoomed-in view. When the animation begins,
        // it will position the zoomed-in view in the place of the thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations to the top-left corner of
        // the zoomed-in view (the default is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and scale properties
        // (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        log.info("set: \n1) {}, {}, {}\n2) {}, {}, {}\n3) {}, {}, {}\n4) {}, {}, {}\n",
                View.X, startBounds.left, finalBounds.left,
                View.Y, startBounds.top, finalBounds.top,
                View.SCALE_X, startScale, 1f,
                View.SCALE_Y, startScale, 1f
        );
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left,
                        finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top,
                        finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f));

        set.setDuration(animationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mZoomAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mZoomAnimator = null;
            }
        });
        set.start();
        mZoomAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down to the original bounds
        // and show the thumbnail instead of the expanded image.
        final float startScaleFinal = startScale;
//        expandedImageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
        expandedLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mZoomAnimator != null) {
                    mZoomAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel, back to their
                // original values.
                AnimatorSet set = new AnimatorSet();
                set
                        .play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView, View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView, View.SCALE_Y, startScaleFinal));
                set.setDuration(animationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        expandedLayout.setVisibility(View.GONE);
                        mZoomAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mZoomAnimator = null;
                    }
                });
                set.start();
                mZoomAnimator = set;
            }
        });
    }

    private void showDialogPreview() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.ImagePreviewDialog);
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_poster_preview, null);
//        ViewFlipper vfPreviewChanger = (ViewFlipper) dialogLayout.findViewById(R.id.vf_preview_changer);
        final ImageView bigPoster = (ImageView) dialogLayout.findViewById(R.id.iv_preview_poster_small);
        final ImageView bigPosterExpanded = (ImageView) dialogLayout.findViewById(R.id.expanded_image);
        final View expandedLayout = dialogLayout.findViewById(R.id.fl_expanded);
        if (bigPosterExpanded == null /*|| vfPreviewChanger == null*/) {
            return;
        }

        //vfPreviewChanger.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_up));
        //vfPreviewChanger.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_down));

        bigPoster.setImageDrawable(mPoster.getDrawable());

        String path;
        path = mFilmDetails.getBigPosterUrl() != null ?
                mFilmDetails.getBigPosterUrl().replace("?1600", "?400") : mFilm.getPosterUrl();
        Utils.setImageViewBitmap(this, path, bigPosterExpanded, null/*vfPreviewChanger*/);


        builder.setView(dialogLayout);


        final AlertDialog dialog = builder.create();
        //dialog.getWindow().getAttributes().windowAnimations = R.style.ImagePreviewDialog; //style id
        try {
            //zoomImageAnimation(bigPoster, bigPosterExpanded, expandedLayout, 500);
            applyAnimation(mPoster, bigPosterExpanded, 500);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        dialogLayout.setBackgroundResource(android.R.color.transparent);
//        //gesture slide down handle
//        final float[] initialY = new float[1];
//        dialogLayout.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent motion) {
//                switch (motion.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
////                        ImageView img = (ImageView) v.findViewById(R.id.iv_preview_poster);
////                        Animation animation = AnimationUtils
////                                .loadAnimation(getApplicationContext(), R.anim.alpha_resize_click);
////                        v.startAnimation(animation);
//                        initialY[0] = motion.getY();
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        float finalY = motion.getY();
//                        if (initialY[0] < finalY) {
//                            dialog.cancel();
//                        }
//                        break;
//                }
//                return true;
//            }
//        });

//        dialog.setCancelable(false);
        dialog.show();
    }

    private void setBookmark() {
        if (!mFilmDetails.isBookmarked()) {
            mFilmDetails.setBookmarked(true);
            mFilm.setBookmarked(true);
            ivBookmark.setImageResource(R.drawable.ic_bookmark_checked);
//                    DBHelper.getInstance(ActivityDetails.this).addBookmark(mFilm);
            DBHelper.getInstance(ActivityDetails.this).dbWorker(DBHelper.ACTION_ADD, DBHelper.FN_BOOKMARKS, mFilm, null);
        } else {
            mFilmDetails.setBookmarked(false);
            mFilm.setBookmarked(false);
            ivBookmark.setImageResource(R.drawable.ic_bookmark_unchecked);
//                    DBHelper.getInstance(ActivityDetails.this).deleteBookmark(mFilm);
            DBHelper.getInstance(ActivityDetails.this).dbWorker(DBHelper.ACTION_DELETE, DBHelper.FN_BOOKMARKS, mFilm, null);
        }
    }

//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v,
//                                    ContextMenu.ContextMenuInfo menuInfo) {
//        int position = (int) v.getTag();
//        mFileIndex = (int) v.getTag();
//        log.info("is(iv_file_play) {}, position = {}", (v.getId() == R.id.iv_file_play), position);
//        super.onCreateContextMenu(menu, v, menuInfo);
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.popup_menu_file, menu);
//    }
//
//    @Override
//    public boolean onContextItemSelected(MenuItem item) {
//        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
//        String urlVideoFile;
//        switch (item.getItemId()) {
//            case R.id.popup_light:
//
//            case R.id.popup_normal:
//
//            default:
//                return super.onContextItemSelected(item);
//        }
//    }


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


}
