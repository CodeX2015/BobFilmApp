package club.bobfilm.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.entity.Film;
import club.bobfilm.app.util.Utils;


public class RVListBookmarksAdapter extends RecyclerView.Adapter<RVListBookmarksAdapter.ViewHolder> {


    Logger log = LoggerFactory.getLogger(RVListBookmarksAdapter.class);
    private List<Film> mItems = null;
    private Context mContext = null;
    private OnItemClickListener mListener;

    public RVListBookmarksAdapter(Context context, List<Film> items, OnItemClickListener listener) {
        mListener = listener;
        mContext = context;
        mItems = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_bookmark, parent, false);
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
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final int idx = holder.getAdapterPosition();
        holder.mItem = mItems.get(position);
        holder.mFileNameView.setText(holder.mItem.getFilmTitle());
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onItemClick(v, idx);
            }
        });
        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mListener.onItemLongClick(v, idx);
                return true;
            }
        });
        holder.mBookmarkDeleteBtnView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onItemClick(v, idx);
            }
        });
        holder.itemView.setActivated(selectedItems.get(position, false));
//        holder.cbxSelectedLine.setChecked(selectedItems.get(position, false));
//        if (holder.cbxSelectedLine.isChecked()) {
//            holder.cbxSelectedLine.setVisibility(View.VISIBLE);
//        } else {
//            holder.cbxSelectedLine.setVisibility(View.GONE);
//        }
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

    public void removeData(int pos) {
        //mItems.remove(pos);
        notifyItemRemoved(pos);
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);

        void onItemLongClick(View v, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        private Film mItem;
        private final TextView mFileNameView;
        private ImageButton mBookmarkDeleteBtnView;
        private CheckBox cbxSelectedLine;


        public ViewHolder(View view) {
            super(view);
            mView = view;
            mFileNameView = (TextView) view.findViewById(R.id.tv_file_name);
//            cbxSelectedLine = (CheckBox) view.findViewById(R.id.cbx_select_line);
            mBookmarkDeleteBtnView = (ImageButton) view.findViewById(R.id.ib_remove_item);
//            view.setOnClickListener(this);
//            view.setOnLongClickListener(this);
//            mBookmarkDeleteBtnView.setOnClickListener(this);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
