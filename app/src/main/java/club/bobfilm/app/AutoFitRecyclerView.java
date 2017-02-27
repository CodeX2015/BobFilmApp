package club.bobfilm.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AutoFitRecyclerView extends RecyclerView {

    private Logger log = LoggerFactory.getLogger(AutoFitRecyclerView.class);
    private MarginDecoration mDecoration = null;
    private LinearLayoutManager linearManager;
    private GridLayoutManager gridManager;
    public int mDefaultPadding = 0;
    private int mColumnWidth = -1;
    int mPrevMargin = 0;

    public AutoFitRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public AutoFitRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AutoFitRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            int[] attrsArray = {
                    android.R.attr.columnWidth
            };
            TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
            mColumnWidth = array.getDimensionPixelSize(0, -1);
            array.recycle();
        }
    }

    private void fitCenterColumns(int spanCount) {
        int fullWidth = getMeasuredWidth();
        if (fullWidth > 0) {
            int offSetFreeSpace = fullWidth - mColumnWidth * spanCount;
            float offSetForItem = offSetFreeSpace / (spanCount + 1);
            int shiftFactor = 1;
            mDefaultPadding = Math.round((int) offSetForItem / shiftFactor);
            //округление до десяток
//            mDefaultPadding = (Math.round((offSetForItem + 5)/10))*10;
//            (int) Utils.pxToDp(offSetForItem, getContext());
            if (mDefaultPadding > 0 && mPrevMargin != mDefaultPadding) {
                log.warn("measuredWidth = {}, mColumnWidth = {}, offsetFreeSpace = {}, padding = {}",
                        fullWidth, mColumnWidth, (fullWidth - spanCount * mColumnWidth),
                        mDefaultPadding);
                mPrevMargin = mDefaultPadding;
                mDecoration = new MarginDecoration(getContext(), 0, 0, 0, mDefaultPadding);
                super.addItemDecoration(mDecoration);
            }
            super.setPadding(mDefaultPadding, mDefaultPadding, 0, 0);
        } else {
            int defaultValue = 8;
            super.setPadding(defaultValue, defaultValue, 0, 0);
            if (mDecoration == null) {
                mDecoration = new MarginDecoration(getContext(), 0, 0, 0, defaultValue);
                super.addItemDecoration(mDecoration);
            }
        }
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        if (layout instanceof GridLayoutManager) {
            gridManager = (GridLayoutManager) layout;
        } else {
            gridManager = null;
        }
        super.setLayoutManager(layout);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (gridManager != null) {
            int spanCount = Math.max(1, getMeasuredWidth() / mColumnWidth);
            int minOffset = 10;
            if (getMeasuredWidth() - spanCount * mColumnWidth < mColumnWidth / minOffset) {
                spanCount--;
            }
            if (BuildConfig.DEBUG) log.info("spanCount is {}", spanCount);
            spanCount = spanCount < 1 ? 1 : spanCount;
            gridManager.setSpanCount(spanCount);
            fitCenterColumns(spanCount);
        }
    }

    class MarginDecoration extends RecyclerView.ItemDecoration {
        private final int marginTop;
        private final int marginBottom;
        private final int marginRight;
        private final int marginLeft;
        private final int margin;

        MarginDecoration(Context context, int left, int top, int right, int bottom) {
            marginLeft = left;
            marginTop = top;
            marginRight = right;
            marginBottom = bottom;
            margin = context.getResources().getDimensionPixelSize(R.dimen.item_offset);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.set(marginLeft, marginTop, marginRight, marginBottom);
        }
    }
}
