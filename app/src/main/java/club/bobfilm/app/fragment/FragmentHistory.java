package club.bobfilm.app.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.activity.ActivityDetails;
import club.bobfilm.app.activity.ActivityTabMain;
import club.bobfilm.app.adapter.RVListBookmarksAdapter;
import club.bobfilm.app.adapter.RVListHistoryAdapter;
import club.bobfilm.app.entity.Film;
import club.bobfilm.app.entity.FilmFile;
import club.bobfilm.app.helpers.DBHelper;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 24.04.2016.
 */
public class FragmentHistory extends BaseFragment {
    Logger log = LoggerFactory.getLogger(FragmentHistory.class);

    private RecyclerView mRecyclerView;
    private List<FilmFile> mDataFromDB = new ArrayList<>();
    private RVListHistoryAdapter mAdapter;
    private ViewFlipper mViewFlipper;
    private TextView mTVerror;
    private ActionBar mToolbar;
    private Context mContext;

    private ActionMode.Callback mDeleteMode = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getActivity().getMenuInflater().inflate(R.menu.list_actionmode_menu, menu);
//            mToolbar.hide();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.am_item_delete:
                    List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
                    for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                        deleteItem(selectedItemPositions.get(i));
                    }
                    actionMode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mActionMode = null;
            mAdapter.clearSelections();
//            mToolbar.show();
        }
    };

    private ActionMode mActionMode;

    private RVListHistoryAdapter.OnItemClickListener
            mItemClickListener = new RVListHistoryAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
            try {
                switch (v.getId()) {
                    case R.id.ib_remove_item:
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(v.getContext(),
                                    "ITEM DELETED = " + String.valueOf(position), Toast.LENGTH_SHORT).show();
                        } else {
                            deleteItem(position);
                        }
                        break;
                    default:
//                    Toast.makeText(v.getContext(),
//                            "ROW PRESSED = " + String.valueOf(position), Toast.LENGTH_SHORT).show();

                        // item click
//                        int idx = mRecyclerView.getChildAdapterPosition(v);
                        if (mActionMode != null) {
                            myToggleSelection(position);
                            return;
                        }
                        startActivityDetails(generateFilm(position));
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void onItemLongClick(View v, int position) {
            if (mActionMode != null) {
                return;
            }
//            Toast.makeText(v.getContext(),
//                    "ROW LONG PRESSED = " + String.valueOf(position), Toast.LENGTH_SHORT).show();
            mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mDeleteMode);
            myToggleSelection(position);
        }
    };
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Film generateFilm(int position) {
        FilmFile file = mDataFromDB.get(position);
        return new Film(file.getFilmTitle(), file.getFilmUrl(), file.getFilmLogoUrl(), file.isFilmBookmarked());
    }

    private void startActivityDetails(Film film) {
        Intent myIntent = new Intent(getActivity(), ActivityDetails.class);
        myIntent.putExtra(Utils.ARG_FILM_DETAILS, film);
        getActivity().startActivityForResult(myIntent, ActivityTabMain.REQUEST_FILMS_DETAILS);
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void deleteItem(int position) {
        DBHelper.getInstance(getActivity()).dbWorker(DBHelper.ACTION_DELETE, DBHelper.FN_HISTORY, mDataFromDB.get(position), null);
        mAdapter.notifyItemRemoved(position);
        mDataFromDB.remove(position);
        mAdapter.notifyItemRangeChanged(position, mAdapter.getItemCount());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_parent_list, container, false);
        mViewFlipper = (ViewFlipper) mView.findViewById(R.id.vf_layout_changer);
        mTVerror = (TextView) mView.findViewById(R.id.tv_error);
        TextView mBtnErrRepeat = (TextView) mView.findViewById(R.id.tv_error_repeat);
        mBtnErrRepeat.setVisibility(View.INVISIBLE);
        mToolbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        mRecyclerView = (RecyclerView) mView.findViewById(R.id.list);
        mSwipeRefreshLayout = (SwipeRefreshLayout) mView.findViewById(R.id.swipeRefreshLayout);
        setSwipeToRefresh();
        getDataFromDB();
        return mView;
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
                    clearAdapter();
                    getDataFromDB();

                    // Stop refresh animation
                    //mSwipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    private void clearAdapter() {
        if (mDataFromDB != null) {
            mDataFromDB.clear();
        }
        if (mAdapter != null) {
            mAdapter.notifyItemRangeRemoved(0, mAdapter.getItemCount() - 1);
            mAdapter = null;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    private void getDataFromDB() {
        DBHelper.getInstance(getActivity()).dbWorker(
                DBHelper.ACTION_GET,
                DBHelper.FN_HISTORY,
                null,
                new DBHelper.OnDBOperationListener() {
                    @Override
                    public void onSuccess(Object result) {
                        //noinspection unchecked
                        mDataFromDB = (List<FilmFile>) result;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setData();
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception ex) {
                        ex.printStackTrace();
                        getActivity().runOnUiThread(new Runnable() {
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

    private void setData() {
        if (mDataFromDB.size() > 0 && mRecyclerView != null) {
            mAdapter = new RVListHistoryAdapter(getActivity(), mDataFromDB, mItemClickListener);
            mRecyclerView.setAdapter(mAdapter);
            mViewFlipper.setDisplayedChild(2);
        } else {
            changeViewForError(getResources().getString(R.string.msg_no_history));
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void myToggleSelection(int position) {
        mAdapter.toggleSelection(position);
        String title = getString(R.string.selected_count, String.valueOf(mAdapter.getSelectedItemCount()));
        mActionMode.setTitle(title);
    }

    private RVListBookmarksAdapter.ViewHolder getViewHolder(int position) {
        return (RVListBookmarksAdapter.ViewHolder) mRecyclerView.findViewHolderForLayoutPosition(position);
    }
}
