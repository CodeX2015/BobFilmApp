package club.bobfilm.app.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.adapter.DownloadsAdapter2;
import club.bobfilm.app.entity.FilmFile;
import club.bobfilm.app.helpers.DBHelper;
import club.bobfilm.app.service.DownloadService;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX.
 */
public class FragmentDownloads extends Fragment {

    private Logger log = LoggerFactory.getLogger(FragmentDownloads.class);
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private DownloadsAdapter2 mAdapter;
    private List<FilmFile> mDownloadFiles = new ArrayList<>();
    private ViewFlipper mViewFlipper;
    private TextView mTvError;
    private Context mContext;
    private BroadcastReceiver mReceiver;
    private ActionBar mToolbar;
    private ActionMode mActionMode;

    private ActionMode.Callback mDeleteMode = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            ((AppCompatActivity) mContext).getMenuInflater().inflate(R.menu.list_actionmode_menu, menu);
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
                    View btnDelete = ((AppCompatActivity) mContext).findViewById(R.id.am_item_delete);
                    showPopup(btnDelete);
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

    private PopupMenu.OnMenuItemClickListener menuItemListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            FilmFile deleteItem = mAdapter.getDeleteItem();
            List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
            switch (item.getItemId()) {
                case R.id.pp_menu_delete:
                    if (deleteItem != null) {
                        deleteItem(deleteItem, false);
                        log.warn("delete {}", deleteItem.getmFileName());
                        mAdapter.clearDeleteItem();
                    } else {
                        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                            deleteItem(mDownloadFiles.get(selectedItemPositions.get(i)), false);
                        }
                        mActionMode.finish();
                    }
                    return true;
                case R.id.pp_menu_delete_files:
                    if (deleteItem != null) {
                        deleteItem(deleteItem, true);
                        log.warn("delete {}", deleteItem.getmFileName());
                        mAdapter.clearDeleteItem();
                    } else {
//                    Toast.makeText(mContext, "delete download files", Toast.LENGTH_SHORT).show();
                        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                            deleteItem(mDownloadFiles.get(selectedItemPositions.get(i)), true);
                        }
                        mActionMode.finish();
                    }
                    return true;
                default:
                    return false;
            }
        }
    };

    private void showPopup(View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.popup_menu_delete, popup.getMenu());
        String clickView;
        switch (v.getId()) {
            case R.id.action_settings:
                clickView = "btnDeleteItem";
                popup.setOnMenuItemClickListener(menuItemListener);
                break;
            case R.id.ib_cancel_download:
                clickView = "btnDeleteItem";
                popup.setOnMenuItemClickListener(menuItemListener);
                break;
            case R.id.am_item_delete:
                clickView = "menu_btnDeleteSelected";
                // This activity implements OnMenuItemClickListener
                popup.setOnMenuItemClickListener(menuItemListener);
                break;
            default:
                clickView = "Unknown btn " + v.getId();
                break;
        }
        log.info("Downloads click on {}", clickView);
        popup.show();
    }

    private DownloadsAdapter2.OnItemClickListener
            mItemClickListener = new DownloadsAdapter2.OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
            FilmFile file = /*mDownloadFiles.get(position);*/mAdapter.getItem(position);
            int idx = Utils.getIndexOfItem(mDownloadFiles, file);
            int idx2 = Utils.getIndexOfItem(mDownloadFiles, file);
            log.info("OnItemClick file {}, index={}, Index2={}, position {}, status: {} isPaused {}",
                    file.getmFileName(), idx, idx2, position, file.getStatus(), file.isPaused);

            switch (v.getId()) {
                case R.id.ib_start_pause_download:
                    try {
                        v.setActivated(!v.isActivated());
                        if (v.isActivated()) {
                            log.info("FilmFile {} with id {} at position {} click_pause - PAUSE",
                                    file.getmFileName(), file.id, idx);
                            DownloadService.intentPause(mContext, file);
                        } else {
                            log.info("FilmFile {} with id {} at position {} click_pause - DOWNLOAD",
                                    file.getmFileName(), file.id, idx);
                            DownloadService.intentDownload(mContext, file);
                        }
                    } catch (Exception ex) {
                        if (BuildConfig.DEBUG) {
                            log.info("FilmFile {} with id {} at position {} ERROR click_play_pause",
                                    file.getmFileName(), file.id, idx);
                            ex.printStackTrace();
                        } else {
                            log.error(Utils.getErrorLogHeader() + new Object() {
                            }.getClass().getEnclosingMethod().getName(), ex);
                        }
                    }
                    break;
                case R.id.ib_cancel_download:
                    log.info("FilmFile {} with id {} at list_idx={} position={} cancel_download",
                            file.getmFileName(), file.id, idx, position);
                    try {
                        DownloadsAdapter2.ViewHolder holder = getViewHolder(position);
                        View btnCancel = holder.btnCancelDownload;
                        //because if anchor for clicked view in recyclerview will scrolled
                        // and hide toolbar(issue in appcompat library)
                        //showPopup(((BaseTabActivity) mContext).findViewById(R.id.action_settings));
                        showPopup(btnCancel);
                    } catch (Exception ex) {
                        if (BuildConfig.DEBUG) {
                            log.debug("FilmFile {} with id {} at position {} ERROR cancel_download",
                                    file.getmFileName(), file.id, idx);
                            ex.printStackTrace();
                        } else {
                            log.error(Utils.getErrorLogHeader() + new Object() {
                            }.getClass().getEnclosingMethod().getName(), ex);
                        }
                    }
                    break;
                default:
                    // item click
//                    idx = mRecyclerView.getChildAdapterPosition(v);
                    if (mActionMode != null) {
                        myToggleSelection(idx);
                        return;
                    }
                    //Check exist file before play
                    if (file.isDownloadComplete()) {
                        String filePath = file.getmFilePath();
                        log.info("try open file {} and status {}", filePath, file.getStatus());
                        if (Utils.checkFileExist(filePath)) {
                            Utils.playVideo(file.getmFilePath(), mContext);
                        } else {
                            Toast.makeText(mContext,
                                    mContext.getString(R.string.msg_download_file_does_not_exist),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (file.getStatus() == DownloadService.DownloadStatuses.DOWNLOADING) {
                            Toast.makeText(mContext,
                                    mContext.getString(R.string.msg_download_file_downloaded_now),
                                    Toast.LENGTH_SHORT).show();
                        } else if (file.getStatus() == DownloadService.DownloadStatuses.PAUSED) {
                            Toast.makeText(mContext,
                                    mContext.getString(R.string.msg_download_file_downloaded_now),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext,
                                    mContext.getString(R.string.msg_download_file_does_not_exist),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
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
            myToggleSelection(mRecyclerView.getChildAdapterPosition(v));
        }
    };

    private void myToggleSelection(int position) {
        mAdapter.toggleSelection(position);
        String title = getString(R.string.selected_count, String.valueOf(mAdapter.getSelectedItemCount()));
        mActionMode.setTitle(title);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onDestroyView() {
        unregisterReceiver();
        log.warn("destroy DownloadService.mDownloadingFiles size = {}",
                DownloadService.mDownloadingFiles.size());
        mDownloadFiles.clear();
        super.onDestroyView();
    }

    public void unregisterReceiver() {
        // дерегистрируем (выключаем) BroadcastReceiver
        if (mReceiver != null) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parent_list, container, false);
        mToolbar = ((AppCompatActivity) mContext).getSupportActionBar();
        mViewFlipper = (ViewFlipper) view.findViewById(R.id.vf_layout_changer);
        mTvError = (TextView) view.findViewById(R.id.tv_error);
        TextView mBtnErrRepeat = (TextView) view.findViewById(R.id.tv_error_repeat);
        mBtnErrRepeat.setVisibility(View.INVISIBLE);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        if (mRecyclerView != null) {
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        }
        setDownloadReceiver();
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        setSwipeToRefresh();
        getDataFromDB();
        return view;
    }

    private void setDownloadReceiver() {
        // создаем BroadcastReceiver
        mReceiver = new BroadcastReceiver() {
            // действия при получении сообщений
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action == null || !action.equals(DownloadService.ACTION_DOWNLOAD_BROADCAST)) {
                    return;
                }

                //noinspection unchecked
                FilmFile downloadFile = (FilmFile) intent.getSerializableExtra(DownloadService.EXTRA_FILE_INFO);
                if (downloadFile == null) {
                    return;
                }
                log.info("receive file={} status={} from service mDownloadFiles size {}",
                        downloadFile.getmFileName(), downloadFile.getStatus().name(), mDownloadFiles.size());
                if (mDownloadFiles != null && mDownloadFiles.size() == 0) {
                    //todo don't understand for what that???
                    log.info("mDownloadFiles IS EMPTY!!!!");
                    changeViewForError(getResources().getString(R.string.msg_no_downloads));
                    getDataFromDB();
                } else {
                    try {
                        mViewFlipper.setDisplayedChild(2);
                        updateItems(downloadFile);
                    } catch (Exception ex) {
                        changeViewForError(ex.toString());
                        if (BuildConfig.DEBUG) {
                            ex.printStackTrace();
                        } else {
                            log.error(Utils.getErrorLogHeader() + new Object() {
                            }.getClass().getEnclosingMethod().getName(), ex);
                        }
                    }
                }
            }
        };
        // создаем фильтр для BroadcastReceiver
        log.warn("Download receiver registered");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadService.ACTION_DOWNLOAD_BROADCAST);
        // регистрируем (включаем) BroadcastReceiver
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver, intentFilter);
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
                    reloadAdapter();
                    // Stop refresh animation
                    //mSwipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    private void reloadAdapter() {
        mViewFlipper.setDisplayedChild(0);
        if (mDownloadFiles != null) {
            mDownloadFiles.clear();
        }
        if (mAdapter != null) {
            int ItemCount = mAdapter.getItemCount();
            //log.info("before {}", mAdapter.getItemCount());
            mAdapter.notifyItemRangeRemoved(0, ItemCount > 0 ? ItemCount - 1 : 0);
            //log.info("middle {}", mAdapter.getItemCount());
            mAdapter.notifyItemRangeChanged(0, ItemCount > 0 ? ItemCount - 1 : 0);
            //log.info("after {}", mAdapter.getItemCount());
            mAdapter = null;
            //mRecyclerView.getRecycledViewPool().clear();
        }
        getDataFromDB();
    }

    private void getDataFromDB() {
        checkDownloadingFiles();
        log.info("getDataFromDB");
        DBHelper.getInstance(mContext).dbWorker(
                DBHelper.ACTION_GET,
                DBHelper.FN_DOWNLOADS,
                null,
                new DBHelper.OnDBOperationListener() {
                    @Override
                    public void onSuccess(Object result) {
                        //noinspection unchecked
                        final List<FilmFile> mDataFromDB = (List<FilmFile>) result;
                        if (mDataFromDB.size() > 0) {
                            log.warn("mDataFromDB size is {}", mDataFromDB.size());
                            sortList(mDataFromDB);
                        }
                        ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mDownloadFiles.size() > 0) {
                                    mViewFlipper.setDisplayedChild(2);
                                    mSwipeRefreshLayout.setRefreshing(false);
                                } else {
                                    changeViewForError(getResources().getString(R.string.msg_no_downloads));
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception ex) {
                        ex.printStackTrace();
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

    private void checkDownloadingFiles() {
        if (DownloadService.mDownloadingFiles != null) {
            log.warn("DownloadService.mDownloadingFiles size = {}", DownloadService.mDownloadingFiles.size());
        }
        if (DownloadService.mDownloadingFiles != null && DownloadService.mDownloadingFiles.size() > 0) {
            mDownloadFiles = new ArrayList<>(DownloadService.mDownloadingFiles);
            setAdapter();
            log.warn("create mDownloadFiles size is {}, mRecyclerView child: {}",
                    mDownloadFiles.size(), mRecyclerView.getAdapter().getItemCount());
        }
    }

    private void sortList(final List<FilmFile> filedFromDB) {
        try {
            if (mDownloadFiles != null) {
                final int downloadingFilesSize = mDownloadFiles.size();
                for (int i = 0; i < filedFromDB.size(); i++) {
                    if (Utils.getIndexOfItem(mDownloadFiles, filedFromDB.get(i)) == -1) {
                        //field from app crash
                        DownloadService.DownloadStatuses fileStatus = filedFromDB.get(i).getStatus();
                        if (fileStatus != DownloadService.DownloadStatuses.COMPLETE
                                && fileStatus != DownloadService.DownloadStatuses.PAUSED) {
                            log.warn("Reset file download status to 'failed'".toUpperCase());
                            filedFromDB.get(i).setStatus(DownloadService.DownloadStatuses.FAILED);
                        } else {
                            //log.warn("file status is {}", fileStatus.toString());
                        }
                        mDownloadFiles.add(downloadingFilesSize + i, filedFromDB.get(i));
                    }
                }
                ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mAdapter == null) {
                            log.warn("setAdapter");
                            setAdapter();
                        } else {
                            log.warn("notifyDataSetChanged");
//                            mAdapter.notifyItemRangeInserted(downloadingFilesSize, filedFromDB.size() - 1);
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                });
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

    private void setAdapter() {
        mAdapter = new DownloadsAdapter2(mDownloadFiles, mContext, mItemClickListener);
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
        if (mViewFlipper.getDisplayedChild() != 2) {
            mViewFlipper.setDisplayedChild(2);
        }
    }

    private void deleteItem(FilmFile file, boolean withFile) {
        int position = mDownloadFiles.indexOf(file);
        if (file.getStatus() != DownloadService.DownloadStatuses.COMPLETE
                || file.getStatus() != DownloadService.DownloadStatuses.FAILED
                || file.getStatus() != DownloadService.DownloadStatuses.CANCELED) {

            DownloadService.intentCancel(mContext, file);

            //todo need check file position
            int servicePosition = Utils.getIndexOfItem(DownloadService.mDownloadingFiles, file);
            if (servicePosition != -1) {
                DownloadService.mDownloadingFiles.remove(servicePosition);
            }
        }
        if (withFile) {
            Utils.deleteFile(file.getmFilePath());
        }
        DBHelper.getInstance(mContext).dbWorker(DBHelper.ACTION_DELETE, DBHelper.FN_DOWNLOADS, mDownloadFiles.get(position), null);
        mDownloadFiles.remove(file);
        mAdapter.notifyItemRemoved(position);
        log.info("REMOVE item {} at position {}, withFile {}", file.getmFileName(), position, withFile);
        mAdapter.notifyItemRangeChanged(position, mDownloadFiles.size());
    }

    private void updateItems(FilmFile file) {
        updateDownloadFileInList(file);
        final int position = mDownloadFiles.indexOf(file);
        if (position == -1) {
            log.warn("file {} doesn't exist in mDownloadFiles (pos={})",
                    file.getmFileName(), position);
            return;
        }
        final DownloadService.DownloadStatuses status = file.getStatus();
        DownloadsAdapter2.ViewHolder holder = getViewHolder(position);
        if (holder == null) {
            log.warn("holder at position {} doesn't exist in adapter, mAdapter isNull {}",
                    position, mAdapter == null);
            return;
        }

        if (file.id != holder.Id) {
            //log.warn("holder id {} doesn't equals item id {} at position {}",
            // holder.Id, file.id, position);
            return;
        }
        int adapterPosition = holder.getAdapterPosition();
//        log.warn("FilmFile {} with id {}, with title {} at position {} has status {}",
//                file.getmFileName(), file.id, holder.tvName.getText(), position, file.getStatusName(status));
        switch (status) {
            case CONNECTING:
                if (isCurrentRVItemVisible(position)) {
                    holder.progressBar.setIndeterminate(true);
                    holder.progressBar.setVisibility(View.VISIBLE);
                    holder.btnPauseDownload.setVisibility(View.VISIBLE);
                    holder.btnPauseDownload.setActivated(false);
                    log.info("refreshView: CONNECTING {} at pos={}, with name {}, status {} icon_pause",
                            mDownloadFiles.get(position).getmFileName(), position,
                            holder.tvName.getText(), file.getStatus().toString());
                }
                break;
            case DOWNLOADING:
                if (isCurrentRVItemVisible(position)) {
                    log.debug("refreshView: DOWNLOADING {} at pos={}, with name {}, status {} icon_pause",
                            mDownloadFiles.get(position).getmFileName(), position,
                            holder.tvName.getText(), file.getStatus().toString());
                    holder.btnPauseDownload.setActivated(false);
                    holder.progressBar.setVisibility(View.VISIBLE);
                    holder.btnPauseDownload.setVisibility(View.VISIBLE);
                    holder.tvDownloadPerSize.setText(file.getDownloadPerSize());
                    holder.progressBar.setProgress(file.getmProgressValue());
                }
                break;
            case COMPLETE:
                if (isCurrentRVItemVisible(position)) {
                    holder.tvDownloadPerSize.setText(Utils.humanReadableByteCount(file.getmFileSize(), true));
                    holder.progressBar.setVisibility(View.GONE);
                    holder.btnPauseDownload.setVisibility(View.GONE);
                }
                break;
            case PAUSED:
                if (isCurrentRVItemVisible(position)) {
                    holder.progressBar.setVisibility(View.VISIBLE);
                    holder.btnPauseDownload.setVisibility(View.VISIBLE);
                    holder.btnPauseDownload.setActivated(true);
                    log.info("refreshView: PAUSED {} at pos={}, with name {}, status {} icon_play",
                            mDownloadFiles.get(position).getmFileName(), position,
                            holder.tvName.getText(), file.getStatus().toString());
                }
                break;
            case CANCELED:
                //cancel download
                if (isCurrentRVItemVisible(position)) {
//                    holder.progressBar.setProgress(file.getmProgressValue());
//                    holder.tvDownloadPerSize.setText(file.getDownloadPerSize());
                }
                break;
            case FAILED:
                file.setDownloadPerSize("");
                if (isCurrentRVItemVisible(position)) {
                    holder.progressBar.setIndeterminate(false);
                    holder.progressBar.setVisibility(View.VISIBLE);
                    holder.btnPauseDownload.setVisibility(View.VISIBLE);
                    holder.tvDownloadPerSize.setText(mContext.getString(R.string.msg_download_error));
                    holder.btnPauseDownload.setActivated(true);

                }
                break;
        }
    }

    private void updateDownloadFileInList(FilmFile downloadFile) {
        if (downloadFile.getStatus() == DownloadService.DownloadStatuses.CANCELED) {
            return;
        }
        boolean shouldAdd = true;
        for (FilmFile file : mDownloadFiles) {
            if (file.equals(downloadFile)) {
                mDownloadFiles.set(mDownloadFiles.indexOf(file), downloadFile);
                shouldAdd = false;
                log.warn("SET FilmFile {} with id {}, at position {}",
                        file.getmFileName(), file.id, mDownloadFiles.indexOf(file));
//                mAdapter.notifyItemChanged(mDownloadFiles.indexOf(file));
                break;
            }
        }
        if (shouldAdd) {
            mDownloadFiles.add(0, downloadFile);
            mAdapter.notifyItemInserted(0);
            log.warn("ADD FilmFile {} with id {} at position {} mAdapter.notifyItemInserted!",
                    downloadFile.getmFileName(), downloadFile.id, mDownloadFiles.indexOf(downloadFile));
        }
    }

    private DownloadsAdapter2.ViewHolder getViewHolder(int position) {
        return (DownloadsAdapter2.ViewHolder) mRecyclerView.findViewHolderForLayoutPosition(position);
    }

    private boolean isCurrentRVItemVisible(int position) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        int first = layoutManager.findFirstVisibleItemPosition();
        int last = layoutManager.findLastVisibleItemPosition();
        //  log.error("position is visible {}", result);
        return first <= position && position <= last;
    }

    private void changeViewForError(String msg) {
        mTvError.setText(msg);
        mTvError.setTextColor(ContextCompat.getColor(mContext, R.color.text_color_error));
        mViewFlipper.setDisplayedChild(1);
        mSwipeRefreshLayout.setRefreshing(false);
    }
}
