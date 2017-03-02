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
    private List<Comment> mFilmComments;
    private boolean isBookmarked = false;

    public String getFilmReviewsUrl() {
        return mFilmReviewsUrl;
    }

    public void setFilmReviewsUrl(String mFilmReviewsUrl) {
        this.mFilmReviewsUrl = mFilmReviewsUrl;
    }

    public List<Comment> getFilmComments() {
        return mFilmComments;
    }

    public void setFilmComments(List<Comment> filmComments) {
        this.mFilmComments = filmComments;
    }

    public String getFilmUrl() {
        return mFilmUrl;
    }

    public void setFilmUrl(String mFilmUrl) {
        this.mFilmUrl = mFilmUrl;
    }

    public List<FilmFile> getFilmFiles() {
        return mFilmFiles;
    }

    public void setFilmFiles(List<FilmFile> mFilmFiles) {
        this.mFilmFiles = mFilmFiles;
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        isBookmarked = bookmarked;
    }

    public String getFilmDetailsHTML() {
        if (mFilmDetailsHTML == null) {return "";}
        return mFilmDetailsHTML;
    }

    public String getPosterUrl() {
        return mPosterUrl;
    }

    public void setPosterUrl(String mPosterUrl) {
        this.mPosterUrl = mPosterUrl;
    }

    public void setFilmDetailsHTML(String mFilmDetailsHTML) {
        this.mFilmDetailsHTML = mFilmDetailsHTML;
    }

    public String getBigPosterUrl() {
        return mBigPosterUrl;
    }

    public void setBigPosterUrl(String mBigPosterUrl) {
        this.mBigPosterUrl = mBigPosterUrl;
    }

    public String getFilmTitle() {
        return mFilmTitle;
    }

    public void setFilmTitle(String mFilmTitle) {
        this.mFilmTitle = mFilmTitle;
    }

    public String getFilmCreateDate() {
        return mFilmCreateDate;
    }

    public void setFilmCreateDate(String mFilmCreateDate) {
        this.mFilmCreateDate = mFilmCreateDate;
    }

    public String getFilmReviews() {
        return mFilmReviews;
    }

    public void setFilmReviews(String mFilmReviews) {

        this.mFilmReviews = mFilmReviews;
    }

    public String getFilmYear() {
        return mFilmYear;
    }

    public void setFilmYear(String mFilmYear) {
        this.mFilmYear = mFilmYear;
    }

}