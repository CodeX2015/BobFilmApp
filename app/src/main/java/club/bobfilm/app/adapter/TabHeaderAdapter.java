package club.bobfilm.app.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import club.bobfilm.app.entity.Section;
import club.bobfilm.app.fragment.FragmentVideo;

/**
 * Created by CodeX on 21.04.2016.
 */
public class TabHeaderAdapter extends FragmentStatePagerAdapter {

    static Logger log = LoggerFactory.getLogger(TabHeaderAdapter.class);
    private List<Section> mSections;
    private Context mContext;

    public TabHeaderAdapter(FragmentManager fm, List<Section> sections, Context context) {
        super(fm);
        this.mSections = sections;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mSections.size();
    }

    @Override
    public Fragment getItem(int i) {
        return FragmentVideo.newInstance(mSections.get(i));
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mSections.get(position).getSectionTitle().toUpperCase();
    }
}