package club.bobfilm.app.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.entity.Film;
import club.bobfilm.app.util.Utils;

import static club.bobfilm.app.adapter.TabHeaderAdapter.log;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ViewHolder, int} and makes a call to the
 * specified {@link RVGridAdapter.OnItemClickListener}.
 */
public class RVGridAdapter extends RecyclerView.Adapter<RVGridAdapter.ViewHolder> {

    private List<Film> mValues = null;
    private OnItemClickListener mClickListener = null;
    private Context mContext = null;


    public RVGridAdapter(Context context, List<Film> items) {
        mContext = context;
        mValues = items;
    }

    public RVGridAdapter(Context context, List<Film> items, OnItemClickListener listener) {
        mContext = context;
        mValues = items;
        mClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grid_films, parent, false);
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

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final int idx = holder.getAdapterPosition();
        holder.mItem = mValues.get(idx);
        try {
            String path = mValues.get(idx).getPosterUrl();
            if (path !=null && !path.equalsIgnoreCase("")){
                Utils.setImageViewBitmap(mContext, path, holder.mPosterView, null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        holder.mReviewsCountView.setText(mValues.get(idx).getReviews());
        holder.mIdView.setText(mValues.get(idx).getFilmTitle());
        holder.mCreateDateView.setText(Utils.convertDate(mValues.get(idx).getCreateDate(), "dd MMMMM yyyy", null));
        holder.mReviewsCountView.setText(mValues.get(idx).getReviews());
        if (mValues.get(position).isBookmarked()) {
            holder.mBookmarkView.setVisibility(View.VISIBLE);
        } else {
            holder.mBookmarkView.setVisibility(View.INVISIBLE);
        }
        holder.mActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickListener.onItemClick(v, idx);
            }
        });
        holder.mQuoteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickListener.onItemClick(v, idx);
            }
        });

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickListener != null) {
                    // Notify the active callbacks interface
                    // that an item has been selected.
                    mClickListener.onItemClick(v, idx);
                }
            }
        });
        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mClickListener.onItemLongClick(v, idx);
                return true;
            }
        });

    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnItemClickListener {
        void onItemClick(View v, int position);

        void onItemLongClick(View v, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mCreateDateView;
        public final TextView mReviewsCountView;
        public final ImageView mPosterView;
        public final ImageView mBookmarkView;
        public final LinearLayout mQuoteView;
        public final ImageView mActionView;
        public Film mItem;
        public final FrameLayout mFrameLayout;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mFrameLayout = (FrameLayout) view.findViewById(R.id.fl_poster);
            mPosterView = (ImageView) view.findViewById(R.id.card_poster);
            mIdView = (TextView) view.findViewById(R.id.tv_film_title);
            mCreateDateView = (TextView) view.findViewById(R.id.date_create);
            mReviewsCountView = (TextView) view.findViewById(R.id.film_reviews);
            mBookmarkView = (ImageView) view.findViewById(R.id.iv_bookmark);
            mQuoteView = (LinearLayout) view.findViewById(R.id.ll_quote);
            mActionView = (ImageView) view.findViewById(R.id.iv_grid_more);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mCreateDateView.getText() + "'";
        }
    }
}
