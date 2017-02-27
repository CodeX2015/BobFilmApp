package club.bobfilm.app.entity;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;

import java.io.Serializable;

import club.bobfilm.app.helpers.DBHelper;

/**
 * Created by CodeX on 22.04.2016.
 */
public class Film implements Serializable, DBHelper.IDbData {

    private Bitmap mPoster;
    private String mPosterUrl;
    private String mFilmTitle;
    private String mFilmUrl;
    private String mCreateDate;
    private String mFilmAbout;
    private String mReviews;
    private String mReviewsUrl;
    private String mNextPageUrl;

    private boolean mHasArticle = false;
    private boolean mBookmarked = false;

    public Film() {
    }

    public Film(String filmTitle, String filmUrl, String filmLogoUrl, boolean filmBookmarked) {
        this.mFilmTitle = filmTitle;
        this.mFilmUrl = filmUrl;
        this.mPosterUrl = filmLogoUrl;
        this.mBookmarked = filmBookmarked;
    }

    public boolean isHasArticle() {
        return mHasArticle;
    }

    public void setHasArticle(boolean mHasArticle) {
        this.mHasArticle = mHasArticle;
    }

    public String getReviewsUrl() {
        return mReviewsUrl;
    }

    public void setReviewsUrl(String mReviewsUrl) {
        this.mReviewsUrl = mReviewsUrl;
    }

    public String getFilmAbout() {
        return mFilmAbout;
    }

    public void setFilmAbout(String mFilmAbout) {
        this.mFilmAbout = mFilmAbout;
    }

    public String getNextPageUrl() {
        return mNextPageUrl;
    }

    public void setNextPageUrl(String mNextPageUrl) {
        this.mNextPageUrl = mNextPageUrl;
    }

    public boolean isBookmarked() {
        return mBookmarked;
    }

    public void setBookmarked(boolean mBookmarked) {
        this.mBookmarked = mBookmarked;
    }

    public Bitmap getPoster() {
        return mPoster;
    }

    public void setPoster(Bitmap mPoster) {
        this.mPoster = mPoster;
    }

    public String getPosterUrl() {
        if (mPosterUrl.equalsIgnoreCase("")) {
            return null;
        }
        return mPosterUrl;
    }

    public void setPosterUrl(String mPosterUrl) {
        this.mPosterUrl = mPosterUrl;
    }

    public String getFilmTitle() {
        return mFilmTitle;
    }

    public void setFilmTitle(String mFilmTitle) {
        this.mFilmTitle = mFilmTitle;
    }

    public String getFilmUrl() {
        return mFilmUrl;
    }

    public void setFilmUrl(String mFilmUrl) {
        this.mFilmUrl = mFilmUrl;
    }

    public String getCreateDate() {
        return mCreateDate;
    }

    public void setCreateDate(String mCreateDate) {
        this.mCreateDate = mCreateDate;
    }

    public String getReviews() {
        return mReviews;
    }

    public void setReviews(String mReviews) {
        this.mReviews = mReviews;
    }

    @Override
    public void fillItSelf(Cursor cursor) {
        this.setFilmTitle(cursor.getString(1));
        this.setFilmUrl(cursor.getString(2));
        this.setBookmarked(cursor.getInt(3) == 1);
        this.setPosterUrl(cursor.getString(4));
    }

    @Override
    public ContentValues fillItemForDB() {
        ContentValues values = new ContentValues();
        values.put("film_name", this.getFilmTitle());
        values.put("film_url", this.getFilmUrl());
        values.put("is_bookmarked", this.isBookmarked());
        values.put("film_logo_url", this.getPosterUrl());
        return values;
    }
}
