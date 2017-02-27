package club.bobfilm.app.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import butterknife.BindView;
import butterknife.ButterKnife;
import club.bobfilm.app.Application;
import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.entity.AppUpdate;
import club.bobfilm.app.helpers.HTMLParser;
import club.bobfilm.app.util.Utils;


/**
 * Created by CodeX on 09.06.2015.
 */
public class ActivitySplash extends AppCompatActivity {
    private Logger log = LoggerFactory.getLogger(ActivitySplash.class);
    private String mDetailsUrl;
    @BindView(R.id.iv_logo)
    ImageView mLogo;
    AlertDialog mDialog;

    private Animator.AnimatorListener mAnimationListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            mLogo.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animator) {

        }

        @Override
        public void onAnimationCancel(Animator animator) {

        }

        @Override
        public void onAnimationRepeat(Animator animator) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Application.setCurrentActivity(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);
        checkVersion();
        Utils.setAppSettings(this);
        Utils.setLogData();

        //check site available and start app
        checkSiteAndStartApp();
//        splashAnimation();
    }

    private void checkVersion() {
        log.warn("HTMLParser.checkForUpdate");
        HTMLParser.checkForUpdate(this, new HTMLParser.LoadListener() {
            @Override
            public void OnLoadComplete(Object result) {
                final AppUpdate appUpdate = (AppUpdate) result;
                ActivitySettings.mAppNewVersionAvailable = true;
                appUpdate.isVersionNewer(true);
                ActivitySettings.mAppNewVersion = appUpdate;
                //Toast.makeText(context, "new version available", Toast.LENGTH_SHORT).show();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //showUpdateDialog(context, newApkUrl);
                        AppCompatActivity context = Application.getCurrentActivity();
//                         TODO: 14.10.2016 Uncomment after paying
                        ActivitySettings.mAppNewVersionAvailable = true;
                        ActivitySettings.mAppNewVersionUrl = appUpdate.getDownloadUrl();
                        Utils.pushNotify(context,
                                context.getString(R.string.msg_update_available),
                                Utils.createDownloadUpdatePI(context, appUpdate.getDownloadUrl()));

                        // TODO: 14.10.2016 Comment after paying
//                        if (ActivitySettings.mAppNewVersionAvailable) {
//                            Utils.emptyMessage(context,
//                                    getString(R.string.app_old_version,
//                                            ActivitySettings.mAppNewVersion.getVersion()),
//                                    true);
//
//                        } else {
//                            //check site available and start app
//                            checkSiteAndStartApp();
//                        }
                    }
                });
            }

            @Override
            public void OnLoadError(Exception ex) {
                ActivitySettings.mAppNewVersionAvailable = false;
                if (ex instanceof InterruptedException) {
                    log.info("No updates found. Application is up to date.");
                    return;
                }
                if (BuildConfig.DEBUG) {
                    ex.printStackTrace();
                } else {
                    log.error(Utils.getErrorLogHeader() + new Object() {
                    }.getClass().getEnclosingMethod().getName(), ex);
                }
            }
        });
    }

    private boolean isActionView() {
        final String action = getIntent().getAction();
        return Intent.ACTION_VIEW.equals(action);
    }

    private boolean isValidDetailsLink() {
        final Intent intent = getIntent();

        //final List<String> segments = intent.getData().get;
//            if (segments.size() > 1) {
//               String mUsername = segments.get(1);
//            }
        Uri data = intent.getData();
        log.info("\nHost: {}\nnPath: {}\nQuery: {}\n",
                data.getHost(), data.getEncodedPath(), data.getEncodedQuery());
        if (!data.getEncodedPath().contains("video")
                && data.getEncodedQuery().contains("r=")) {
            mDetailsUrl = data.getEncodedPath() + "?" + data.getEncodedQuery();
            return true;
        }
        return false;
    }

    private void checkSiteAndStartApp() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isActionView()) {
                    checkSiteAvailable();
                    //startActivity(new Intent(ActivitySplash.this, ActivityTabMain.class));
                } else {
                    if (isValidDetailsLink()) {
                        startActivityDetails();
                        finish();
                    } else {
                        showNotValidUrl();
                    }
                }
            }
        }, getResources().getInteger(R.integer.activity_splash_time));
    }

    private void showNotValidUrl() {
        log.debug(getString(R.string.msg_not_valid_details_url));
        Utils.emptyMessage(this, getString(R.string.msg_not_valid_details_url), true);
    }

    private void startActivityDetails() {
        Intent myIntent = new Intent(ActivitySplash.this, ActivityDetails.class);
        myIntent.putExtra(Utils.ARG_FILM_URL, mDetailsUrl);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(myIntent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    protected void startArchiveActivity(int tab) {
        Intent myIntent = new Intent(this, ActivityTabArchive.class);
        myIntent.putExtra(ActivityTabArchive.EXTRA_TAB_POSITION, tab);
        startActivityForResult(myIntent, ActivityTabMain.REQUEST_ARCHIVE);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void checkSiteAvailable() {
        if (Utils.isInternetAvailable(this)) {
            final String webSite = "http://" + getString(R.string.host_www_name);
            HTMLParser.isSiteAvailable(webSite, new HTMLParser.LoadListener() {
                @Override
                public void OnLoadComplete(final Object result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result instanceof String) {
                                showInformationDialog((String) result);
                            } else {
                                if ((boolean) result) {
                                    startActivity(new Intent(ActivitySplash.this, ActivityTabMain.class));
                                    finish();
                                } else {
                                    showErrorMessage(getString(R.string.msg_connection_failed));
                                }
                            }
                        }
                    });
                }

                @Override
                public void OnLoadError(final Exception ex) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showErrorMessage(ex.getMessage());
                        }
                    });
                }
            });
        } else {
            showErrorMessage(getString(R.string.msg_connection_failed));
        }
    }

    private void showInformationDialog(String msg) {
        mDialog = Utils.showMessage(R.string.dialog_header_information, msg,
                R.string.dialog_btn_ok, -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int btnId) {
                        startArchiveActivity(ActivityTabArchive.FRAGMENT_DOWNLOADS);
                        finish();
                    }
                }
        );
        mDialog.show();
    }

    private void showErrorMessage(String msg) {
        mDialog = Utils.showMessage(R.string.dialog_header_information, msg,
                R.string.dialog_btn_later, R.string.dialog_btn_repeat,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int btnId) {
                        switch (btnId) {
                            case DialogInterface.BUTTON_POSITIVE:
                                startArchiveActivity(ActivityTabArchive.FRAGMENT_DOWNLOADS);
                                finish();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                checkSiteAvailable();
                                break;
                        }
                    }
                }
        );
        mDialog.show();
    }

    @Override
    protected void onDestroy() {
        if (mDialog != null) mDialog.dismiss();
        super.onDestroy();
    }

    private void splashAnimation() {
        ObjectAnimator scaleXAnimation = ObjectAnimator.ofFloat(mLogo, "scaleX", 5.0F, 1.0F);
        scaleXAnimation.addListener(mAnimationListener);
        scaleXAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleXAnimation.setDuration(1200);
        ObjectAnimator scaleYAnimation = ObjectAnimator.ofFloat(mLogo, "scaleY", 5.0F, 1.0F);
        scaleYAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleYAnimation.setDuration(1200);
        ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(mLogo, "alpha", 0.0F, 1.0F);
        alphaAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        alphaAnimation.setDuration(1200);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(scaleXAnimation).with(scaleYAnimation).with(alphaAnimation);
        animatorSet.setStartDelay(500);
        animatorSet.start();
    }
}
