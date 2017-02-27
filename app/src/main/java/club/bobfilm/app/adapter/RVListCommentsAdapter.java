package club.bobfilm.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.entity.Comment;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 03.05.2016.
 */
public class RVListCommentsAdapter extends RecyclerView.Adapter<RVListCommentsAdapter.ViewHolder> {

    Logger log = LoggerFactory.getLogger(RVListCommentsAdapter.class);
    private List<Comment> mItems = null;
    private Context mContext = null;
    private RVListCommentsAdapter.OnItemClickListener mListener;

    public RVListCommentsAdapter(Context context, List<Comment> items, OnItemClickListener listener) {
        mContext = context;
        mItems = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_comment, parent, false);
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
        //todo
        holder.mItem = mItems.get(position);
        try {
            int avatarWidth = (int) mContext.getResources()
                    .getDimension(R.dimen.comment_avatar_width);
            int avatarHeight = (int) mContext.getResources()
                    .getDimension(R.dimen.comment_avatar_height);
            if (holder.mItem.isReplica()) {
                holder.mItem.setLeftOffset("20");
                avatarWidth = avatarWidth / 2;
                avatarHeight = avatarHeight / 2;
            }
            holder.mAvatar.getLayoutParams().width = avatarWidth;
            holder.mAvatar.getLayoutParams().height = avatarHeight;

            RecyclerView.LayoutParams relativeParams = (RecyclerView.LayoutParams)
                    holder.mCommentLayoutView.getLayoutParams();
            relativeParams.setMargins(holder.mItem.getLeftOffset(), 0, 0, 0); // left, top, right, bottom
            holder.mCommentLayoutView.setLayoutParams(relativeParams);
        } catch (Exception ex) {
            log.warn("error", ex);
        }

        Utils.setImageViewBitmap(mContext, holder.mItem.getAvatarUrl(), holder.mAvatar, null);
        holder.mUserView.setText(Utils.underlineText(mItems.get(position).getUser()));
        holder.mUserView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = holder.getAdapterPosition();
                if (mListener != null) {
                    mListener.onItemClick(v, index);
                }
            }
        });
        holder.mCommentTitleView.setText(Utils.underlineText(holder.mItem.getCommentTitle()));
        holder.mCommentTitleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = holder.getAdapterPosition();
                if (mListener != null) {
                    mListener.onItemClick(v, index);
                }
            }
        });

        holder.mCommentBodyView.setText(Html.fromHtml(holder.mItem.getCommentBodyHTML()));
        holder.mCreateDate.setText(Utils.convertDate(mItems.get(position).getCreateDate(),
                "hh:mm, dd MMMMM yyyy", "hh:mm, dd.MM.yyyy"));

    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);

        void onItemLongClick(View v, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        private Comment mItem;
        private final RelativeLayout mCommentLayoutView;
        private final TextView mCommentBodyView;
        private final ImageView mAvatar;
        private final TextView mCreateDate;
        private final TextView mUserView;
        private final TextView mCommentTitleView;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mCommentLayoutView = (RelativeLayout) view.findViewById(R.id.ll_comment_item);
            mAvatar = (ImageView) view.findViewById(R.id.civ_comment_avatar);
            mCommentTitleView = (TextView) view.findViewById(R.id.tv_comment_title);
            mCommentBodyView = (TextView) view.findViewById(R.id.tv_comment_body);
            mUserView = (TextView) view.findViewById(R.id.tv_comment_user);
            mCreateDate = (TextView) view.findViewById(R.id.tv_comment_create_date);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
