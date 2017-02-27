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
import club.bobfilm.app.adapter.RVListCommentsAdapter;
import club.bobfilm.app.entity.Comment;
import club.bobfilm.app.helpers.HTMLParser;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 24.04.2016.
 */
public class ActivityComments extends BaseActivity implements View.OnClickListener,
        RVListCommentsAdapter.OnItemClickListener {
    private Logger log = LoggerFactory.getLogger(ActivityComments.class);

    private RecyclerView mRecyclerView;
    private List<Comment> mComments = new ArrayList<>();

    private ViewFlipper mViewFlipper;
    private TextView mTVerror;

    private String mCommentsUrl;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onResume() {
        super.onResume();
        Application.setCurrentActivity(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_parent_list_without_padding);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mViewFlipper = (ViewFlipper) findViewById(R.id.vf_layout_changer);
        mTVerror = (TextView) findViewById(R.id.tv_error);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);

        setSwipeToRefresh();
        mSwipeRefreshLayout.setRefreshing(true);
        getData();
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
//                        mComments.clear();
                        mRecyclerView.getAdapter().notifyItemRangeRemoved(0, mComments.size() - 1);
                        getCommentsFromNetwork();
                    } catch (Exception ex) {
                        changeViewForError(ex.getMessage());
                    }
                }
            });
        }
    }

    private void getData() {
        if (getIntent() != null) {
            mCommentsUrl = getIntent().getExtras().getString(Utils.ARG_COMMENTS_URL);
            if (mCommentsUrl != null && !mCommentsUrl.equalsIgnoreCase("")) {
                try {
                    getCommentsFromNetwork();
                } catch (Exception ex) {
                    changeViewForError(ex.getMessage());
                }
            }
        }
    }

    private void getCommentsFromNetwork() {
        String commentsUrl = HTMLParser.SITE + mCommentsUrl;
        HTMLParser.getParsedSite(commentsUrl, HTMLParser.ACTION_COMMENTS, null, new HTMLParser.LoadListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void OnLoadComplete(Object result) {
                        mComments = new ArrayList<>((List<Comment>) result);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mComments.size() > 0) {
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

    private void changeViewForError(String msg) {
        mTVerror.setText(msg);
        mTVerror.setTextColor(ContextCompat.getColor(ActivityComments.this, R.color.text_color_error));
        mViewFlipper.setDisplayedChild(1);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void setData() {
        mRecyclerView.setBackgroundResource(R.color.layout_background_item);
        mRecyclerView.setAdapter(new RVListCommentsAdapter(ActivityComments.this, mComments, this));
        mViewFlipper.setDisplayedChild(2);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_error_repeat:
                mSwipeRefreshLayout.setRefreshing(true);
                getData();
                break;
        }
    }

    @Override
    public void onItemClick(View v, int position) {
        switch (v.getId()) {
            case R.id.tv_comment_title:
                log.info("comment_title clicked at {}", position);
                break;
            case R.id.tv_comment_user:
                log.info("user clicked at {}", position);
                break;
        }
    }

    @Override
    public void onItemLongClick(View v, int position) {

    }
}

