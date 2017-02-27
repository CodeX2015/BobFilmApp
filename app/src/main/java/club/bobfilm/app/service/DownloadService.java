package club.bobfilm.app.service;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.activity.ActivitySettings;
import club.bobfilm.app.activity.ActivityTabArchive;
import club.bobfilm.app.entity.FilmFile;
import club.bobfilm.app.helpers.DBHelper;
import club.bobfilm.app.helpers.downloader.CallBack;
import club.bobfilm.app.helpers.downloader.DownloadException;
import club.bobfilm.app.helpers.downloader.DownloadManager;
import club.bobfilm.app.helpers.downloader.DownloadRequest;
import club.bobfilm.app.util.Utils;


/**
 * Created by CodeX on 15/7/28.
 */
public class DownloadService extends Service {

    private static final String TAG = DownloadService.class.getSimpleName() + " {}";

    public static final String ACTION_DOWNLOAD_BROADCAST = "ua.ex.toseex:action_download_broadcast";
    private static final String ACTION_NOTIFICATION_BROADCAST = "ua.ex.toseex:action_download_pause_broadcast";

    public static final String ACTION_DOWNLOAD = "ua.ex.toseex:action_download";
    public static final int FLAG_DOWNLOAD_FILE = 101;

    public static final String ACTION_PAUSE = "ua.ex.toseex:action_pause";

    public static final String ACTION_CANCEL = "ua.ex.toseex:action_cancel";

    public static final String ACTION_PAUSE_ALL = "ua.ex.toseex:action_pause_all";

    public static final String ACTION_CANCEL_ALL = "ua.ex.toseex:action_cancel_all";

    public static final String EXTRA_FILE_INFO = "extra_file_info";
    private static final String EXTRA_FILE_URL = "extra_file_url";

    NotificationManagerCompat mNotificationManager;
    NotificationCompat.Builder mBuilder;

    private static Logger log = LoggerFactory.getLogger(DownloadService.class);
    private java.io.File mDownloadDirectory;

    private DownloadManager mDownloadManager;
    private LinkedHashMap<String, DownloadCallBack> mDownloadCallBacks = new LinkedHashMap<>();
    private LinkedHashMap<Integer, PendingIntent> mSwitchedNotification = new LinkedHashMap<>();

    public static ArrayList<FilmFile> mDownloadingFiles = new ArrayList<>();
    private static PendingIntent mContentIntent;
    private BroadcastReceiver mReceiver;
    private int mPrevDownloadsCount;
    private boolean isGroupNotify;

    public enum DownloadStatuses {
        STARTED,
        CONNECTING,
        DOWNLOADING,
        PAUSED,
        FAILED,
        COMPLETE,
        CANCELED,
        DEFAULT
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDownloadManager = DownloadManager.getInstance();
        if (ActivitySettings.DownloadPath != null && !ActivitySettings.DownloadPath.equals("")) {
            mDownloadDirectory = new java.io.File(ActivitySettings.DownloadPath);
        } else {
            mDownloadDirectory = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        }
        if (mDownloadDirectory != null) {
            //noinspection ResultOfMethodCallIgnored
            mDownloadDirectory.mkdirs();
        }
        setNotificationActionReceiver();
        Intent intent = new Intent(this, ActivityTabArchive.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mContentIntent = PendingIntent.getActivity(this, ActivityTabArchive.FRAGMENT_DOWNLOADS,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        pauseAll();
        mDownloadingFiles.clear();
    }

    public static boolean isDownloading(FilmFile file) {
        if (file == null) {
            return false;
        }
        DownloadStatuses fileStatus = file.getStatus();
        return Utils.getIndexOfItem(mDownloadingFiles, file) != -1
                || fileStatus == DownloadStatuses.DOWNLOADING
                || fileStatus == DownloadStatuses.CONNECTING;
    }

    private void pause(FilmFile file) {
//        log.info("pause file {}, status {}",
//                file.getmFileName(), file.getStatus().toString());
        mDownloadManager.pause(file.getmFileName());
    }

    private void cancel(FilmFile file) {
//        log.info("cancel file {}, status isCanceled {}",
//                file.getmFileName(), (file.getStatus() == DownloadStatuses.CANCELED));
        mDownloadManager.cancel(file.getmFileName());
        DownloadCallBack callBack = mDownloadCallBacks.get(file.getmFileName());
        if (callBack != null) {
            callBack.onDownloadCanceled();
            mDownloadCallBacks.remove(file.getmFileName());
        }
    }

    private void pauseAll() {
//        log.info("pause all, files {}",mDownloadingFiles.size());
//                mDownloadManager.pauseAll();
    }

    private void cancelAll() {
        mDownloadManager.cancelAll();
    }

    public static void intentDownload(Context context, FilmFile file) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_DOWNLOAD);
        intent.putExtra(EXTRA_FILE_INFO, file);
        if (BuildConfig.DEBUG) {
//            log.info("Download \nfile: {}\nurl: {}\n",
//                    file.getmFileName(), file.getmFileUrl());
            context.startService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void intentPause(Context context, FilmFile file) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_PAUSE);
        intent.putExtra(EXTRA_FILE_INFO, file);
        context.startService(intent);
    }

