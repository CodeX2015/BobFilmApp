package club.bobfilm.app.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import club.bobfilm.app.Application;
import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.adapter.RVListSearchResultAdapter;
import club.bobfilm.app.adapter.SearchViewArrayAdapter;
import club.bobfilm.app.entity.Film;
import club.bobfilm.app.entity.Section;
import club.bobfilm.app.helpers.DBHelper;
import club.bobfilm.app.helpers.HTMLParser;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 24.04.2016.
 */
public class TestActivitySearchResult extends AppCompatActivity
        implements View.OnClickListener,
        SearchView.OnQueryTextListener,
        MenuItemCompat.OnActionExpandListener,
        AdapterView.OnItemClickListener {
    public static final String EXTRA_SEARCH_QUERY = "extra_search_query";
    private Logger log = LoggerFactory.getLogger(TestActivitySearchResult.class);

    @BindView(R.id.vf_layout_changer)
    ViewFlipper mViewFlipper;
    @BindView(R.id.tv_error)
    TextView mTvError;
    @BindView(R.id.tv_error_repeat)
    TextView mBtnErrRepeat;
    @BindView(R.id.list)
    RecyclerView mRecyclerView;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private List<Film> mSearchResults = new ArrayList<>();
    private String mSearchRequest;
    private Section mCategory;
    private String mToolbarTitle;
    private SearchViewArrayAdapter mSearchView;
    private ArrayAdapter<String> mSearchAdapter;
    private AutoCompleteTextView mSearchEditText;
    private String mSearchText;
    private boolean isSearchCollapse;

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


    protected void checkDetails(final Film film, Section category, ArrayList<Film> addressList) {
        if (!film.isHasArticle()) {
            startActivityDetails(film);
        } else {
            //noinspection unchecked
//            getListOfSubCategories(film);
            startActivitySubCategories(film, category, addressList);
        }
    }

    protected void startActivityDetails(Film film) {
        Intent myIntent = new Intent(this, ActivityDetails.class);
        myIntent.putExtra(Utils.ARG_FILM_DETAILS, film);
        this.startActivityForResult(myIntent, ActivityTabMain.REQUEST_FILMS_DETAILS);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    protected void startActivitySubCategories(Film film, Section category, ArrayList<Film> addressList) {
        Intent myIntent = new Intent(this, ActivitySubCategories.class);
        myIntent.putExtra(Utils.ARG_SUB_CATEGORIES, film);
        myIntent.putExtra(Utils.ARG_SERIALIZABLE_SECTION, category);
        myIntent.putExtra(Utils.ARG_ADDRESS_LIST, addressList);

//        myIntent.putExtra(Utils.ARG_NEXT_PAGE_URL, film.getNextPageUrl());
//        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(myIntent);
//        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    static Intent newInstanceSearch(Context mContext, Section category) {
        Intent myIntent = new Intent(mContext, TestActivitySearchResult.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        myIntent.putExtra(Utils.ARG_SERIALIZABLE_SECTION, category);
        return myIntent;
    }

    String getSearchQuery(String searchText, String searchId) {
        if (searchText.equalsIgnoreCase("")) {
            return null;
        } else {
            return "/search?" + searchId + "&s=" + searchText;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Application.setCurrentActivity(this);
        setContentView(R.layout.fragment_parent_list);
        ButterKnife.bind(this);

        if (getIntent() != null) {
            mCategory = (Section) getIntent().getSerializableExtra(Utils.ARG_SERIALIZABLE_SECTION);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setSwipeToRefresh();
        mViewFlipper.setDisplayedChild(3);
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
                        mRecyclerView.getAdapter().notifyItemRangeRemoved(0,
                                mSearchResults.size() - 1);
                        getData();
                    } catch (Exception ex) {
                        changeViewForError(ex.getMessage());
                    }
                }
            });
        }
    }

    private void getData() {
        try {
            if (BuildConfig.DEBUG) {
                //getDataFromDB();
                getSearchDataFromNetwork();
            } else {
                getSearchDataFromNetwork();
            }
        } catch (Exception ex) {
            changeViewForError(ex.getMessage());
        }
    }

    private void getSearchDataFromNetwork() {
        if (mSearchRequest != null && !mSearchRequest.equalsIgnoreCase("")) {
            String searchUrl = HTMLParser.SITE + mSearchRequest;
            HTMLParser.getParsedSite(searchUrl, HTMLParser.ACTION_SEARCH,
                    null, new HTMLParser.LoadListener() {
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
                                        changeViewForError(
                                                getResources()
                                                        .getString(R.string.msg_nothing_found));
                                    }
                                }
                            });
                        }

                        @Override
                        public void OnLoadError(final Exception ex) {
                            if (!ex.getMessage().equalsIgnoreCase(
                                    getString(R.string.msg_connection_failed))) {
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

    private void getDataFromDB() {
        DBHelper.getInstance(this).dbWorker(
                DBHelper.ACTION_GET,
                DBHelper.FN_BOOKMARKS,
                null,
                new DBHelper.OnDBOperationListener() {
                    @Override
                    public void onSuccess(Object result) {
                        //noinspection unchecked
                        mSearchResults = (List<Film>) result;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mSearchResults.size() > 0) {
                                    setData();
                                } else {
                                    changeViewForError(
                                            getResources().getString(R.string.msg_nothing_found));
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception ex) {
                        if (!ex.getMessage().equalsIgnoreCase(
                                getString(R.string.msg_connection_failed))) {
                            log.error(Utils.getErrorLogHeader() + new Object() {
                            }.getClass().getEnclosingMethod().getName(), ex);
                        }
                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        changeViewForError(ex.getMessage());
                                    }
                                }
                        );
                    }
                });
    }

    private void changeViewForError(String msg) {
        mTvError.setText(msg);
        mTvError.setTextColor(ContextCompat.getColor(
                TestActivitySearchResult.this, R.color.text_color_error));
        mViewFlipper.setDisplayedChild(1);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void setData() {
        RVListSearchResultAdapter mAdapter = new RVListSearchResultAdapter(
                TestActivitySearchResult.this, mSearchResults, mItemClickListener);
        mRecyclerView.setAdapter(mAdapter);
        mViewFlipper.setDisplayedChild(2);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    void setSearchViewState(boolean collapse) {
        if (collapse) {
            mSearchView.setText("");
            isSearchCollapse = true;
            mSearchView.setIconified(true);
        } else {
            isSearchCollapse = false;
            mSearchView.setIconified(false);
        }
    }

    @Override
    public void onBackPressed() {
//        if (isSearchCollapse) {
        super.onBackPressed();
//            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
//        } else {
//            setSearchViewState(true);
//        }
    }

    @Override
    public void onClick(View view) {
        log.info("onClick");
        switch (view.getId()) {
            case R.id.tv_error_repeat:
                mSwipeRefreshLayout.setRefreshing(true);
                getData();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);

        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        setSearchView(searchMenuItem);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log.info("onOptionsItemSelected");
        switch (item.getItemId()) {
            case android.R.id.home:
//                if (isSearchCollapse) {
                onBackPressed();
//                } else {
//                    setSearchViewState(true);
//                }
                break;
//            case R.id.action_search:
//                setSearchViewState(false);
//                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setSearchView(MenuItem searchMenuItem) {
        log.info("setSearchView");
        mSearchView = (SearchViewArrayAdapter) MenuItemCompat.getActionView(searchMenuItem);

        // get AutoCompleteTextView from SearchView
        mSearchEditText = (AutoCompleteTextView)
                mSearchView.findViewById(R.id.search_src_text);
        final View dropDownAnchor = mSearchView.findViewById(mSearchEditText.getDropDownAnchor());
        if (dropDownAnchor != null) {
            dropDownAnchor.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {

                    // calculate width of DropdownView
                    int point[] = new int[2];
                    dropDownAnchor.getLocationOnScreen(point);
                    // x coordinate of DropDownView
                    int dropDownPadding = point[0] + mSearchEditText.getDropDownHorizontalOffset();

                    Rect screenSize = new Rect();
                    getWindowManager().getDefaultDisplay().getRectSize(screenSize);
                    // screen width
                    int screenWidth = screenSize.width();

                    // set DropDownView width
                    mSearchEditText.setDropDownWidth(screenWidth - dropDownPadding * 2);
                }
            });
        }

        mSearchView.setQueryHint(getString(R.string.search_hint, mCategory.getSectionTitle()));
        mSearchView.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        mSearchView.setDropDownBackgroundResource(
                ResourcesCompat.getDrawable(getResources(),
                        R.drawable.rnd_corners_white_bg_white, null));

        mSearchView.setOnItemClickListener(this);
        mSearchView.setOnQueryTextListener(this);
        setSearchViewState(false);
        MenuItemCompat.setOnActionExpandListener(searchMenuItem, this);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        log.info("onMenuItemActionExpand");
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        log.info("onMenuItemActionCollapse");
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        log.info("onQueryTextSubmit");
        startSiteSearch(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        log.info("onQueryTextChange");
        if (newText.length() >= 3) {
            getSearchHints(newText);
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        log.info("onItemClick");
        String query = mSearchAdapter.getItem(position);
        mSearchView.setText(query);
        mSearchEditText.setSelection(query != null ? query.length() : 0);
        startSiteSearch(mSearchAdapter.getItem(position));
    }

    private void startSiteSearch(String searchText) {
        mToolbarTitle = searchText;
        mSearchRequest = getSearchQuery(Utils.encodeToUtf(searchText), mCategory.getSearchId());
        getData();
    }

    public void getSearchHints(String s) {
        String url = "http://www.ex.ua/r_search_hint?" + mCategory.getSearchId() + "&s=" + Uri.encode(s);
        log.info("getSearchHints: {}", url);
        HTMLParser.getParsedSite(url, HTMLParser.ACTION_SEARCH_HINTS, s, new HTMLParser.LoadListener() {
            @Override
            public void OnLoadComplete(Object result) {
                String[] listHints = (String[]) result;
                log.info("getSearchHints: {}", listHints[0]);
                if (listHints.length > 0) {
                    mSearchAdapter = new ArrayAdapter<>(TestActivitySearchResult.this,
                            R.layout.item_spinner_dropdown, listHints);
                }
                log.debug("AdapterCreated");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSearchView.setAdapter(mSearchAdapter);
                    }
                });
                mSearchView.performClick();
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
        });
    }

}

