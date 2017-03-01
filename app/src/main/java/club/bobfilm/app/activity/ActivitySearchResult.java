package club.bobfilm.app.activity;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import club.bobfilm.app.Application;
import club.bobfilm.app.R;
import club.bobfilm.app.adapter.RVListSearchResultAdapter;
import club.bobfilm.app.entity.Film;
import club.bobfilm.app.entity.Section;
import club.bobfilm.app.helpers.BobFilmParser;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 24.04.2016.
 */
public class ActivitySearchResult extends BaseActivity implements View.OnClickListener {
    public static final String EXTRA_SEARCH_QUERY = "extra_search_query";
    private Logger log = LoggerFactory.getLogger(ActivitySearchResult.class);

    private RecyclerView mRecyclerView;
    private List<Film> mSearchResults = new ArrayList<>();

    private ViewFlipper mViewFlipper;
    private TextView mTVerror;

    private String mSearchRequest;

    private RVListSearchResultAdapter.OnItemClickListener
            mItemClickListener = new RVListSearchResultAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
//            log.info("ROW PRESSED = " + String.valueOf(position));
            Film film = mSearchResults.get(position);
            mCategory.setNextPageUrl(film.getNextPageUrl());
            checkDetails(film, mCategory, null);
        }

        @Override
        public void onItemLongClick(View v, int position) {
//            log.info("ROW LONG PRESSED = " + String.valueOf(position));
        }
    };
    private Section mCategory;
    private String mToolbarTitle;
    private SwipeRefreshLayout mSwipeRefreshLayout;

//    private void checkDetails(final Film film) {
//        if (!film.isHasArticle()) {
//            startActivityDetails(film);
//        } else {
//            //noinspection unchecked
////            getListOfSubCategories(film);
//            startActivitySubCategories(film);
//        }
//    }

//    private void startActivityDetails(Film film) {
//        Intent myIntent = new Intent(ActivitySearchResult.this, ActivityDetails.class);
//        myIntent.putExtra(Utils.ARG_FILM_DETAILS, film);
//        startActivity(myIntent);
//    }
//
//    private void startActivitySubCategories(Film film) {
//        Intent myIntent = new Intent(this, ActivitySubCategories.class);
//        myIntent.putExtra(Utils.ARG_SUB_CATEGORIES, film);
//        myIntent.putExtra(Utils.ARG_SERIALIZABLE_SECTION, new Section(film.getNextPageUrl()));
////        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
//        startActivity(myIntent);
//    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.setCurrentActivity(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_parent_list);

        getData();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (mToolbarTitle != null && !mToolbarTitle.equalsIgnoreCase("")) {
                getSupportActionBar().setTitle(mToolbarTitle);
            } else {
                getSupportActionBar().setTitle(R.string.activity_search_results_title);
            }
        }

        mViewFlipper = (ViewFlipper) findViewById(R.id.vf_layout_changer);
        mTVerror = (TextView) findViewById(R.id.tv_error);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);

        setSwipeToRefresh();
        mSwipeRefreshLayout.setRefreshing(true);

    }

    private void setSwipeToRefresh() {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setColorSchemeResources(
                    R.color.c_swipe_refresh_blue,
                    R.color.c_swipe_refresh_orange,
                    R.color.c_swipe_refresh_green,
                    R.color.c_swipe_refresh_red);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    try {
//                        mSearchResults.clear();
                        mRecyclerView.getAdapter().notifyItemRangeRemoved(0, mSearchResults.size()-1);
                        getSearchDataFromNetwork();
                    } catch (Exception ex){
                        changeViewForError(ex.getMessage());
                    }
                }
            });
        }
    }

    private void getData() {
        if (getIntent() != null) {
            mCategory = (Section) getIntent().getSerializableExtra(Utils.ARG_SERIALIZABLE_SECTION);
            mSearchRequest = getIntent().getExtras().getString(Utils.ARG_SEARCH_RESULTS);
            mToolbarTitle = getIntent().getExtras().getString(EXTRA_SEARCH_QUERY);
        }
        try {
            getSearchDataFromNetwork();
        } catch (Exception ex){
            changeViewForError(ex.getMessage());
        }
    }

    private void getSearchDataFromNetwork() {
        if (mSearchRequest != null && !mSearchRequest.equalsIgnoreCase("")) {
            String searchUrl = BobFilmParser.mSite + mSearchRequest;
            BobFilmParser.getParsedSite(searchUrl, BobFilmParser.ACTION_SEARCH, null, new BobFilmParser.LoadListener() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public void OnLoadComplete(Object result) {
                            mSearchResults = new ArrayList<>((List<Film>) result);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mSearchResults.size() > 0) {
                                        setData();
                                    } else {
                                        changeViewForError(getResources().getString(R.string.msg_nothing_found));
                                    }
                                }
                            });
                        }

                        @Override
                        public void OnLoadError(final Exception ex) {
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
                    }
            );
        }

    }

    private void changeViewForError(String msg) {
        mTVerror.setText(msg);
        mTVerror.setTextColor(ContextCompat.getColor(ActivitySearchResult.this, R.color.text_color_error));
        mViewFlipper.setDisplayedChild(1);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void setData() {
        RVListSearchResultAdapter mAdapter = new RVListSearchResultAdapter(ActivitySearchResult.this, mSearchResults, mItemClickListener);
        mRecyclerView.setAdapter(mAdapter);
        mViewFlipper.setDisplayedChild(2);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.tv_error_repeat:
                mSwipeRefreshLayout.setRefreshing(true);
                getData();
                break;
        }
    }
}

