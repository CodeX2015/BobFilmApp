package club.bobfilm.app.entity;

import java.io.Serializable;
import java.util.List;

/**
 * Created by CodeX on 22.04.2016.
 */
public class Section implements Serializable {

    private int mSectionPosition;
    private String mSectionTitle;
    private String mSectionUrl;
    private List<Film> mFilms;
    private String mSearchId;
    private String mNextPageUrl = "";

    public Section() {
    }

    public Section(String sectionTitle, String searchId ) {
        mSectionTitle = sectionTitle;
        mSearchId = searchId;
    }

    public Section(String nextPageUrl) {
        this.mNextPageUrl = nextPageUrl;
    }

    public int getmSectionPosition() {
        return mSectionPosition;
    }

    public void setmSectionPosition(int mSectionPosition) {
        this.mSectionPosition = mSectionPosition;
    }

    public String getNextPageUrl() {
        return mNextPageUrl;
    }

    public void setNextPageUrl(String mNextPageUrl) {
        this.mNextPageUrl = mNextPageUrl;
    }

    public String getSearchId() {
        return "original_id=" + mSearchId;
    }

    public void setSearchId(String mSearchId) {
        this.mSearchId = mSearchId;
    }

    public String getSectionUrl() {
        return mSectionUrl;
    }

    public void setSectionUrl(String mSectionUrl) {
        this.mSectionUrl = mSectionUrl;
    }

    public String getSectionTitle() {
        return mSectionTitle;
    }

    public void setSectionTitle(String mSectionTitle) {
        this.mSectionTitle = mSectionTitle;
    }

    public List<Film> getFilms() {
        return mFilms;
    }

    public void setFilms(List<Film> mFilms) {
        this.mFilms = mFilms;
    }
}
