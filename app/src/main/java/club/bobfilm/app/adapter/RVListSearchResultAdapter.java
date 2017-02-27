package club.bobfilm.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.entity.Film;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 03.05.2016.
 */
public class RVListSearchResultAdapter extends RecyclerView.Adapter<RVListSearchResultAdapter.ViewHolder> {

    private final OnItemClickListener mListener;
    Logger log = LoggerFactory.getLogger(RVListSearchResultAdapter.class);
    private List<Film> mItems = null;
    private Context mContext = null;
    boolean STATE_PLAY = false;
    boolean STATE_PAUSE = true;
    boolean mIsPauseState = STATE_PLAY;

    public RVListSearchResultAdapter(Context context, List<Film> items, RVListSearchResultAdapter.OnItemClickListener listener) {
        mContext = context;
        mItems = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_search_results, parent, false);
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
        try {
            holder.mItem = mItems.get(position);
            if (holder.mItem.getPosterUrl() != null && holder.mFilmPoster != null) {
                Utils.setImageViewBitmap(mContext, holder.mItem.getPosterUrl(), holder.mFilmPoster, null);
            }
            holder.mFilmTitleView.setText(holder.mItem.getFilmTitle());
            holder.mFilmAboutView.setText(Html.fromHtml(holder.mItem.getFilmAbout()));
            holder.mFilmCreateDate.setText(Utils.convertDate(mItems.get(position).getCreateDate(),
                    "hh:mm, dd MMMMM yyyy", "hh:mm, dd.MM.yyyy"));
            holder.mFilmReviews.setText(mItems.get(position).getReviews());
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClick(v, holder.getAdapterPosition());
                }
            });
            holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mListener.onItemLongClick(v, holder.getAdapterPosition());
                    return true;
                }
            });
        } catch (Exception ex) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), ex);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void swapData(List<Film> data) {
        if (data != null) {
            if (mItems != null) {
                mItems.clear();
            } else {
                mItems = new ArrayList<Film>();
            }
            mItems.addAll(data);
        }
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);

        void onItemLongClick(View v, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        private Film mItem;
        private final TextView mFilmAboutView;
        private final ImageView mFilmPoster;
        private final TextView mFilmReviews;
        private final TextView mFilmTitleView;
        private final TextView mFilmCreateDate;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mFilmPoster = (ImageView) view.findViewById(R.id.iv_list_poster);
            mFilmTitleView = (TextView) view.findViewById(R.id.tv_film_title);
            mFilmAboutView = (TextView) view.findViewById(R.id.tv_film_about);
            mFilmReviews = (TextView) view.findViewById(R.id.tv_reviews);
            mFilmCreateDate = (TextView) view.findViewById(R.id.tv_create_date);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
