package club.bobfilm.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import club.bobfilm.app.Application;
import club.bobfilm.app.AutoFitRecyclerView;
import club.bobfilm.app.ProgressDialog;
import club.bobfilm.app.R;
import club.bobfilm.app.adapter.AddressLineAdapter;
import club.bobfilm.app.adapter.RVGridAdapter;
import club.bobfilm.app.entity.Film;
import club.bobfilm.app.entity.Section;
import club.bobfilm.app.helpers.DBHelper;
import club.bobfilm.app.helpers.BobFilmParser;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 05.06.2016.
 */
public class ActivitySubCategories extends BaseActivity {
    private final Logger log = LoggerFactory.getLogger(ActivitySubCategories.class);
    private List<Film> mSubCategories = new ArrayList<>();

    private boolean loading = true;
    private int pastVisibleItems;
    private int visibleItemCount;
    private int totalItemCount;

    private List<Film> mDataFromDB;
    private Film mSubCategory;
    private ProgressDialog mProgressDialog;
    private Section mCategory;
    private String mSubCategoryUrl;
    private ViewFlipper mViewFlipper;
    private TextView mTVerror;
    private RVGridAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Parcelable mRVState;

    private RVGridAdapter.OnItemClickListener mItemClickListener = new RVGridAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
            mSubCategory = mSubCategories.get(position);
            switch (v.getId()) {
                case R.id.ll_quote:
                    //Toast.makeText(getActivity(), "View comments", Toast.LENGTH_SHORT).show();
                    Film film = mSubCategories.get(position);
                    ActivitySubCategories.this.isCommentsExists(film.getReviews(), film.getReviewsUrl());
                    break;
                case R.id.iv_grid_more:
                    //Toast.makeText(getActivity(), "some action", Toast.LENGTH_SHORT).show();
                    showPopup(v, position);
                    break;
                default:
                    //Toast.makeText(getActivity(), "view details", Toast.LENGTH_SHORT).show();
                    ActivitySubCategories.this.checkDetails(mSubCategories.get(position), mCategory, mAddressList);
                    break;
            }
        }

        @Override
        public void onItemLongClick(View v, int position) {
            //Toast.makeText(getActivity(), "Long click", Toast.LENGTH_SHORT).show();
        }
    };
    private String mNextPageUrl = "";
    private int mPrevItemCount;
    private TextView mTvHeaderView;
    private TextView mTVToolbarHome;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private void showPopup(final View v, final int position) {
        log.warn("position of card is {}", position);
        PopupMenu popup = new PopupMenu(this, v);
        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.popup_share:
//                        Toast.makeText(ActivitySubCategories.this, "ic_share film", Toast.LENGTH_SHORT).show();
                        String shareString = mSubCategory.getFilmTitle() + "\n\n"
//                            + mFilm.getFilmAbout().substring(0, 20) + "..." + "\n\n"
                                + mSubCategoryUrl;
                        log.warn("share:\n{}", shareString);
                        Utils.shareTextUrl(ActivitySubCategories.this, getString(R.string.action_send_to), shareString);
                        return true;
                    case R.id.popup_add_bookmark:
                        Toast.makeText(ActivitySubCategories.this, "add film to bookmark", Toast.LENGTH_SHORT).show();
                        addBookmarkForCard(position);
                        return true;
                    default:
                        return false;
                }
            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.popup_menu_grid_more, popup.getMenu());
        popup.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.setCurrentActivity(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_categories);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (getSupportActionBar() == null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        mViewFlipper = (ViewFlipper) findViewById(R.id.vf_fragment);
        mTVerror = (TextView) findViewById(R.id.tv_error);
        mTVToolbarHome = (TextView) findViewById(R.id.tv_category_home);
        mTvHeaderView = (TextView) findViewById(R.id.tv_category_header);
        mRecyclerView = (AutoFitRecyclerView) findViewById(R.id.grid);
        mProgressDialog = new ProgressDialog(this, getResources().getString(R.string.msg_pb_dialog));
        getData();
        createHeaderAddressLine();
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        setSwipeToRefresh();
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
//                    mSubCategories.clear();
                    mRecyclerView.getAdapter().notifyItemRangeRemoved(0, mSubCategories.size() - 1);
                    getListOfSubCategories();

                    // Stop refresh animation
                    //mSwipeRefreshLayout.setRefreshing(false);

                    //If at any point, you want to disable pull to refresh gestures and progress animations, call setEnabled(false) on the view.
                    //mSwipeRefreshLayout.setEnabled(false);
                }
            });
        }
    }

    private void createHeaderAddressLine() {
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRVAddressLine = (RecyclerView) findViewById(R.id.address_line);
        if (mRVAddressLine != null) {
            mRVAddressLine.setLayoutManager(layoutManager);
            mAddressLineAdapter = new AddressLineAdapter(ActivitySubCategories.this, mAddressLineItemClickListener, mAddressList);
            mRVAddressLine.setAdapter(mAddressLineAdapter);
            mRVAddressLine.scrollToPosition(mAddressLineAdapter.getItemCount() - 1);
            mRVAddressLine.setVisibility(View.VISIBLE);
        }
    }

    private RecyclerView mRVAddressLine;
    private AddressLineAdapter mAddressLineAdapter;
    private ArrayList<Film> mAddressList;

    private AddressLineAdapter.OnItemClickListener
            mAddressLineItemClickListener = new AddressLineAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
            //Toast.makeText(ActivitySubCategories.this, "item " + position + " clicked", Toast.LENGTH_SHORT).show();
            //if (mAddressLineAdapter.getItemCount() - 1 != position) {
            startBackwardActivity(mAddressLineAdapter.getItem(position), mAddressLineAdapter.getItems());
            //}
        }
    };

    @Override
    public void onBackPressed() {
        if (mAddressLineAdapter.getItemCount() > 1) {
            mAddressLineAdapter.removeChild();
            startActivitySubCategories(mAddressLineAdapter.getItem(mAddressLineAdapter.getItemCount() - 1),
                    mCategory, mAddressLineAdapter.getItems());
        } else {
            startMainActivity();
        }
    }

    private void startBackwardActivity(Film article, ArrayList<Film> addressList) {
        startActivitySubCategories(article, mCategory, addressList);
    }

    @SuppressWarnings("unchecked")
    private void getData() {
        if (getIntent() != null) {

            mSubCategory = (Film) getIntent().getSerializableExtra(Utils.ARG_SUB_CATEGORIES);
            mCategory = (Section) getIntent().getSerializableExtra(Utils.ARG_SERIALIZABLE_SECTION);
            mAddressList = (ArrayList<Film>) getIntent().getSerializableExtra(Utils.ARG_ADDRESS_LIST);

            if (mCategory != null) {
                mTVToolbarHome.setText(mCategory.getSectionTitle());
                mTVToolbarHome.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startMainActivity();
                    }
                });
                mTvHeaderView.setText(mSubCategory.getFilmTitle());
