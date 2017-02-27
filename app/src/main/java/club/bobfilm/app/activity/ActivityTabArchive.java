package club.bobfilm.app.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import club.bobfilm.app.Application;
import club.bobfilm.app.R;
import club.bobfilm.app.ZoomOutPageTransformer;
import club.bobfilm.app.fragment.FragmentBookmarks;
import club.bobfilm.app.fragment.FragmentDownloads;
import club.bobfilm.app.fragment.FragmentHistory;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 18.05.2016.
 */
public class ActivityTabArchive extends BaseTabActivity {
    public static final int FRAGMENT_DOWNLOADS = 0;
    public static final int FRAGMENT_BOOKMARKS = 1;
    public static final int FRAGMENT_HISTORY = 2;
    private Logger log = LoggerFactory.getLogger(ActivityTabArchive.class);
    private List<String> mSecondarySections;
    private ActionBar mToolbar;
    private int mPagerPosition = 0;
    private ViewPager mViewPager;
    private ArchivePagerAdapter mPagerAdapter;

    @Override
    public void onResume() {
        super.onResume();
        Application.setCurrentActivity(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mToolbar = getSupportActionBar();
//        if (mToolbar != null) {
//            mToolbar.setDisplayHomeAsUpEnabled(true);
//        }
        //invalidateOptionsMenu();

        mPagerPosition = super.getPagerPosition();
        fragmentViewPagerInitial();
    }

    CharSequence generateSpannableString() {
        int[] imageResIds = {
                R.mipmap.ic_launcher
        };
        // Generate title based on item position
        Drawable image = ContextCompat.getDrawable(ActivityTabArchive.this, imageResIds[0]);
        image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
        SpannableString sb = new SpannableString("   " + mSecondarySections.get(0));
        ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    @Override
    public void onBackPressed() {
        createResult();
        if (isTaskRoot()) {
            //unregisterDownloadReceiver();
            super.onBackPressed();
        } else {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    private void unregisterDownloadReceiver() {
        if (mViewPager != null && mViewPager.getAdapter() != null) {
            //todo debug
            FragmentDownloads fragmentDownloads = ((FragmentDownloads) (mViewPager.getAdapter()
                    .instantiateItem(mViewPager, mViewPager.getCurrentItem())));
            fragmentDownloads.unregisterReceiver();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.action_search);
            if (item != null) {
                item.setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_archive) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void createResult() {
        setResult(RESULT_OK, new Intent(this, ActivityTabMain.class));
    }

    private void fragmentViewPagerInitial() {
        mSecondarySections = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.tabs_header)));

        // Get the ViewPager and set it's TabHeaderAdapter so that it can display items
        mViewPager = (ViewPager) findViewById(R.id.pager);

        mPagerAdapter = new ArchivePagerAdapter(
                getSupportFragmentManager(), mSecondarySections);
        if (mViewPager != null) {
            mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
            mViewPager.setOffscreenPageLimit(3);
            mViewPager.setAdapter(mPagerAdapter);
            mViewPager.setCurrentItem(mPagerPosition);
        }

        // Give the TabLayout the ViewPager
        TabLayout tabs = (TabLayout) findViewById(R.id.sliding_tabs);
        if (tabs != null) {
            tabs.setVisibility(View.VISIBLE);
            tabs.setTabGravity(TabLayout.GRAVITY_FILL);
            tabs.setTabMode(TabLayout.MODE_FIXED);

        }
        if (tabs != null) {
//            tabs.setViewPager(mViewPager);

            tabs.setupWithViewPager(mViewPager);
            tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (mViewPager != null) {
                        mViewPager.setCurrentItem(tab.getPosition());
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
        }
    }

    class ArchivePagerAdapter extends FragmentStatePagerAdapter {
        List<String> mSecondarySections;

        public ArchivePagerAdapter(FragmentManager fm, List<String> sections) {
            super(fm);
            this.mSecondarySections = sections;
        }

        @Override
        public int getCount() {
            return mSecondarySections.size();
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = null;
            switch (i) {
                case FRAGMENT_DOWNLOADS:
                    fragment = new FragmentDownloads();
                    break;
                case FRAGMENT_BOOKMARKS:
                    fragment = new FragmentBookmarks();
                    break;
                case FRAGMENT_HISTORY:
                    fragment = new FragmentHistory();
                    break;
                default:
                    break;
            }
            if (fragment != null) {
                Bundle args = new Bundle();
                args.putString(Utils.ARG_FILMS_URL, mSecondarySections.get(i));
                fragment.setArguments(args);
            } else {
                return new Fragment();
            }
            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mSecondarySections.get(position).toUpperCase();
        }
    }
}
