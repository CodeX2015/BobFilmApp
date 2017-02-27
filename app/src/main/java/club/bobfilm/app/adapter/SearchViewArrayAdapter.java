package club.bobfilm.app.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

/**
 * Created by CodeX on 13.06.2016.
 */
public class SearchViewArrayAdapter extends SearchView {

    private SearchView.SearchAutoComplete mSearchAutoComplete;

    public SearchViewArrayAdapter(Context context) {
        super(context);
        initialize();
    }

    public SearchViewArrayAdapter(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public void initialize() {
        mSearchAutoComplete = (SearchAutoComplete) findViewById(android.support.v7.appcompat.R.id.search_src_text);
        this.setAdapter(null);
        this.setOnItemClickListener(null);
    }

    @Override
    public void setSuggestionsAdapter(CursorAdapter adapter) {
        // don't let anyone touch this
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        mSearchAutoComplete.setOnItemClickListener(listener);
    }

    public void setAdapter(ArrayAdapter<?> adapter) {
        mSearchAutoComplete.setAdapter(adapter);
    }

    public void setText(String text) {
        mSearchAutoComplete.setText(text);
    }

    public void setDropDownVerticalOffset(int value) {
        mSearchAutoComplete.setDropDownVerticalOffset(value);
    }

    public void setDropDownAnchor(int value) {
        mSearchAutoComplete.setDropDownAnchor(value);
    }

    public void setDropDownWidth(int value) {
        mSearchAutoComplete.setDropDownWidth(value);
    }

    public void setDropDownBackgroundResource(Drawable value) {
        mSearchAutoComplete.setDropDownBackgroundDrawable(value);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mSearchAutoComplete.getLayoutParams();
        params.setMargins(0, 0, 0, 0); //substitute parameters for left, top, right, bottom
        mSearchAutoComplete.setLayoutParams(params);
    }

}
