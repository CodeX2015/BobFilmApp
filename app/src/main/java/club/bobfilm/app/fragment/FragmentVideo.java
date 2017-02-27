package club.bobfilm.app.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import club.bobfilm.app.AutoFitRecyclerView;
import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.activity.ActivityComments;
import club.bobfilm.app.activity.ActivityDetails;
import club.bobfilm.app.activity.ActivitySearchResult;
import club.bobfilm.app.activity.ActivitySubCategories;
import club.bobfilm.app.activity.ActivityTabMain;
import club.bobfilm.app.adapter.RVGridAdapter;
import club.bobfilm.app.adapter.SearchViewArrayAdapter;
import club.bobfilm.app.entity.Film;
import club.bobfilm.app.entity.Section;
import club.bobfilm.app.helpers.DBHelper;
import club.bobfilm.app.helpers.HTMLParser;
import club.bobfilm.app.util.Utils;

/**
 * A fragment representing a grid of Items.
 */
public class FragmentVideo extends Fragment
        implements SearchView.OnQueryTextListener,
        MenuItemCompat.OnActionExpandListener,
        AdapterView.OnItemClickListener {

    private static String LOG_TAG = "FragmentVideo";
    private static Logger log = LoggerFactory.getLogger(FragmentVideo.class);
    private ViewFlipper mViewFlipper;
    private TextView mTVerror;
    private String mFilmsURL;
    private List<Film> mFilms = new ArrayList<>();
    private View mView;
    private Context mContext;
    private RVGridAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Parcelable recyclerViewState;
    private List<Film> mDataFromDB;
    private Section mCategory;
    private Film mFilm;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private int mPrevItemCount;
    private ArrayAdapter<String> mSearchAdapter;
    private MenuItem searchMenuItem;
    //    private SearchView mSearchView;
    private SearchViewArrayAdapter mSearchView;

    private boolean loading = true;
    private int pastVisibleItems;
    private int visibleItemCount;
    private int totalItemCount;


    //Listener for GridItem buttons
    private RVGridAdapter.OnItemClickListener mItemClickListener = new RVGridAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
            try {
                mFilm = mFilms.get(position);
                switch (v.getId()) {
                    case R.id.ll_quote:
                        //click quote button
                        isCommentsExists(position);
                        break;
                    case R.id.iv_grid_more:
                        //click 3 dots button
                        showPopup(v, position);
                        break;
                    default:
                        //click GridItem
                        checkDetails(mFilms.get(position));
                        break;
                }
            } catch (Exception ex) {
                log.error("RVGridAdapter.OnItemClick: ", ex);
            }
        }

        @Override
        public void onItemLongClick(View v, int position) {
            //Toast.makeText(mContext, "Long click", Toast.LENGTH_SHORT).show();
        }
    };


    //check if item has articles
    private void checkDetails(final Film film) {
        if (!film.isHasArticle()) {
            startActivityDetails(film);
        } else {
            //noinspection unchecked
            startActivitySubCategories(film);
        }
    }

    private void startActivityDetails(Film film) {
        Intent myIntent = new Intent(mContext, ActivityDetails.class);
//        Intent myIntent = new Intent(mContext, TestActivity.class);
        myIntent.putExtra(Utils.ARG_FILM_DETAILS, film);
        ((AppCompatActivity) mContext).startActivityForResult(myIntent, ActivityTabMain.REQUEST_FILMS_DETAILS);
        ((AppCompatActivity) mContext).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void startActivitySubCategories(Film film) {
        Intent myIntent = new Intent(mContext, ActivitySubCategories.class);
        myIntent.putExtra(Utils.ARG_SUB_CATEGORIES, film);
        myIntent.putExtra(Utils.ARG_SERIALIZABLE_SECTION, mCategory);
//        myIntent.putExtra(Utils.ARG_NEXT_PAGE_URL, mCategory.getNextPageUrl());
//        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        mContext.startActivity(myIntent);
        ((AppCompatActivity) mContext).overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void isCommentsExists(int pos) {
        Film film = mFilms.get(pos);
        if (!film.getReviews().equalsIgnoreCase(mContext.getResources().getString(R.string.no_reviews))) {
            startActivityComments(film.getReviewsUrl());
        } else {
            Toast.makeText(mContext, R.string.msg_no_reviews, Toast.LENGTH_SHORT).show();
        }
    }

    private void startActivityComments(String commentsUrl) {
        Intent myIntent = new Intent(mContext, ActivityComments.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        myIntent.putExtra(Utils.ARG_COMMENTS_URL, commentsUrl);
        startActivity(myIntent);
        ((AppCompatActivity) mContext).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showPopup(View v, final int position) {
        PopupMenu popup = new PopupMenu(mContext, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.popup_menu_grid_more, popup.getMenu());
        updateMenuTitles(popup.getMenu());
        popup.show();
        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(
                //Listener popup menu 3dots button
                new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.popup_share:
                                String shareString = mFilm.getFilmTitle() + "\n\n" +
                                        HTMLParser.SITE + mFilm.getFilmUrl();
                                log.warn("share:\n{}", shareString);
                                Utils.shareTextUrl(mContext, getString(R.string.action_send_to), shareString);
                                return true;
                            case R.id.popup_add_bookmark:
                                setBookmarkForCard(position);
                                return true;
//                            case R.id.popup_test_download:
//                                Utils.testDownloading(2, mContext);
//                                log.warn("testDownloading click");
//                                return false;
//                            case R.id.popup_add_history:
////                    DBHelper.getInstance(mContext).addHistoryFile(new FilmFile("test_title",
////                            "test_fileUrl", mFilm));
////                                FilmFile file = new FilmFile("test_title", "test_fileUrl", mFilm);
////                                DBHelper.getInstance(mContext).dbWorker(DBHelper.ACTION_ADD,
//                                          DBHelper.FN_HISTORY, file, null);
//                                return false;
                            default:
                                return false;
                        }
                    }
                }
        );

    }

    private void updateMenuTitles(Menu menu) {
        MenuItem popupBookmark = menu.findItem(R.id.popup_add_bookmark);
        if (mFilm.isBookmarked()) {
            popupBookmark.setTitle(mContext.getString(R.string.popup_remove_bookmark));
        } else {
            popupBookmark.setTitle(mContext.getString(R.string.popup_add_bookmark));
        }
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FragmentVideo() {
    }

    public static FragmentVideo newInstance(Section category) {
        FragmentVideo fragment = new FragmentVideo();
        Bundle args = new Bundle();
        args.putSerializable(Utils.ARG_SERIALIZABLE_SECTION, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCategory = (Section) getArguments().getSerializable(Utils.ARG_SERIALIZABLE_SECTION);
            if (mCategory != null) {
                mFilmsURL = HTMLParser.SITE + mCategory.getSectionUrl();
            }
        }

        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_video_grid, container, false);
        mViewFlipper = (ViewFlipper) mView.findViewById(R.id.vf_fragment);
        mTVerror = (TextView) mView.findViewById(R.id.tv_error);
        mSwipeRefreshLayout = (SwipeRefreshLayout) mView.findViewById(R.id.swipeRefreshLayout);
        setSwipeToRefresh();
        if (savedInstanceState == null || mFilms.size() == 0) {
            getListOfFilms();
        } else {
            resetAdapter();
            setData();
        }
        return mView;
    }

    private void resetAdapter() {
        if (mAdapter != null) mAdapter = null;
        if (mRecyclerView != null) mRecyclerView.setAdapter(null);
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
                    //mRecyclerView.stopScroll();
                    //mRecyclerView.getRecycledViewPool().clear();
                    //mRecyclerView.getAdapter().notifyItemRangeRemoved(0, mFilms.size() - 1);
                    reloadAdapter();

                    // Stop refresh animation
                    //mSwipeRefreshLayout.setRefreshing(false);

                    //If at any point, you want to disable pull to refresh gestures and progress animations, call setEnabled(false) on the view.
                    //mSwipeRefreshLayout.setEnabled(false);
                }
            });
        }
    }

    private void reloadAdapter() {
        mViewFlipper.setDisplayedChild(0);
        if (mFilms != null) {
            mFilms.clear();
        }
        if (mAdapter != null) {
            //mAdapter.notifyItemRangeRemoved(0, mAdapter.getItemCount() - 1);
            mAdapter.notifyDataSetChanged();
            mAdapter = null;
        }
        getListOfFilms();
    }

    private void getListOfFilms() {
        String url = mFilmsURL + "&per=" + HTMLParser.FILMS_COUNT_PER_PAGE;
        HTMLParser.getParsedSite(url, HTMLParser.ACTION_FILMS, mCategory, new HTMLParser.LoadListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void OnLoadComplete(final Object result) {
                List<Film> parseResult = new ArrayList<>((List<Film>) result);
                if (parseResult.size() > 0) {
                    mPrevItemCount = mFilms.size();
                    mFilms.addAll(parseResult);
                    log.info("mFilms size is: {}, loading {}", mFilms.size(), loading);
                }
                getDataFromDB();
            }

            @Override
            public void OnLoadError(final Exception ex) {
                if (!ex.getMessage().equalsIgnoreCase(getString(R.string.msg_connection_failed))) {
                    if (BuildConfig.DEBUG) {
                        ex.printStackTrace();
                    } else {
                        log.error(Utils.getErrorLogHeader() + new Object() {
                        }.getClass().getEnclosingMethod().getName(), ex);
                    }
                }
                ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        changeViewForError(ex.getMessage());
                    }
                });
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        mContext = context;
        super.onAttach(context);
    }

    private void getDataFromDB() {
        DBHelper.getInstance(mContext).dbWorker(
                DBHelper.ACTION_GET,
                DBHelper.FN_BOOKMARKS,
                null,
                new DBHelper.OnDBOperationListener() {
                    @Override
                    public void onSuccess(Object result) {
                        //noinspection unchecked
                        mDataFromDB = (List<Film>) result;
                        ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                changeBookmarks();
                                setData();
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception ex) {
                        if (BuildConfig.DEBUG) {
                            ex.printStackTrace();
                        } else {
                            log.error(Utils.getErrorLogHeader() + new Object() {
                            }.getClass().getEnclosingMethod().getName(), ex);
                        }
                        ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                changeViewForError(ex.toString());
                            }
                        });
                    }
                }
        );
    }

    private void changeViewForError(String msg) {
        mTVerror.setText(msg);
        mTVerror.setTextColor(ContextCompat.getColor(mContext, R.color.text_color_error));
        mViewFlipper.setDisplayedChild(1);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void changeBookmarks() {
        for (Film film : mFilms) {
            film.setBookmarked(false);
            for (Film bookmark : mDataFromDB) {
                if (film.getFilmUrl().equalsIgnoreCase(bookmark.getFilmUrl())) {
                    film.setBookmarked(!film.isBookmarked());
                }
            }
        }
    }

    public void getUpdatedBookmark() {
        resetAdapter();
        getDataFromDB();
    }

    private void setData() {
        ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mAdapter != null && mRecyclerView.getAdapter() != null) {
                        log.info("refreshAdapter");
                        refreshAdapter();
                    } else {
                        log.info("setAdapter");
                        setAdapter();
                    }
                    mSwipeRefreshLayout.setRefreshing(false);
                    mViewFlipper.setDisplayedChild(2);
                } catch (Exception ex) {
                    if (BuildConfig.DEBUG) {
                        ex.printStackTrace();
                    } else {
                        log.error(Utils.getErrorLogHeader() + new Object() {
                        }.getClass().getEnclosingMethod().getName(), ex);
                    }
                    changeViewForError(ex.toString());
                }
            }
        });
    }

    private void refreshAdapter() {
//        mRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        mAdapter.notifyItemRangeInserted(mPrevItemCount, mFilms.size() - 1);
        log.info("insert range ({}-{})", mPrevItemCount, mFilms.size() - 1);
        loading = true;
        recyclerViewState = mRecyclerView.getLayoutManager().onSaveInstanceState();//save
    }

    private void setAdapter() {
        recyclerViewState = null;
        if (mFilms != null && mFilms.size() > 0) {
            mRecyclerView = (AutoFitRecyclerView) mView.findViewById(R.id.grid);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 1));
            mAdapter = new
                    RVGridAdapter(mContext, mFilms, mItemClickListener);
            mRecyclerView.setAdapter(mAdapter);
            //save
            if (mRecyclerView.getLayoutManager() != null) {
                recyclerViewState = mRecyclerView.getLayoutManager().onSaveInstanceState();
            } else {
                log.warn("RecyclerView layout manager is null");
            }
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    //super.onScrolled(mRecyclerView, dx, dy);
                    if (dy > 0) //check for scroll down
                    {
                        visibleItemCount = recyclerView.getLayoutManager().getChildCount();
                        totalItemCount = recyclerView.getLayoutManager().getItemCount();
                        pastVisibleItems = ((GridLayoutManager)
                                recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                        //log.info("ScrollDown: {}, {}, {}",
                        // visibleItemCount[0],pastVisibleItems[0], totalItemCount[0]);
                        if (loading) {
                            //log.info("loading true");
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                loading = false;
                                //log.info("Last Item Wow! {}, {}, {}",
                                // visibleItemCount[0], pastVisibleItems[0], totalItemCount[0]);
                                if (!mCategory.getNextPageUrl().equalsIgnoreCase("")) {
                                    mFilmsURL = HTMLParser.SITE + mCategory.getNextPageUrl();
                                    log.info("Loading: {}", mFilmsURL);
                                    mSwipeRefreshLayout.setRefreshing(true);
                                    getListOfFilms();
                                }
                            }
                        }
                    }
                }
            });
        } else {
            mViewFlipper.setDisplayedChild(0);
            getListOfFilms();
        }
    }

    private void setBookmarkForCard(int position) {
        log.warn("set to bookmark:\n{}, {}", mFilm.getFilmTitle(), mFilm.isBookmarked());
        RVGridAdapter.ViewHolder holder = getViewHolder(position);
        if (mFilm.isBookmarked()) {
            //remove bookmark
            holder.mBookmarkView.setVisibility(View.INVISIBLE);
            DBHelper.getInstance(mContext).dbWorker(DBHelper.ACTION_DELETE, DBHelper.FN_BOOKMARKS, mFilm, null);
        } else {
            //add bookmark
            holder.mBookmarkView.setVisibility(View.VISIBLE);
            DBHelper.getInstance(mContext).dbWorker(DBHelper.ACTION_ADD, DBHelper.FN_BOOKMARKS, mFilm, null);

        }
        mFilm.setBookmarked(!mFilm.isBookmarked());
    }

    private RVGridAdapter.ViewHolder getViewHolder(int position) {
        return (RVGridAdapter.ViewHolder) mRecyclerView.findViewHolderForLayoutPosition(position);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        /*searchMenuItem = menu.findItem(R.id.action_search);
//        log.info("onCreateOptionsMenu: create mSearchView");
        mSearchView = (SearchViewArrayAdapter) MenuItemCompat.getActionView(searchMenuItem);
        // id of AutoCompleteTextView
        int searchEditTextId = R.id.search_src_text; // for AppCompat

        mSearchView.setQueryHint(getString(R.string.search_hint, mCategory.getSectionTitle()));
        mSearchView.setDropDownAnchor(R.id.toolbar);
        mSearchView.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        mSearchView.setDropDownBackgroundResource(
                ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.rnd_corners_white_bg_white, null));

//        mSearchView = (SearchView) searchMenuItem.getActionView();
//        mSearchView.setQueryHint(mContext.getResources().getString(R.string.search_hint));
        mSearchView.setOnItemClickListener(this);

        mSearchView.setOnQueryTextListener(this);

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, this);*/
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSearchView.setText(mSearchAdapter.getItem(position));
        startActivitySearch(mSearchAdapter.getItem(position));
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
        log.info("onQueryTextSubmit: {}", query);
        startActivitySearch(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        log.info("onQueryTextChange: {}", newText);
        if (newText.length() >= 3) {
            getSearchHints(newText);
            return true;
        }
        return false;
    }

    private void startActivitySearch(String searchText) {
        String searchUrl;
        if (searchText.equalsIgnoreCase("")) {
            searchUrl = "/search?" + mCategory.getSearchId() + "&s=Безумие+Олмейера";
        } else {
            searchUrl = "/search?" + mCategory.getSearchId() + "&s=" + Utils.encodeToUtf(searchText);
        }
        Intent myIntent = new Intent(mContext, ActivitySearchResult.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        myIntent.putExtra(Utils.ARG_SEARCH_RESULTS, searchUrl);
        myIntent.putExtra(Utils.ARG_SERIALIZABLE_SECTION, mCategory);
        myIntent.putExtra(ActivitySearchResult.EXTRA_SEARCH_QUERY, searchText);
        startActivity(myIntent);
        ((AppCompatActivity) mContext).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
                    mSearchAdapter = new ArrayAdapter<>(mContext,
                            R.layout.item_spinner_dropdown, listHints);
                }
                log.debug("AdapterCreated");
                ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        if (mSearchAdapter.getCount() > 0) {
                        mSearchView.setAdapter(mSearchAdapter);
//                        }
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
                ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        changeViewForError(ex.getMessage());
                    }
                });
            }
        });
    }
}