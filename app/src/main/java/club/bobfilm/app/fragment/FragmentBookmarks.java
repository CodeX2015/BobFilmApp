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
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import club.bobfilm.app.adapter.RVListBookmarksAdapter;
import club.bobfilm.app.entity.Film;
import club.bobfilm.app.helpers.DBHelper;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 24.04.2016.
 */
public class FragmentBookmarks extends BaseFragment {
    Logger log = LoggerFactory.getLogger(FragmentBookmarks.class);
    private static final String TAG = "BookmarkListFragment";
    private RecyclerView mRecyclerView;
    private List<Film> mDataFromDB = new ArrayList<>();

    private ViewFlipper mViewFlipper;
    private TextView mTVerror;
    private Context mContext;
    private RVListBookmarksAdapter mAdapter;
    private ActionBar mToolbar;
    private ActionMode mActionMode;

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
//                    View btnDelete = ((AppCompatActivity)mContext).findViewById(R.id.am_item_delete);
//                    showPopup(btnDelete);
                    List<Integer> selectedItemPositions =
                            mAdapter.getSelectedItems();
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



    private RVListBookmarksAdapter.OnItemClickListener
            mItemClickListener = new RVListBookmarksAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
            switch (v.getId()) {
                case R.id.ib_remove_item:
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(v.getContext(),
                                "ITEM DELETED = " + String.valueOf(position), Toast.LENGTH_SHORT).show();
                        deleteItem(position);
                    } else {
                        deleteItem(position);
                    }
                    break;
                default:
//                    Toast.makeText(v.getContext(),
//                            "ROW PRESSED = " + String.valueOf(position), Toast.LENGTH_SHORT).show();

                    // item click
//                    int idx = mRecyclerView.getChildAdapterPosition(v);
                    if (mActionMode != null) {
                        myToggleSelection(position);
                        return;
                    }
                    startActivityDetails(mDataFromDB.get(position));
                    break;
            }
        }

        @Override
        public void onItemLongClick(View v, int position) {
            if (mActionMode != null) {
                return;
            }
//            Toast.makeText(v.getContext(),
//                    "ROW LONG PRESSED = " + String.valueOf(position), Toast.LENGTH_SHORT).show();
            mActionMode = ((AppCompatActivity) mContext).startSupportActionMode(mDeleteMode);
//            try {
//                int idx = mRecyclerView.getChildAdapterPosition(v);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
            myToggleSelection(position);
        }
    };

    private void startActivityDetails(Film film) {
        Intent myIntent = new Intent(mContext, ActivityDetails.class);
        myIntent.putExtra(Utils.ARG_FILM_DETAILS, film);
        mContext.startActivity(myIntent);
        ((AppCompatActivity) mContext).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private PopupMenu.OnMenuItemClickListener menuItemListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.pp_menu_delete:
//                    Toast.makeText(mContext, "delete from downloads", Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.pp_menu_delete_files:
//                    Toast.makeText(mContext, "delete download files", Toast.LENGTH_SHORT).show();
                    return true;
                default:
                    return false;
            }
        }
    };
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private void showPopup(View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(menuItemListener);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.popup_menu_delete, popup.getMenu());
        popup.show();
    }

    private void deleteItem(int position) {
        DBHelper.getInstance(mContext).dbWorker(DBHelper.ACTION_DELETE, DBHelper.FN_BOOKMARKS, mDataFromDB.get(position), null);
        log.info("delete {} at {} adapter {} List {}",
                mDataFromDB.get(position).getFilmTitle(), position, mAdapter.getItemCount(), mDataFromDB.size());
        mAdapter.notifyItemRemoved(position);
        mDataFromDB.remove(position);
        mAdapter.notifyItemRangeChanged(position, mAdapter.getItemCount());
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_parent_list, container, false);
        mViewFlipper = (ViewFlipper) mView.findViewById(R.id.vf_layout_changer);
        mTVerror = (TextView) mView.findViewById(R.id.tv_error);
        TextView mBtnErrRepeat = (TextView) mView.findViewById(R.id.tv_error_repeat);
        mBtnErrRepeat.setVisibility(View.INVISIBLE);
        mToolbar = ((AppCompatActivity) mContext).getSupportActionBar();
        mRecyclerView = (RecyclerView) mView.findViewById(R.id.list);
        mSwipeRefreshLayout = (SwipeRefreshLayout) mView.findViewById(R.id.swipeRefreshLayout);
        setSwipeToRefresh();
        getDataFromDB();
        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
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
            mAdapter.notifyItemRangeRemoved(0, mAdapter.getItemCount()-1);
            mAdapter = null;
        }
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
                                setData();
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception ex) {
                        ex.printStackTrace();
                        log.error(Utils.getErrorLogHeader() + new Object() {
                        }.getClass().getEnclosingMethod().getName(), ex);
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
    }

    private void setData() {
        if (mDataFromDB.size() > 0 && mRecyclerView != null) {
            mAdapter = new RVListBookmarksAdapter(mContext, mDataFromDB, mItemClickListener);
            mRecyclerView.setAdapter(mAdapter);
            mViewFlipper.setDisplayedChild(2);
        } else {
            changeViewForError(getResources().getString(R.string.msg_no_bookmarks));
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void myToggleSelection(int position) {
        mAdapter.toggleSelection(position);
        String title = getString(R.string.selected_count, String.valueOf(mAdapter.getSelectedItemCount()));
        mActionMode.setTitle(title);
    }

}

