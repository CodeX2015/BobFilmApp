package club.bobfilm.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import club.bobfilm.app.R;
import club.bobfilm.app.entity.Film;

/**
 * Created by CodeX on 27.06.2016.
 */
public class AddressLineAdapter extends RecyclerView.Adapter<AddressLineAdapter.ViewHolder> {
    private final Context mContext;
    private OnItemClickListener mListener;
    ArrayList<Film> mItems = new ArrayList<>();

    public AddressLineAdapter(Context context, OnItemClickListener listener, ArrayList<Film> items) {
        mContext = context;
        mListener = listener;
        if (items != null) {
            mItems = items;
        }
    }

    public void addChild(Film item) {
        mItems.add(item);
//            notifyItemInserted(getItemCount() - 1);
        notifyDataSetChanged();
    }

    public void removeChild() {
        if (getItemCount() > 0) {
            mItems.remove(getItemCount() - 1);
        }
        notifyDataSetChanged();
    }

    private void removeRange(int endItem) {
        ArrayList<Film> newItems = new ArrayList<>();
        for (int i = 0; i <= endItem; i++) {
            newItems.add(mItems.get(i));
        }
        mItems.clear();
        mItems = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_address_line, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final int idx = holder.getAdapterPosition();
        holder.mItem = mItems.get(position);
        holder.mPathItem.setText(holder.mItem.getFilmTitle() + " > ");
        if (position == getItemCount() - 1) {
            holder.mPathItem.setTextColor(ContextCompat.getColor(mContext, R.color.tab_text_selected));
        } else {
            holder.mPathItem.setTextColor(ContextCompat.getColor(mContext, R.color.tab_text_unselected));
        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeRange(idx);
                mListener.onItemClick(v, idx);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public ArrayList<Film> getItems() {
        return mItems;
    }

    public Film getItem(int position) {
        return mItems.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        private Film mItem;
        private final TextView mPathItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mPathItem = (TextView) mView.findViewById(R.id.tv_path_item);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);

    }
}
