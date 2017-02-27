package club.bobfilm.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.entity.FilmFile;
import club.bobfilm.app.util.Utils;


public class RVListHistoryAdapter extends RecyclerView.Adapter<RVListHistoryAdapter.ViewHolder> {

    Logger log = LoggerFactory.getLogger(RVListHistoryAdapter.class);
    private List<FilmFile> mItems = null;
    private Context mContext = null;
    private OnItemClickListener mListener;

    public RVListHistoryAdapter(Context context, List<FilmFile> items, OnItemClickListener listener) {
        mListener = listener;
        mContext = context;
        mItems = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_history, parent, false);
        try {
            return new ViewHolder(view);
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

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final int idx = holder.getAdapterPosition();
        holder.mItem = mItems.get(position);
        holder.mFileNameView.setText(holder.mItem.getmFileName());
        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mListener.onItemLongClick(v, idx);
                return true;
            }
        });
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onItemClick(v, idx);
            }
        });

        holder.mDeleteHistoryBtnView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onItemClick(v, idx);
            }
        });
        holder.itemView.setActivated(selectedItems.get(position, false));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
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

    public interface OnItemClickListener {
        void onItemClick(View v, int position);

        void onItemLongClick(View v, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        private FilmFile mItem;
        private final TextView mFileNameView;
        private final TextView mFileCreateDateView;
        private final TextView mFileCreateTimeView;
        private ImageButton mDeleteHistoryBtnView;


        public ViewHolder(View view) {
            super(view);
            mView = view;
            mFileNameView = (TextView) view.findViewById(R.id.tv_file_name);
            mFileCreateDateView = (TextView) view.findViewById(R.id.tv_file_create_date);
            mFileCreateTimeView = (TextView) view.findViewById(R.id.tv_file_create_time);
            mDeleteHistoryBtnView = (ImageButton) view.findViewById(R.id.ib_remove_item);
        }

        @Override
        public String toString() {
            return super.toString();
        }

    }
}