//                mTvHeaderView.setVisibility(View.VISIBLE);

                if (mAddressList == null) {
                    mAddressList = new ArrayList<>();
                    mAddressList.add(mSubCategory);
                }
                if (mAddressList.size() > 0 &&
                        !mAddressList.get(mAddressList.size() - 1).getFilmTitle().equalsIgnoreCase(mSubCategory.getFilmTitle())) {
                    mAddressList.add(mSubCategory);
                }
            }
//            mNextPageUrl = getIntent().getStringExtra(Utils.ARG_NEXT_PAGE_URL);
            if (mSubCategory != null && mNextPageUrl != null) {
                try {
                    getListOfSubCategories();
                } catch (Exception error) {
                    log.error(Utils.getErrorLogHeader() + new Object() {
                    }.getClass().getEnclosingMethod().getName(), error);
                }
            }

        } else {
            Toast.makeText(ActivitySubCategories.this, "Nothing to show", Toast.LENGTH_SHORT).show();
        }
    }

    private void startMainActivity() {
        Intent myIntent = new Intent(this, ActivityTabMain.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NO_HISTORY);
        myIntent.putExtra("main", 0);
        startActivity(myIntent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void getListOfSubCategories() {
        mSubCategoryUrl = BobFilmParser.mSite + mSubCategory.getFilmUrl();
        BobFilmParser.loadSite(mSubCategoryUrl, BobFilmParser.ACTION_SUB_CATEGORIES, mCategory, new BobFilmParser.LoadListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void OnLoadComplete(final Object result) {
                ArrayList<Film> parseResult = new ArrayList<>((List<Film>) result);
                if (parseResult.size() > 0) {
                    mPrevItemCount = mSubCategories.size();
                    mSubCategories.addAll(parseResult);
                }
                getDataFromDB();
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

    private void setData() {
        try {
            if (mAdapter != null) {
                refreshAdapter();
            } else {
                setAdapter();
            }
        } catch (Exception error) {
            log.error(Utils.getErrorLogHeader() + new Object() {
            }.getClass().getEnclosingMethod().getName(), error);
        }
        mViewFlipper.setDisplayedChild(2);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void refreshAdapter() {
        //        mRecyclerView.getLayoutManager().onRestoreInstanceState(mRVState);
        mAdapter.notifyItemRangeChanged(mPrevItemCount, mSubCategories.size() - 1);
        log.info("add range ({}-{})", mPrevItemCount, mSubCategories.size() - 1);
//        mAdapter.notifyDataSetChanged();
        loading = true;
        mRVState = mRecyclerView.getLayoutManager().onSaveInstanceState();//save
    }

    private void setAdapter() {
        mRVState = null;
        // Set the adapter
        if (mSubCategories != null && mSubCategories.size() > 0) {
//            mRecyclerView.addItemDecoration(new MarginDecoration(this));
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));
            mAdapter = new
                    RVGridAdapter(this, mSubCategories, mItemClickListener);
            mRecyclerView.setAdapter(mAdapter);
            //save
            if (mRecyclerView.getLayoutManager() != null) {
                mRVState = mRecyclerView.getLayoutManager().onSaveInstanceState();
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
                        pastVisibleItems = ((GridLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                        log.info("Scrolldown: {}, {}, {} url: {}",
                                visibleItemCount, pastVisibleItems, totalItemCount, mCategory.getNextPageUrl());
                        if (loading) {
                            //log.info("loading true");
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                loading = false;
                                //log.info("Last Item Wow! {}, {}, {}", visibleItemCount[0], pastVisibleItems[0], totalItemCount[0]);
                                if (!mCategory.getNextPageUrl().equalsIgnoreCase("")) {
                                    mSubCategoryUrl = BobFilmParser.mSite + mCategory.getNextPageUrl();
                                    log.info("Loading: " + mSubCategoryUrl);
                                    getListOfSubCategories();
                                }
                            }
                        }
                    }
                }
            });
        } else {
            mViewFlipper.setDisplayedChild(1);
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
                        mDataFromDB = (List<Film>) result;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                changeBookmarks();
                                setData();
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception ex) {
                        ex.printStackTrace();
                        log.error(Utils.getErrorLogHeader() + new Object() {
                        }.getClass().getEnclosingMethod().getName(), ex);
                        runOnUiThread(new Runnable() {
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
        mTVerror.setTextColor(ContextCompat.getColor(this, R.color.text_color_error));
        mViewFlipper.setDisplayedChild(1);
    }

    private void changeBookmarks() {
        for (Film film : mSubCategories) {
            film.setBookmarked(false);
            for (Film bookmark : mDataFromDB) {
                if (film.getFilmUrl().equalsIgnoreCase(bookmark.getFilmUrl())) {
                    film.setBookmarked(!film.isBookmarked());
                }
            }
        }
    }

    private void addBookmarkForCard(int position) {
        log.warn("add to bookmark:\n{}", mSubCategory.getFilmTitle());
        mSubCategory.setBookmarked(true);
        DBHelper.getInstance(ActivitySubCategories.this).dbWorker(DBHelper.ACTION_ADD,
                DBHelper.FN_BOOKMARKS, mSubCategory, null);
        RVGridAdapter.ViewHolder holder = getViewHolder(position);
        holder.mBookmarkView.setVisibility(View.VISIBLE);
    }

    private RVGridAdapter.ViewHolder getViewHolder(int position) {
        return (RVGridAdapter.ViewHolder) mRecyclerView.findViewHolderForLayoutPosition(position);
    }
}