    public static void intentCancel(Context context, FilmFile file) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_CANCEL);
        intent.putExtra(EXTRA_FILE_INFO, file);
        context.startService(intent);
    }

    public static void intentDownloadOneFile(Context context, String fileUrl) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_DOWNLOAD);
        intent.setFlags(FLAG_DOWNLOAD_FILE);
        intent.putExtra(EXTRA_FILE_URL, fileUrl);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            FilmFile file;
            if (intent.getFlags() == FLAG_DOWNLOAD_FILE) {
                //For download single update file
                String fileUrl = intent.getStringExtra(EXTRA_FILE_URL);
                String fileName = "update_toseex.apk";
                file = new FilmFile(fileName, fileUrl, false);
                log.info("Downloading {}, {}, {}",
                        file.getmFileName(), file.getmFileUrl(), file.isLightVersionChoice());
                downloadFile(file);
            } else {
                //For download video files
                file = (FilmFile) intent.getSerializableExtra(EXTRA_FILE_INFO);
                String fileName = "";
                if (file != null) {
                    fileName = file.getmFileName();
                }
                int filePosition = Utils.getIndexOfItem(mDownloadingFiles, file);
                String action = intent.getAction();
                try {
                    switch (action) {
                        case ACTION_DOWNLOAD:
                            if (filePosition == -1) {
                                mDownloadingFiles.add(file);
                            } else {
                                mDownloadingFiles.set(filePosition, file);
                            }
                            download(file);
                            break;
                        case ACTION_PAUSE:
                            pause(file);
                            break;
                        case ACTION_CANCEL:
                            cancel(file);
                            break;
                        case ACTION_PAUSE_ALL:
                            pauseAll();
                            break;
                        case ACTION_CANCEL_ALL:
                            cancelAll();
                            break;
                    }
                } catch (Exception ex) {
                    if (BuildConfig.DEBUG) {
                        ex.printStackTrace();
                    } else {
                        log.error(Utils.getErrorLogHeader() + new Object() {
                        }.getClass().getEnclosingMethod().getName(), ex);
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void download(final FilmFile file) {
        //set download path
        file.setmFilePath(mDownloadDirectory.getAbsolutePath() + "/" + file.getmFileName());

        final DownloadRequest request = new DownloadRequest.Builder()
                .setTitle(file.getmFileName())
                .setUri(file.getmFileUrl())
                .setFolder(mDownloadDirectory)
                .build();
//        log.info("real download: \nsetUri {}\nfilename {}\n",
//                request.getUri(), request.getTitle());
        DownloadCallBack mDownloadCallBack = new DownloadCallBack(file);
        mDownloadCallBacks.put(file.getmFileName(), mDownloadCallBack);
        mDownloadManager.download(request, file.getmFileName(), mDownloadCallBack);
    }


    private void downloadFile(final FilmFile file) {
        final DownloadRequest request = new DownloadRequest.Builder()
                .setTitle(file.getmFileName())
                .setUri(file.getmFileUrl())
                .setFolder(mDownloadDirectory)
                .build();
        DownloadFileCallBack mDownloadFileCallBack = new DownloadFileCallBack(file);

        mDownloadManager.download(request, file.getmFileName(), mDownloadFileCallBack);
//        log.warn("download.setUri {} , filename {}",
//                file.getmFileUrl(), file.getmFileName());
    }

    private void updateStockNotify(int smallIcon, String contentTitle,
                                   String contentText, Progress progress,
                                   String ticker, PendingIntent contentIntent,
                                   boolean onGoing, boolean autoCancel,
                                   boolean addAction, boolean when, FilmFile file) {
        if (mNotificationManager == null) {
            mNotificationManager = NotificationManagerCompat.from(getApplicationContext());
        }
        if (mBuilder == null) {
            mBuilder = new NotificationCompat.Builder(getApplicationContext());
        }
        if (smallIcon != -1) {
            mBuilder.setSmallIcon(smallIcon);
        }
        if (contentTitle != null) {
            mBuilder.setContentTitle(contentTitle);
        }
        if (contentText != null) {
            mBuilder.setContentText(contentText);
        }
        if (ticker != null) {
            mBuilder.setTicker(ticker);
        }
        if (contentIntent != null) {
            mBuilder.setContentIntent(contentIntent);
        }
        if (when) {
            mBuilder.setWhen(System.currentTimeMillis());
        }
        mBuilder.setOngoing(onGoing);
        mBuilder.setAutoCancel(autoCancel);

        if (progress != null) {
            mBuilder.setProgress(progress.maxProgress,
                    progress.currentProgress, progress.indeterminate);
        }

//        createSwitchedPendingIntent(file);
//        mBuilder.addAction(R.id.action_icon, "Action", mSwitchedNotification.get(file.id));

//        log.debug("Notification {} file:{}, id:{}",
// contentText, file.getmFileName(), file.id);
        mBuilder.setWhen(file.id);
        mNotificationManager.notify(file.id, mBuilder.build());
    }

    class DownloadFileCallBack implements CallBack {

        private final FilmFile mFile;
        private final Resources mResources;
        private long mLastUpdateTime;

        public DownloadFileCallBack(FilmFile file) {
            mFile = file;
            mResources = getApplicationContext().getResources();
//            log.warn("DownloadFile: {}", mFile.getmFileUrl());
        }

        @Override
        public void onStarted() {
//            log.debug("onStart() file with name: {} and id: {}",
//                    mFile.getmFileName(), mFile.id);
            mFile.setmFilePath(mDownloadDirectory.getAbsolutePath() + "/" + mFile.getmFileName());
            updateStockNotify(R.mipmap.ic_launcher,
                    mFile.getmFileName(),
                    mResources.getString(R.string.download_init),
                    new Progress(100, 0, true),
                    mFile.getmFileName() + " " + mResources.getString(R.string.download_init),
                    mContentIntent, true, false, true, false, mFile);
        }

        @Override
        public void onConnecting() {
//            log.debug("onConnecting() file with name: {} and id: {}",
//                    mFile.getmFileName(), mFile.id);
            mFile.setStatus(DownloadStatuses.CONNECTING);
//            updateStockNotify(-1, null, mResources.getString(R.string.download_connecting),
//                    null,
//                    null, null, true, false, false, false, mFile);
        }

        @Override
        public void onConnected(long l, boolean b) {
//            log.debug("onConnected() file with name: {} and id: {}",
//                    mFile.getmFileName(), mFile.id);
//            updateStockNotify(-1, null, mResources.getString(R.string.download_connected),
//                    null,
//                    null, null, true, false, false, false, mFile);
        }

        @Override
        public void onProgress(long finished, long total, int progress) {
            if (mFile.getStatus() == DownloadStatuses.COMPLETE ||
                    mFile.getStatus() == DownloadStatuses.PAUSED ||
                    mFile.getStatus() == DownloadStatuses.CANCELED) {
                return;
            }
            long currentTime = System.currentTimeMillis() / 1000;
            if (mLastUpdateTime == 0) {
                mLastUpdateTime = currentTime;
            }

            //Update progress every ~1-5 sec
            int delay = Utils.generateRandomValueByRange();
            if (currentTime - mLastUpdateTime > delay) {
                if (mFile.getmFileSize() == 0) {
                    mFile.setmFileSize(total);
                }
                mFile.setStatus(DownloadStatuses.DOWNLOADING);
                mFile.setmProgressValue(progress);
                mFile.setDownloadPerSize(Utils.getDownloadPerSize(finished, total));
                updateStockNotify(-1, null, mResources.getString(R.string.download_downloading),
                        new Progress(100, progress, false),
                        String.valueOf(progress),
                        null, true, false, false, false, mFile);
                mLastUpdateTime = currentTime;
            }
        }

        @SuppressLint("SimpleDateFormat")
        @Override
        public void onCompleted() {
            if (mFile.getStatus() == DownloadStatuses.COMPLETE) {
                return;
            }
//            log.debug("onCompleted() file with name: {} and id: {}",
//                    mFile.getmFileName(), mFile.id);
            mFile.setStatus(DownloadStatuses.COMPLETE);
            mFile.setmProgressValue(100);
            mFile.setmDownloadTime(new SimpleDateFormat("HH:mm:ss").format(new Date()));
            mFile.setmDownloadDate(new Date());
            mFile.setmDownloadTimeDate(new SimpleDateFormat("HH:mm, dd.MM.yyyy").format(new Date()));
            mFile.setDownloadComplete(true);

            mBuilder.setOnlyAlertOnce(true);
            updateStockNotify(-1, null, mResources.getString(R.string.download_complete),
                    new Progress(0, 0, false),
                    mFile.getmFileName() + " " + mResources.getString(R.string.notice_download_complete),
                    createInstallAppPI(mFile.getmFilePath()),
                    false, true, false, true, mFile);
            Utils.installApp(DownloadService.this, mFile.getmFilePath());
            checkComplete();
        }

        @Override
        public void onDownloadPaused() {

        }

        @Override
        public void onDownloadCanceled() {

        }

        @Override
        public void onFailed(DownloadException e) {
            log.warn("onFailed() file with name: {} and id: {}",
                    mFile.getmFileName(), mFile.id);
            e.printStackTrace();
//            log.error("Download file failed!", e);
            mFile.setStatus(DownloadStatuses.FAILED);

            updateStockNotify(-1, null,
                    mResources.getString(R.string.download_failed) + " " + e.getErrorMessage(),
                    new Progress(0, 0, false),
                    mFile.getmFileName() + " " + e.getErrorMessage(),
                    null, false, true, false, true, mFile);
        }
    }

    public class DownloadCallBack implements CallBack {

        private FilmFile mFile;
        private long mLastUpdateTime;
        private NotificationCompat.Builder mBuilder;
        private NotificationManagerCompat mNotificationManager;
        private LocalBroadcastManager mLocalBroadcastManager;
        private Resources mResources;

        public FilmFile getFile() {
            return mFile;
        }

        public DownloadCallBack(FilmFile file) {
            mFile = file;
            mNotificationManager = NotificationManagerCompat.from(getApplicationContext());
            mBuilder = new NotificationCompat.Builder(getApplicationContext());
            mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
            mResources = getApplicationContext().getResources();
        }

        @Override
        public void onStarted() {
            //todo save to base experimental
            DBHelper.getInstance(DownloadService.this).dbWorker(DBHelper.ACTION_ADD,
                    DBHelper.FN_DOWNLOADS, mFile, null);

            int idx = Utils.getIndexOfItem(mDownloadingFiles, mFile);
            log.debug("onStarted() file with name: {} and id: {} and index: {} ",
                    mFile.getmFileName(), mFile.id, idx);

            mFile.setStatus(DownloadStatuses.CONNECTING);
            updateNotification(R.mipmap.ic_launcher,
                    mFile.getmFileName(),
                    mResources.getString(R.string.download_init),
                    new Progress(100, 0, true),
                    mFile.getmFileName() + " " + mResources.getString(R.string.download_init),
                    mContentIntent, true, false, true);
            sendBroadCast();
        }

        @Override
        public void onConnecting() {
            int idx = Utils.getIndexOfItem(mDownloadingFiles, mFile);
            log.debug("onConnecting() file with name: {} and id: {} and index: {}", mFile.getmFileName(), mFile.id, idx);
            mFile.setStatus(DownloadStatuses.CONNECTING);
//            updateNotification(-1, null, mResources.getString(R.string.download_connecting),
//                  null, null, null, true, false, false);
//            sendBroadCast();
        }

        @Override
        public void onConnected(long l, boolean b) {
            int idx = Utils.getIndexOfItem(mDownloadingFiles, mFile);
            log.debug("onConnected() file with name: {} and id: {} and index: {}",
                    mFile.getmFileName(), mFile.id, idx);
//            updateNotification(-1, null, mResources.getString(R.string.download_connected),
//              null, null, null, true, false, false);
//            sendBroadCast();
        }

        @Override
        public void onProgress(long finished, long total, int progress) {
            if (mFile.getStatus() == DownloadStatuses.COMPLETE ||
                    mFile.getStatus() == DownloadStatuses.PAUSED ||
                    mFile.getStatus() == DownloadStatuses.CANCELED) {
//                switch (mFile.getStatus()) {
//                    case DownloadStatuses.COMPLETE:
//                        log.info("onProgress() file {} status already COMPLETE", mFile.getmFileName());
//                        break;
//                    case DownloadStatuses.PAUSED:
//                        log.info("onProgress() file {} status already PAUSED", mFile.getmFileName());
//                        break;
//                    case DownloadStatuses.CANCELED:
//                        log.info("onProgress() file {} status already CANCELED", mFile.getmFileName());
//                        break;
//                }
                return;
            }
            long currentTime = System.currentTimeMillis() / 1000;
            if (mLastUpdateTime == 0) {
                mLastUpdateTime = currentTime;
            }

            //Update progress every ~1-5 sec
            int delay = Utils.generateRandomValueByRange();
            if (currentTime - mLastUpdateTime > delay) {
                int idx = Utils.getIndexOfItem(mDownloadingFiles, mFile);

                if (mFile.getmFileSize() == 0) {
                    mFile.setmFileSize(total);
                }
                mFile.setStatus(DownloadStatuses.DOWNLOADING);
                mFile.setmProgressValue(progress);
                mFile.setDownloadPerSize(Utils.getDownloadPerSize(finished, total));
                updateNotification(-1, null, mResources.getString(R.string.download_downloading),
                        new Progress(100, progress, false),
                        null, null, true, false, false);
                sendBroadCast();
                mLastUpdateTime = currentTime;
            }
        }

        @SuppressLint("SimpleDateFormat")
        @Override
        public void onCompleted() {
            if (mFile.getStatus() == DownloadStatuses.COMPLETE) {
                return;
            }
            int idx = Utils.getIndexOfItem(mDownloadingFiles, mFile);
            log.debug("onCompleted() file with name: {} and id: {} and index: {}",
                    mFile.getmFileName(), mFile.id, idx);

            mFile.setStatus(DownloadStatuses.COMPLETE);
            mFile.setmProgressValue(100);
            mFile.setmDownloadTime(new SimpleDateFormat("HH:mm:ss").format(new Date()));
            mFile.setmDownloadDate(new Date());
            mFile.setmDownloadTimeDate(new SimpleDateFormat("HH:mm, dd.MM.yyyy").format(new Date()));
            mFile.setDownloadComplete(true);
            //todo temporary off cancel event
            mDownloadingFiles.remove(mFile);
            mBuilder.setOnlyAlertOnce(true);
            updateNotification(-1, null, mResources.getString(R.string.download_complete),
                    new Progress(0, 0, false),
                    mFile.getmFileName() + " " +
                            mResources.getString(R.string.notice_download_complete),
                    null, false, true, false);

            DBHelper.getInstance(DownloadService.this).dbWorker(DBHelper.ACTION_ADD,
                    DBHelper.FN_DOWNLOADS, mFile, null);
            sendBroadCast();
            checkComplete();
        }

        @Override
        public void onDownloadPaused() {
            if (mFile.getStatus() == DownloadStatuses.PAUSED) {
                return;
            }
            int idx = Utils.getIndexOfItem(mDownloadingFiles, mFile);
            log.debug("onDownloadPaused() file with name: {} and id: {} and index: {}",
                    mFile.getmFileName(), mFile.id, idx);

            mFile.setStatus(DownloadStatuses.PAUSED);

            updateNotification(-1, null, mResources.getString(R.string.download_pause),
                    null, mFile.getmFileName() + " " + mResources.getString(R.string.notice_download_pause),
                    null, true, false, false);

            sendBroadCast();
        }

        @Override
        public void onDownloadCanceled() {
            if (mFile.getStatus() == DownloadStatuses.CANCELED) {
                return;
            }
            int idx = Utils.getIndexOfItem(mDownloadingFiles, mFile);
//            log.debug("onDownloadCanceled() file with name: {} and id: {} and index: {}",
//                    mFile.getmFileName(), mFile.id, idx);

            mNotificationManager.cancel(mFile.id);
            mFile.setStatus(DownloadStatuses.CANCELED);
            mFile.setmProgressValue(0);
            mFile.setDownloadPerSize("");
//            log.warn("before remove size {}", mDownloadingFiles.size());
            //todo temporary off cancel event
            mDownloadingFiles.remove(mFile);
//            log.warn("after remove size {}", mDownloadingFiles.size());

            updateNotification(-1, null, mResources.getString(R.string.download_canceled),
                    new Progress(0, 0, false),
                    mFile.getmFileName() + " " + mResources.getString(R.string.notice_download_canceled), null, false, true, false);

            sendBroadCast();
        }

        @Override
        public void onFailed(DownloadException ex) {
            if (mFile.getStatus() == DownloadStatuses.FAILED) {
                return;
            }
            int idx = Utils.getIndexOfItem(mDownloadingFiles, mFile);
//            log.debug("onFailed() file with name: {} and id: {} and index: {}",
//                    mFile.getmFileName(), mFile.id, idx);
            ex.printStackTrace();
            mFile.setStatus(DownloadStatuses.FAILED);
            mFile.setDownloadPerSize(getString(R.string.msg_download_error) + " " + ex.getErrorMessage());
            //todo temporary off error event
            mDownloadingFiles.remove(mFile);
            updateNotification(-1, null, mResources.getString(R.string.download_failed) + "\n" + ex.getErrorMessage(),
                    new Progress(0, 0, false),
                    mFile.getmFileName() + " " + ex.getErrorMessage(), null, false, true, false);
            DBHelper.getInstance(DownloadService.this).dbWorker(DBHelper.ACTION_ADD,
                    DBHelper.FN_DOWNLOADS, mFile, null);
            sendBroadCast();
        }

        private void updateNotification(int smallIcon, String contentTitle,
                                        String contentText, Progress progress,
                                        String ticker, PendingIntent contentIntent,
                                        boolean onGoing, boolean autoCancel,
                                        boolean addAction) {
            //Todo uncomment after pay
            if (mDownloadingFiles.size() > 3 || isGroupNotify) {
                log.info("notify size: {}, prev: {}, isGroup {}",
                        mDownloadingFiles.size(), mPrevDownloadsCount, isGroupNotify);
                isGroupNotify = true;
                if (mDownloadingFiles.size() != mPrevDownloadsCount) {
                    groupNotifications(mDownloadingFiles);
                    mPrevDownloadsCount = mDownloadingFiles.size();
                }
                return;
            }

            // Using RemoteViews to bind custom layouts into Notification
            RemoteViews remoteViews = new RemoteViews(getPackageName(),
                    R.layout.notification_download);

            if (smallIcon != -1) {
                mBuilder.setSmallIcon(smallIcon);
            }
            if (ticker != null) {
                mBuilder.setTicker(ticker);
            }
            if (contentIntent != null) {
                mBuilder.setContentIntent(contentIntent);
            }
            mBuilder.setOngoing(onGoing);
            mBuilder.setAutoCancel(autoCancel);
            mBuilder.setContent(remoteViews);
            mBuilder.setWhen(System.currentTimeMillis());
            mBuilder.setShowWhen(true);

            // Locate and set the Image into notification_download.xml ImageViews
            remoteViews.setImageViewResource(R.id.small_icon, R.mipmap.ic_launcher);

//            log.warn("notify update: file {}, status is {}",
//                    mFile.getmFileName(), mFile.getStatus());
            if (mFile.getStatus() == DownloadStatuses.PAUSED
                    || mFile.getStatus() == DownloadStatuses.FAILED) {
//                log.info("notice Pause or Fail: play icon");
                remoteViews.setImageViewResource(R.id.action_icon, R.drawable.ic_media_play);
            } else if (mFile.getStatus() == DownloadStatuses.COMPLETE
                    || mFile.getStatus() == DownloadStatuses.CANCELED) {
//                log.info("notice Complete or Canceled: remove icon");
                remoteViews.setViewVisibility(R.id.action_icon, View.GONE);
            } else if (mFile.getStatus() == DownloadStatuses.CONNECTING) {
//                log.info("notice Connecting: pause icon");
                remoteViews.setImageViewResource(R.id.action_icon, R.drawable.ic_media_pause);
            } else {
//                log.info("notice Init or Downloading: pause icon");
                remoteViews.setImageViewResource(R.id.action_icon, R.drawable.ic_media_pause);
            }

            // Locate and set the Text into notification_download.xml TextViews
            if (contentTitle != null) {
                remoteViews.setTextViewText(R.id.title, contentTitle);
            }
            if (contentText != null) {
                remoteViews.setTextViewText(R.id.text, contentText);
            }

//             Locate and set the Progress into notification_download.xml Progressbar
            if (progress == null) {
                progress = new Progress(100, 0, true);
            }
            remoteViews.setProgressBar(progress.viewId, progress.maxProgress,
                    progress.currentProgress, progress.indeterminate);
            if (progress.maxProgress == progress.currentProgress) {
                remoteViews.setViewVisibility(progress.viewId, View.GONE);
            } else {
                remoteViews.setViewVisibility(progress.viewId, View.VISIBLE);
            }
            createSwitchedPendingIntent(mFile);
            remoteViews.setOnClickPendingIntent(R.id.action_icon,
                    mSwitchedNotification.get(mFile.id));

            //log.debug("Notification {} file:{}, id:{}", contentText,
            // mFile.getmFileName(), mFile.id);

            mNotificationManager.notify(mFile.id, mBuilder.build());
        }

        private void sendBroadCast() {
//            log.info("sending broadcast file {}, status {}",
//                    mFile.getmFileName(), mFile.getStatus().toString());
            Intent intent = new Intent();
            intent.setAction(DownloadService.ACTION_DOWNLOAD_BROADCAST);
            intent.putExtra(EXTRA_FILE_INFO, mFile);
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }

    private void groupNotifications(List<FilmFile> files) {
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);

        // Create an InboxStyle notification
        android.support.v4.app.NotificationCompat.InboxStyle notificationStyle =
                new android.support.v4.app.NotificationCompat.InboxStyle();
        for (FilmFile file : files) {
            notificationStyle.addLine(file.getmFileName());
        }
        String summaryExpandHeader, summaryCollapsedHeader;
        summaryCollapsedHeader = summaryExpandHeader = files.size() > 0
                ? String.format(Locale.ENGLISH, "%2$s (%1$d)", files.size(),
                getApplicationContext().getString(R.string.group_notify_download))
                : getApplicationContext().getString(R.string.notice_download_complete);
        notificationStyle.setBigContentTitle(summaryExpandHeader);
        //notificationStyle.setSummaryText("toseex");

        Notification summaryNotification =
                new android.support.v4.app.NotificationCompat.Builder(this)
                        .setContentTitle(summaryCollapsedHeader)
                        .setSmallIcon(R.drawable.ic_file_download)
                        .setLargeIcon(largeIcon)
                        .setGroup("Downloader")
                        .setGroupSummary(true)
                        .setStyle(notificationStyle)
                        .setTicker(summaryExpandHeader)
                        .setContentIntent(mContentIntent)
                        .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancelAll();
        notificationManager.notify(5000, summaryNotification);
    }

    private void checkComplete() {
        int countInCompletes = 0;
        for (FilmFile file : mDownloadingFiles) {
            if (file.getStatus() != DownloadStatuses.COMPLETE) {
                countInCompletes++;
            }
        }
        if (countInCompletes == 0) {
            stopSelf();
        }
    }

    private class Progress {
        int viewId = R.id.download_progress;
        int maxProgress;
        int currentProgress;
        boolean indeterminate;

        Progress(int maxProgress, int currentProgress, boolean indeterminate) {
            this.maxProgress = maxProgress;
            this.currentProgress = currentProgress;
            this.indeterminate = indeterminate;
        }
    }

    private PendingIntent createInstallAppPI(String apkPath) {
        log.info("createInstallAppPI: {}", apkPath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new java.io.File(apkPath)),
                "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    private void createSwitchedPendingIntent(FilmFile file) {
        //Pause intent
        Intent pauseReceive = new Intent();
        pauseReceive.setAction(ACTION_NOTIFICATION_BROADCAST);
        pauseReceive.putExtra(EXTRA_FILE_INFO, file);
        PendingIntent pendingIntentAction = PendingIntent
                .getBroadcast(DownloadService.this, file.id,
                        pauseReceive, PendingIntent.FLAG_UPDATE_CURRENT);
        mSwitchedNotification.put(file.id, pendingIntentAction);
    }

    private void setNotificationActionReceiver() {
        // создаем BroadcastReceiver
        mReceiver = new BroadcastReceiver() {
            // действия при получении сообщений
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action == null
                        || !action.equals(DownloadService.ACTION_NOTIFICATION_BROADCAST)) {
                    return;
                }
                log.info("receive intent from notification");
                FilmFile file = (FilmFile) intent
                        .getSerializableExtra(DownloadService.EXTRA_FILE_INFO);
                if (file == null) {
                    return;
                }
                int position = Utils.getIndexOfItem(mDownloadingFiles, file);

                log.warn("receive click on {} at {} with id {}",
                        file.getmFileName(), position, file.id);

                if (file.getStatus() == DownloadStatuses.PAUSED
                        || file.getStatus() == DownloadStatuses.FAILED) {
                    log.warn("resume download for {}", file.getmFileName());
                    intentDownload(DownloadService.this, file);
                } else {
                    log.warn("pause download for {}", file.getmFileName());
                    intentPause(DownloadService.this, file);
                }
            }
        };
        // создаем фильтр для BroadcastReceiver
//        log.warn("Notification action receiver registered");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadService.ACTION_NOTIFICATION_BROADCAST);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(mReceiver, intentFilter);
    }

    private void checkAndRemoveItem(FilmFile fileInfo) {
        int position = Utils.getIndexOfItem(mDownloadingFiles, fileInfo);
        if (position != -1) {
            mDownloadingFiles.remove(position);
        }
    }

    private void checkAndAddItem(FilmFile fileInfo) {
        int position = Utils.getIndexOfItem(mDownloadingFiles, fileInfo);
        if (position == -1) {
            mDownloadingFiles.add(0, fileInfo);
        } else {
            mDownloadingFiles.set(position, fileInfo);
        }
    }
}
