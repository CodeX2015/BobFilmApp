package club.bobfilm.app.entity;

import android.content.ContentValues;
import android.database.Cursor;

import java.io.Serializable;
import java.util.Date;

import club.bobfilm.app.helpers.DBHelper;
import club.bobfilm.app.helpers.HTMLParser;
import club.bobfilm.app.service.DownloadService;

/**
 * Created by CodeX on 26.04.2016.
 */
public class FilmFile implements Serializable, DBHelper.IDbData {
    public int idL;
    public int id;
    private String mFileName;
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

    public FilmFile(String fileName, String urlFile, Film film) {
        this.mFileName = fileName;
        this.mFileUrl = urlFile;
        this.mFilmTitle = film.getFilmTitle();
        this.mFilmUrl = film.getFilmUrl();
        this.mFilmLogoUrl = film.getPosterUrl();
        this.mFilmBookmarked = film.isBookmarked();
        this.id = fileName.hashCode();
    }

    public FilmFile(String fileName, String urlFile) {
        this.mFileName = fileName;
        this.mFileUrl = urlFile;
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

    public String getmFilmTitle() {
        return mFilmTitle;
    }

    public void setmFilmTitle(String mFilmTitle) {
        this.mFilmTitle = mFilmTitle;
    }

    public String getmFilmLogoUrl() {
        return mFilmLogoUrl;
    }

    public void setmFilmLogoUrl(String mFilmLogoUrl) {
        this.mFilmLogoUrl = mFilmLogoUrl;
    }

    public boolean ismFilmBookmarked() {
        return mFilmBookmarked;
    }

    public void setmFilmBookmarked(boolean mFilmBookmarked) {
        this.mFilmBookmarked = mFilmBookmarked;
    }

    public String getmFilmUrl() {
        return mFilmUrl;
    }

    public void setmFilmUrl(String mFilmUrl) {
        this.mFilmUrl = mFilmUrl;
    }

    public String getmLightFileUrl() {
        return mLightFileUrl;
    }

    public void setmLightFileUrl(String mLightFileUrl) {
        this.mLightFileUrl = mLightFileUrl;
    }

    public String getmFilePath() {
        return mFilePath;
    }

    public void setmFilePath(String mFilePath) {
        this.mFilePath = mFilePath;
    }

    public boolean isViewed() {
        return isViewed;
    }

    public void setViewed(boolean viewed) {
        isViewed = viewed;
    }

    public String getmDownloadTime() {
        return mDownloadTime;
    }

    public void setmDownloadTime(String mDownloadTime) {
        this.mDownloadTime = mDownloadTime;
    }

    public Date getmDownloadDate() {
        return mDownloadDate;
    }

    public void setmDownloadDate(Date mDownloadDate) {
        this.mDownloadDate = mDownloadDate;
    }

    public long getmAlreadyDownload() {
        return mAlreadyDownload;
    }

    public void setmAlreadyDownload(long mAlreadyDownload) {
        this.mAlreadyDownload = mAlreadyDownload;
    }

    public int getmProgressValue() {
        return mProgressValue;
    }

    public void setmProgressValue(int mProgressValue) {
        this.mProgressValue = mProgressValue;
    }

    public String getmDownloadTimeDate() {
        return mDownloadTimeDate;
    }

    public void setmDownloadTimeDate(String mDownloadTimeDate) {
        this.mDownloadTimeDate = mDownloadTimeDate;
    }

    public long getmFileSize() {
        return mFileSize;
    }

    public void setmFileSize(long mFileSize) {
        this.mFileSize = mFileSize;
    }


    public String getmFileLogoUrl() {
        return mFileLogoUrl;
    }

    public void setmFileLogoUrl(String mFileLogoUrl) {
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

    public String getmFileName() {
        return mFileName;
    }

    public void setmFileName(String fileName) {
        this.mFileName = fileName;
        this.id = fileName.hashCode();
    }

    public String getmFileUrl() {
        if (!mFileUrl.contains("http")) return HTMLParser.SITE + mFileUrl;
        return mFileUrl;
    }

    public void setmFileUrl(String mFileUrl) {
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
        this.setmFileName(cursor.getString(1));
        this.setmFileUrl(cursor.getString(2));
        this.setmFileLogoUrl(cursor.getString(3));
        this.setmFileSize(Long.parseLong(cursor.getString(4)));
        this.setmDownloadTimeDate(cursor.getString(5));
        this.setDownloadComplete(cursor.getInt(6) == 1);
        this.setmFilmUrl(cursor.getString(7));
        this.setmFilmTitle(cursor.getString(8));
        this.setmFilmBookmarked(cursor.getInt(9) == 1);
        this.setmFilmLogoUrl(cursor.getString(10));
        this.setViewed(cursor.getInt(11) == 1);
        this.setmFilePath(cursor.getString(12));
        this.setStatusFromInt(cursor.getInt(13));
        this.setmProgressValue(cursor.getInt(14));
        this.setDownloadPerSize(cursor.getString(15));
    }

    @Override
    public ContentValues fillItemForDB() {
        ContentValues values = new ContentValues();
        values.put("file_name", this.getmFileName());
        values.put("file_url", this.getmFileUrl());
        values.put("file_logo_url", this.getmFileLogoUrl());
        values.put("file_size", this.getmFileSize());
        values.put("file_download_date", this.getmDownloadTimeDate());
        values.put("is_download_complete", this.isDownloadComplete() ? 1 : 0);
        values.put("film_url", this.getmFilmUrl());
        values.put("film_title", this.getmFilmTitle());
        values.put("film_is_bookmarked", this.ismFilmBookmarked() ? 1 : 0);
        values.put("film_logo_url", this.getmFilmLogoUrl());
        values.put("is_viewed", this.isViewed());
        values.put("file_path", this.getmFilePath());
        values.put("download_status", this.getIntStatus());
        values.put("download_progress", this.getmProgressValue());
        values.put("downloaded_size", this.getDownloadPerSize());
        return values;
    }

    public String getmLightFileName() {
        return mLightFileName;
    }
}
