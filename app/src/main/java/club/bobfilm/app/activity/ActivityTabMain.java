package club.bobfilm.app.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import club.bobfilm.app.Application;
import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.ProgressDialog;
import club.bobfilm.app.R;
import club.bobfilm.app.ZoomOutPageTransformer;
import club.bobfilm.app.adapter.TabHeaderAdapter;
import club.bobfilm.app.entity.Section;
import club.bobfilm.app.fragment.FragmentVideo;
import club.bobfilm.app.helpers.BobFilmParser;
import club.bobfilm.app.util.Utils;

public class ActivityTabMain extends BaseTabActivity {

    public static final String EXTRA_FILM_DETAILS = "extra_film_details";
    public static final int REQUEST_SETTINGS = 100;
    public static final int REQUEST_FILMS_DETAILS = 101;
    public static final int REQUEST_ARCHIVE = 102;
    private Logger log = LoggerFactory.getLogger(ActivityTabMain.class);
    private List<Section> mSections;
    private TabLayout mTabs;
    private TabHeaderAdapter mTabHeaderAdapter;
    private ViewPager mPager;
    private ProgressDialog mProgressDialog;
    private ViewFlipper mViewFlipper;
    private TextView mTvError;
    private ActionBar mToolbar;

    @Override
    protected void onDestroy() {
        stopDialog();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.setCurrentActivity(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra("Exit me", false)) {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }
        mViewFlipper = (ViewFlipper) findViewById(R.id.vf_layout_changer);
        mTvError = (TextView) findViewById(R.id.tv_error);
        //setAppSettings();

        if (!BuildConfig.DEBUG) {
            if (!isAppPurchase()) {
                return;
            }
        }

        mToolbar = getSupportActionBar();
        mTabs = (TabLayout) findViewById(R.id.sliding_tabs);
        if (savedInstanceState == null) {
            setData();
        } else {
            log.info("not null");
            //noinspection unchecked
            mSections = (List<Section>) savedInstanceState.getSerializable("sections");
            if (mSections != null && mSections.size() > 0) {
                setAdapter();
            } else {
                setData();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("sections", (Serializable) mSections);
        super.onSaveInstanceState(outState);
    }

    private void setData() {
        startDialog();
        if (mSections == null || mSections.size() == 0) {
            log.info("OnCreate: getDataFromNetwork");
            getSectionsList();
        } else {
            log.info("OnCreate: setAdapter");
            setAdapter();
        }
    }

    private void startDialog() {
        try {
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(this, getString(R.string.msg_pb_dialog));
            }
            mProgressDialog.show();
        } catch (Exception ex) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), ex);
            }
        }
    }

    private void stopDialog() {
        try {
            if (mProgressDialog != null) {
                mProgressDialog.cancel();
            }
        } catch (Exception ex) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), ex);
            }
        }
    }

    private void getSectionsList() {
        BobFilmParser.getParsedSite(BobFilmParser.mSite,
                BobFilmParser.ACTION_SECTIONS,
                null, new BobFilmParser.LoadListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void OnLoadComplete(final Object result) {
                        mSections = new ArrayList<>((List<Section>) result);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setAdapter();
                            }
                        });
                    }

                    @Override
                    public void OnLoadError(final Exception ex) {
                        if (!ex.getMessage()
                                .equalsIgnoreCase(getString(R.string.msg_connection_failed))) {
                            if (BuildConfig.DEBUG) {
                                ex.printStackTrace();
                            } else {
                                log.error(Utils.getErrorLogHeader() + new Object() {
                                }.getClass().getEnclosingMethod().getName(), ex);
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                stopDialog();
                                changeViewForError(ex.getMessage());
                                //showErrorMessage();
                            }
                        });
                    }

