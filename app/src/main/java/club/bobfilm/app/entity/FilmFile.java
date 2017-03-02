package club.bobfilm.app.entity;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

import club.bobfilm.app.helpers.DBHelper;
import club.bobfilm.app.helpers.BobFilmParser;
import club.bobfilm.app.service.DownloadService;

/**
 * Created by CodeX on 26.04.2016.
 */
public class FilmFile implements Serializable, DBHelper.IDbData {
    public int idL;
    public int id;
    private String mFileName;
    @SerializedName("comment")
    private String mFileComment;
    @SerializedName("file")
    private String mFileUrl;
    private String mLightFileUrl;
    private String mLightFileName;
    private boolean isLightVersionChoice = false;
    private String mFileLogoUrl;
    private long mFileSize = 0;
    private long mAlreadyDownload = 0;
    private String mDownloadTimeDate = "";
    private String mDownloadTime = "";
    private Date mDownloadDate;
    private int mProgressValue = 0;
    private String mFilePath;
    private boolean isDownloadComplete = false;
    private boolean isViewed = false;
    private DownloadService.DownloadStatuses status = DownloadService.DownloadStatuses.DEFAULT;
    private String downloadPerSize;
    private String mFilmUrl;

    private String mFilmTitle;
    private String mFilmLogoUrl;
    private boolean mFilmBookmarked;
    public boolean isPaused;

    public FilmFile() {
    }

    public FilmFile(String fileName, String urlFile) {
        this.mFileName = fileName;
        this.mFileUrl = urlFile;
        this.id = fileName.hashCode();
    }

    public FilmFile(String fileName, String urlFile, Film film) {
        this.mFileName = fileName;
        this.mFileUrl = urlFile;
        this.mFilmTitle = film.getFilmTitle();
        this.mFilmUrl = film.getFilmUrl();
        this.mFilmLogoUrl = film.getPosterUrl();
        this.mFilmBookmarked = film.isBookmarked();
        this.id = fileName.hashCode();
    }

//    public FilmFile(String fileName, String urlFile, String urlLogo, String urlLightFileUrl) {
//        this.mFileName = fileName;
//        this.mFileUrl = urlFile;
//        this.mLightFileUrl = urlLightFileUrl;
//        this.mFileLogoUrl = urlLogo;
//        this.id = fileName.hashCode();
//    }

    public FilmFile(String fileName, String urlFile, String urlLogo, String urlLightFileUrl, String urlLightFileName) {
        this.mFileName = fileName;
        this.mFileUrl = urlFile;
        this.mLightFileUrl = urlLightFileUrl;
        this.mFileLogoUrl = urlLogo;
        this.mLightFileName = urlLightFileName;
        this.id = fileName.hashCode();
        this.idL = urlLightFileName.hashCode();
    }

    public FilmFile(String fileName, String urlFile, String urlLogo) {
        this.mFileName = fileName;
        this.mFileUrl = urlFile;
        this.mFileLogoUrl = urlLogo;
        this.id = fileName.hashCode();
    }

    public FilmFile(String fileName, String urlFile, boolean isLightVersionChoice) {
        mFileName = fileName;
        mFileUrl = urlFile;
        this.isLightVersionChoice = isLightVersionChoice;
        id = fileName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return getClass() == obj.getClass() && this.id == ((FilmFile) obj).id;
    }

    public boolean isLightVersionChoice() {
        return isLightVersionChoice;
    }

    public void setLightVersionChoice(boolean lightVersionChoice) {
        isLightVersionChoice = lightVersionChoice;
    }

    public String getFileComment() {
        return mFileComment;
    }

    public void setFileComment(String fileComment) {
        this.mFileComment = fileComment;
    }

    public String getFilmTitle() {
        return mFilmTitle;
    }

    public void setFilmTitle(String mFilmTitle) {
        this.mFilmTitle = mFilmTitle;
    }

    public String getFilmLogoUrl() {
        return mFilmLogoUrl;
    }

    public void setFilmLogoUrl(String mFilmLogoUrl) {
        this.mFilmLogoUrl = mFilmLogoUrl;
    }

    public boolean isFilmBookmarked() {
        return mFilmBookmarked;
    }

    public void setFilmBookmarked(boolean mFilmBookmarked) {
        this.mFilmBookmarked = mFilmBookmarked;
    }

    public String getFilmUrl() {
        return mFilmUrl;
    }

    public void setFilmUrl(String mFilmUrl) {
        this.mFilmUrl = mFilmUrl;
    }

    public String getLightFileUrl() {
        return mLightFileUrl;
    }

    public void setLightFileUrl(String mLightFileUrl) {
        this.mLightFileUrl = mLightFileUrl;
    }

    public String getLightFileName() {
        return mLightFileName;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String mFilePath) {
        this.mFilePath = mFilePath;
    }

    public boolean isViewed() {
        return isViewed;
    }

    public void setViewed(boolean viewed) {
        isViewed = viewed;
    }

    public String getDownloadTime() {
        return mDownloadTime;
    }

    public void setDownloadTime(String mDownloadTime) {
        this.mDownloadTime = mDownloadTime;
    }

    public Date getDownloadDate() {
        return mDownloadDate;
    }

