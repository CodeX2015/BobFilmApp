package club.bobfilm.app.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.ProgressBarIndeterminateDeterminate;
import club.bobfilm.app.R;
import club.bobfilm.app.entity.FilmFile;
import club.bobfilm.app.service.DownloadService;
import club.bobfilm.app.util.Utils;

import static club.bobfilm.app.util.Utils.humanReadableByteCount;


/**
 * Created by CodeX on 17.05.2016.
 */
public class DownloadsAdapter2 extends RecyclerView.Adapter<DownloadsAdapter2.ViewHolder> {

    private Logger log = LoggerFactory.getLogger(DownloadsAdapter2.class);
    private final Context mContext;
    private List<FilmFile> mItems;
    private OnItemClickListener mListener;
    private FilmFile mDeleteItem = null;

    public DownloadsAdapter2(List<FilmFile> files, Context context, OnItemClickListener listener) {
        this.mItems = files;
        this.mListener = listener;
        this.mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_download, parent, false);
//        log.info("onCreateViewHolder {}", viewType);
        try {
            return new ViewHolder(itemView);
        } catch (Exception ex) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), ex);
            }
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
//        log.info("getItemId {}", position);
        return mItems.get(position).id;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
//        log.info("onBindViewHolder {}", position);
        try {
            bindData(holder);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //animate(holder);
    }

    private void bindData(final ViewHolder holder) {
        int idx = holder.getAdapterPosition();
        final FilmFile item = mItems.get(idx);
        holder.Id = item.id;
        holder.tvName.setText(item.getFileName());
        if (item.isDownloadComplete() || item.getStatus() == DownloadService.DownloadStatuses.COMPLETE) {
            holder.progressBar.setVisibility(View.GONE);
            holder.btnPauseDownload.setVisibility(View.GONE);
            holder.tvDownloadPerSize.setText(humanReadableByteCount(item.getFileSize(), true));
        } else {
            checkPauseState(item, holder);
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.btnPauseDownload.setVisibility(View.VISIBLE);
            holder.progressBar.setProgress(item.getProgressValue());

            String downloadText = "";
            if (item.getDownloadPerSize() == null || item.getDownloadPerSize().equalsIgnoreCase("")) {
                downloadText = Utils.humanReadableByteCount(item.getFileSize(), true);
            } else {
                downloadText = item.getDownloadPerSize();
            }
            holder.tvDownloadPerSize.setText(downloadText);
        }

        holder.btnPauseDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = holder.getAdapterPosition();
//                log.info("BindView position {}, title = {} isPAUSED = {}, status {}",
//                        index, holder.tvName.getText(), item.isPaused, item.getStatus().toString());
                if (mListener != null) {
                    mListener.onItemClick(v, index);
                }
            }
        });
        holder.btnCancelDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = holder.getAdapterPosition();
                mDeleteItem = mItems.get(index);
                log.debug("click canceled position {}", index);
                mListener.onItemClick(v, index);
            }
        });
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = holder.getAdapterPosition();
                mListener.onItemClick(v, index);
            }
        });
        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int index = holder.getAdapterPosition();
                mListener.onItemLongClick(v, index);
                return true;
            }
        });
        holder.mView.setActivated(selectedItems.get(idx, false));
    }

    private void checkPauseState(FilmFile item, ViewHolder holder) {
        int index = holder.getAdapterPosition();
        log.info("check state title {} state {} isPaused {}",
                item.getFilmTitle(), item.getStatus().toString(), item.isPaused);
        if (item.getStatus() == DownloadService.DownloadStatuses.PAUSED
                || item.getStatus() == DownloadService.DownloadStatuses.COMPLETE
                || item.getStatus() == DownloadService.DownloadStatuses.FAILED) {
            log.info("State PAUSE holder position {}, title {} status {}",
                    index, holder.tvName.getText(), item.getStatus().toString());
            holder.btnPauseDownload.setActivated(true);
            item.isPaused = true;
        } else {
            log.info("StatePAUSE holder position {}, title {} status {}",
                    index, holder.tvName.getText(), item.getStatus().toString());
            holder.btnPauseDownload.setActivated(false);
            item.isPaused = false;
        }
    }

    //animate insert and remove item to adapter
    public void animate(ViewHolder holder) {
        final Animation animAnticipateOvershoot = AnimationUtils.loadAnimation(mContext, R.anim.rv_bounce_interpolator);
        holder.mView.setAnimation(animAnticipateOvershoot);
    }

    //Selected Item sections
    private SparseBooleanArray selectedItems = new SparseBooleanArray();

    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
        } else {
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);
    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    public FilmFile getDeleteItem() {
        return mDeleteItem;
    }

    public FilmFile getItem(int position) {
        return mItems.get(position);
    }

    public void clearDeleteItem() {
        mDeleteItem = null;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);

        void onItemLongClick(View v, int position);
    }

    public final class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        public int Id;
        @BindView(R.id.tv_file_name)
        public TextView tvName;

        @BindView(R.id.ib_start_pause_download)
        public ImageView btnPauseDownload;

        @BindView(R.id.ib_cancel_download)
        public ImageButton btnCancelDownload;

        @BindView(R.id.tv_progress_value)
        public TextView tvDownloadPerSize;

        @BindView(R.id.pb_download_progress)
        public ProgressBarIndeterminateDeterminate progressBar;

        public ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            ButterKnife.bind(this, itemView);
        }
    }
}