//            @Override
//            public void OnConnectionProblem(Object message) {
//                log.debug("{}", (String) message);
//                stopDialog();
//                changeViewForError((String) message);
//                showErrorMessage();
//            }
                });
    }

    private void changeViewForError(String msg) {
        //mTvError.setText(msg);
        //mTvError.setTextColor(ContextCompat.getColor(this, R.color.text_color_error));
        mViewFlipper.setDisplayedChild(1);
    }

    private void showErrorMessage() {
        Utils.showMessage(R.string.dialog_header_information, getString(R.string.msg_connection_failed),
                R.string.dialog_btn_later, R.string.dialog_btn_repeat,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int btnId) {
                        switch (btnId) {
                            case DialogInterface.BUTTON_POSITIVE:
                                startArchiveActivity(ActivityTabArchive.FRAGMENT_DOWNLOADS);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                setData();
                                break;
                        }
                    }
                }
        ).show();
    }

    private void setAdapter() {
        try {
            if (mSections.size() > 0) {
                mViewFlipper.setDisplayedChild(0);
                fragmentViewPagerInitial();
            } else {
                stopDialog();
                changeViewForError("Сервер не вернул данные");
                //showErrorMessage();
            }
        } catch (Exception ex) {
            stopDialog();
            changeViewForError(ex.getMessage());
            //showErrorMessage();
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), ex);
            }
        }
    }

    private void fragmentViewPagerInitial() {
        // Get the ViewPager and set it's TabHeaderAdapter so that it can display items
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setPageTransformer(true, new ZoomOutPageTransformer());
        mTabHeaderAdapter = new TabHeaderAdapter(getSupportFragmentManager(), mSections, ActivityTabMain.this);
        if (mPager != null) {
            //page download count
            mPager.setOffscreenPageLimit(1);
            mPager.setAdapter(mTabHeaderAdapter);
            //todo tab sport with articles
//            mPager.setCurrentItem(13);
            mPager.setCurrentItem(super.getPagerPosition());
        }

        // Give the TabLayout the ViewPager
        if (mTabs != null) {
            mTabs.setVisibility(View.VISIBLE);
            mTabs.setupWithViewPager(mPager);
            setTabsSelectable();
            stopDialog();
        }
    }

//    public void setupTabLayout(TabLayout tabLayout) {
//        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
//        tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
//        tabLayout.setupWithViewPager(mViewpager);
//
//        TextView tab = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
//        tab.setText("Library");
//        tab.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_tabbar_library, 0, 0);
//        tabLayout.getTabAt(0).setCustomView(tab);
//        //..
//    }

    private void setTabsSelectable() {
        ViewGroup tabs = (ViewGroup) mTabs.getChildAt(0);
        View tab;
        for (int i = 0; i < tabs.getChildCount(); i++) {
            tab = tabs.getChildAt(i);
            tab.setClickable(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                tab.setBackground(ContextCompat.getDrawable(ActivityTabMain.this, android.R.attr.selectableItemBackground));
            }
        }
    }

    @Override
    public void onClick(View v) {
        //Toast.makeText(ActivityTabMain.this, "test_click", Toast.LENGTH_SHORT).show();
        switch (v.getId()) {
            case R.id.tv_error_repeat:
                setData();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_search:
                //todo search engine
//                if (BuildConfig.DEBUG) {
                startActivity(TestActivitySearchResult.newInstanceSearch(this,
                        mSections.get(mPager.getCurrentItem())));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
//                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_FILMS_DETAILS:
//                Film film = (Film) data.getSerializableExtra(EXTRA_FILM_DETAILS);
                try {
                    updateFragment();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    log.error(Utils.getErrorLogHeader() + new Object() {
                    }.getClass().getEnclosingMethod().getName(), ex);
                }
                break;
            case REQUEST_ARCHIVE:
                try {
                    updateFragment();
                } catch (Exception ex) {
                    if (BuildConfig.DEBUG) {
                        ex.printStackTrace();
                    } else {
                        log.error(Utils.getErrorLogHeader() + new Object() {
                        }.getClass().getEnclosingMethod().getName(), ex);
                    }
                }
                break;
            case REQUEST_SETTINGS:
                setAppSettings();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        stopDialog();
        if (isTaskRoot()) {
            super.onBackPressed();
        } else {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

//    private void setAppSettings() {
//        Utils.setAppSettings(this);
//        HTMLParser.setContext(this);
//        //Utils.checkForUpdate(this);
//    }

    private boolean isAppPurchase() {
//        if (Utils.isAppBad(this)) {
//            super.trialIsOver();
//            return false;
//        }
        return true;
    }

    private void updateFragment() {
        if (mPager != null && mTabHeaderAdapter != null) {
            ((FragmentVideo) mTabHeaderAdapter.instantiateItem(mPager,
                    mPager.getCurrentItem())).getUpdatedBookmark();
        }
    }
}