    public void setDownloadDate(Date mDownloadDate) {
        this.mDownloadDate = mDownloadDate;
    }

    public long getAlreadyDownload() {
        return mAlreadyDownload;
    }

    public void setAlreadyDownload(long mAlreadyDownload) {
        this.mAlreadyDownload = mAlreadyDownload;
    }

    public int getProgressValue() {
        return mProgressValue;
    }

    public void setProgressValue(int mProgressValue) {
        this.mProgressValue = mProgressValue;
    }

    public String getDownloadTimeDate() {
        return mDownloadTimeDate;
    }

    public void setDownloadTimeDate(String mDownloadTimeDate) {
        this.mDownloadTimeDate = mDownloadTimeDate;
    }

    public long getFileSize() {
        return mFileSize;
    }

    public void setFileSize(long mFileSize) {
        this.mFileSize = mFileSize;
    }


    public String getFileLogoUrl() {
        return mFileLogoUrl;
    }

    public void setFileLogoUrl(String mFileLogoUrl) {
        this.mFileLogoUrl = mFileLogoUrl;
    }

    public boolean isDownloadComplete() {
        return isDownloadComplete;
    }

    public void setDownloadComplete(boolean downloadComplete) {
        isDownloadComplete = downloadComplete;
        if (downloadComplete) {
            mProgressValue = 100;
        }
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String fileName) {
        this.mFileName = fileName;
        this.id = fileName.hashCode();
    }

    public String getFileUrl() {
        if (!mFileUrl.contains("http")) return BobFilmParser.mSite + mFileUrl;
        return mFileUrl;
    }

    public void setFileUrl(String mFileUrl) {
        this.mFileUrl = mFileUrl;
    }

    public void setStatus(DownloadService.DownloadStatuses status) {
        this.status = status;
    }

    public int getIntStatus() {
        switch (status) {
            case DEFAULT:
                return -1;
            case STARTED:
                return 2;
            case CONNECTING:
                return 3;
            case DOWNLOADING:
                return 4;
            case COMPLETE:
                return 5;
            case FAILED:
                return 6;
            case PAUSED:
                return 7;
            case CANCELED:
                return 8;
            default:
                return -1;
        }
    }

    public void setStatusFromInt(int status) {
        switch (status) {
            case -1:
                this.status = DownloadService.DownloadStatuses.DEFAULT;
                break;
            case 2:
                this.status = DownloadService.DownloadStatuses.STARTED;
                break;
            case 3:
                this.status = DownloadService.DownloadStatuses.CONNECTING;
                break;
            case 4:
                this.status = DownloadService.DownloadStatuses.DOWNLOADING;
                break;
            case 5:
                this.status = DownloadService.DownloadStatuses.COMPLETE;
                break;
            case 6:
                this.status = DownloadService.DownloadStatuses.FAILED;
                break;
            case 7:
                this.status = DownloadService.DownloadStatuses.PAUSED;
                break;
            case 8:
                this.status = DownloadService.DownloadStatuses.CANCELED;
                break;
            default:
                this.status = DownloadService.DownloadStatuses.DEFAULT;
                break;
        }
    }

    public DownloadService.DownloadStatuses getStatus() {
        return status;
    }

    public void setDownloadPerSize(String downloadPerSize) {
        this.downloadPerSize = downloadPerSize;
    }

    public String getDownloadPerSize() {
        return downloadPerSize;
    }

    @Override
    public void fillItSelf(Cursor cursor) {
        this.setFileName(cursor.getString(1));
        this.setFileUrl(cursor.getString(2));
        this.setFileLogoUrl(cursor.getString(3));
        this.setFileSize(Long.parseLong(cursor.getString(4)));
        this.setDownloadTimeDate(cursor.getString(5));
        this.setDownloadComplete(cursor.getInt(6) == 1);
        this.setFilmUrl(cursor.getString(7));
        this.setFilmTitle(cursor.getString(8));
        this.setFilmBookmarked(cursor.getInt(9) == 1);
        this.setFilmLogoUrl(cursor.getString(10));
        this.setViewed(cursor.getInt(11) == 1);
        this.setFilePath(cursor.getString(12));
        this.setStatusFromInt(cursor.getInt(13));
        this.setProgressValue(cursor.getInt(14));
        this.setDownloadPerSize(cursor.getString(15));
    }

    @Override
    public ContentValues fillItemForDB() {
        ContentValues values = new ContentValues();
        values.put("file_name", this.getFileName());
        values.put("file_url", this.getFileUrl());
        values.put("file_logo_url", this.getFileLogoUrl());
        values.put("file_size", this.getFileSize());
        values.put("file_download_date", this.getDownloadTimeDate());
        values.put("is_download_complete", this.isDownloadComplete() ? 1 : 0);
        values.put("film_url", this.getFilmUrl());
        values.put("film_title", this.getFilmTitle());
        values.put("film_is_bookmarked", this.isFilmBookmarked() ? 1 : 0);
        values.put("film_logo_url", this.getFilmLogoUrl());
        values.put("is_viewed", this.isViewed());
        values.put("file_path", this.getFilePath());
        values.put("download_status", this.getIntStatus());
        values.put("download_progress", this.getProgressValue());
        values.put("downloaded_size", this.getDownloadPerSize());
        return values;
    }
}
