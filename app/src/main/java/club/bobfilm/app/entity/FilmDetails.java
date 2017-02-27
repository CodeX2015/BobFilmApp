package club.bobfilm.app.entity;

import java.io.Serializable;
import java.util.List;

/**
 * Created by CodeX on 22.04.2016.
 */
public class FilmDetails implements Serializable {
    private String mBigPosterUrl;
    private String mPosterUrl;
    private String mFilmUrl;
    private String mFilmTitle;
    private String mFilmCreateDate;
    private String mFilmReviews;
    private String mFilmReviewsUrl;
    private String mFilmYear;
    private String mFilmDetailsHTML;
    private List<FilmFile> mFilmFiles;
    private boolean isBookmarked = false;


    public String getmFilmReviewsUrl() {
        return mFilmReviewsUrl;
    }

    public void setmFilmReviewsUrl(String mFilmReviewsUrl) {
        this.mFilmReviewsUrl = mFilmReviewsUrl;
    }

    public String getmFilmUrl() {
        return mFilmUrl;
    }

    public void setmFilmUrl(String mFilmUrl) {
        this.mFilmUrl = mFilmUrl;
    }

    public List<FilmFile> getmFilmFiles() {
        return mFilmFiles;
    }

    public void setmFilmFiles(List<FilmFile> mFilmFiles) {
        this.mFilmFiles = mFilmFiles;
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        isBookmarked = bookmarked;
    }

    public String getmFilmDetailsHTML() {
        if (mFilmDetailsHTML == null) {return "";}
        return mFilmDetailsHTML;
    }

    public String getmPosterUrl() {
        return mPosterUrl;
    }

    public void setmPosterUrl(String mPosterUrl) {
        this.mPosterUrl = mPosterUrl;
    }

    public void setmFilmDetailsHTML(String mFilmDetailsHTML) {
        this.mFilmDetailsHTML = mFilmDetailsHTML;
    }

    public String getmBigPosterUrl() {
        return mBigPosterUrl;
    }

    public void setmBigPosterUrl(String mBigPosterUrl) {
        this.mBigPosterUrl = mBigPosterUrl;
    }

    public String getmFilmTitle() {
        return mFilmTitle;
    }

    public void setmFilmTitle(String mFilmTitle) {
        this.mFilmTitle = mFilmTitle;
    }

    public String getmFilmCreateDate() {
        return mFilmCreateDate;
    }

    public void setmFilmCreateDate(String mFilmCreateDate) {
        this.mFilmCreateDate = mFilmCreateDate;
    }

    public String getmFilmReviews() {
        return mFilmReviews;
    }

    public void setmFilmReviews(String mFilmReviews) {
        this.mFilmReviews = mFilmReviews;
    }

    public String getmFilmYear() {
        return mFilmYear;
    }

    public void setmFilmYear(String mFilmYear) {
        this.mFilmYear = mFilmYear;
    }

}
