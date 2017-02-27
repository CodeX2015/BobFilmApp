package club.bobfilm.app.helpers.downloader.core;

import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import club.bobfilm.app.helpers.downloader.Constants;
import club.bobfilm.app.helpers.downloader.DownloadException;
import club.bobfilm.app.helpers.downloader.DownloadInfo;
import club.bobfilm.app.helpers.downloader.architecture.DownloadStatus;
import club.bobfilm.app.helpers.downloader.architecture.DownloadTask;
import club.bobfilm.app.helpers.downloader.db.ThreadInfo;
import club.bobfilm.app.helpers.downloader.utility.IOCloseUtils;


/**
 * Modified by CodeX on 2016/10/26.
 */
public abstract class DownloadTaskImpl implements DownloadTask {

    private String mTag;
    private static Logger log = LoggerFactory.getLogger(DownloadTaskImpl.class);

    private final DownloadInfo mDownloadInfo;
    private final ThreadInfo mThreadInfo;
    private final DownloadTask.OnDownloadListener mOnDownloadListener;

    private volatile int mStatus;

    private volatile int mCommend = 0;

    public DownloadTaskImpl(DownloadInfo downloadInfo, ThreadInfo threadInfo, OnDownloadListener listener) {
        this.mDownloadInfo = downloadInfo;
        this.mThreadInfo = threadInfo;
        this.mOnDownloadListener = listener;

        this.mTag = getTag();
        if (TextUtils.isEmpty(mTag)) {
            mTag = this.getClass().getSimpleName();
        }
    }

    @Override
    public void cancel() {
        mCommend = DownloadStatus.STATUS_CANCELED;
    }

    @Override
    public void pause() {
        mCommend = DownloadStatus.STATUS_PAUSED;
    }

    @Override
    public boolean isDownloading() {
        return mStatus == DownloadStatus.STATUS_PROGRESS;
    }

    @Override
    public boolean isComplete() {
        return mStatus == DownloadStatus.STATUS_COMPLETED;
    }

    @Override
    public boolean isPaused() {
        return mStatus == DownloadStatus.STATUS_PAUSED;
    }

    @Override
    public boolean isCanceled() {
        return mStatus == DownloadStatus.STATUS_CANCELED;
    }

    @Override
    public boolean isFailed() {
        return mStatus == DownloadStatus.STATUS_FAILED;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        insertIntoDB(mThreadInfo);
        try {
            mStatus = DownloadStatus.STATUS_PROGRESS;
            //TODO --!> добалено условие чтобы открывал соединение с сетью только у тех кто не докачал (прогресс<100%)
            //Хз насколько это необходимо, не успел обдумать, я хотел избежать ситуации когда Таски со статусом Completed
            //открывают соединение с сервером и блокируют перезапуск Тасков, прежде заверешенных с ошибкой.
            if (getDownloadPercent() < 100) {
                tryExecuteDownload();
            }
            synchronized (mOnDownloadListener) {
                mStatus = DownloadStatus.STATUS_COMPLETED;
                mOnDownloadListener.onDownloadCompleted();
            }
        } catch (DownloadException e) {
            handleDownloadException(e);
        }
    }

    private int getDownloadPercent() {
        return (int) (mThreadInfo.getFinished() / mThreadInfo.getEnd() - mThreadInfo.getStart()) * 100;
    }

    private void handleDownloadException(DownloadException e) {
        switch (e.getErrorCode()) {
            case DownloadStatus.STATUS_FAILED:
                synchronized (mOnDownloadListener) {
                    mStatus = DownloadStatus.STATUS_FAILED;
                    mOnDownloadListener.onDownloadFailed(e);

                }
                break;
            case DownloadStatus.STATUS_PAUSED:
                synchronized (mOnDownloadListener) {
                    mStatus = DownloadStatus.STATUS_PAUSED;
                    mOnDownloadListener.onDownloadPaused();
                }
                break;
            case DownloadStatus.STATUS_CANCELED:
                synchronized (mOnDownloadListener) {
                    mStatus = DownloadStatus.STATUS_CANCELED;
                    mOnDownloadListener.onDownloadCanceled();
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown state");
        }
    }

    private void tryExecuteDownload() throws DownloadException {
        int numberOfRetries = 4;
        for (int i = 0; i < 4; i++) {
            try {
                executeDownload();
                break;
            } catch (DownloadException ex) {
                if (i == 3) {
                    throw ex;
                }
            }
        }
    }

    private void executeDownload() throws DownloadException {
        log.warn("THREAD INFO END {}", mThreadInfo.getEnd());
        log.warn("THREAD INFO FINISHED {}", mThreadInfo.getFinished());
        final URL url;
        try {
            url = new URL(mThreadInfo.getUri());
        } catch (MalformedURLException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Bad url.", e);
        }
        final OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Constants.HTTP.CONNECT_TIME_OUT, TimeUnit.MILLISECONDS)
                .readTimeout(Constants.HTTP.READ_TIME_OUT, TimeUnit.MILLISECONDS)
                .build();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();
        setHttpHeader(getHttpHeaders(mThreadInfo), requestBuilder);
        Request request = requestBuilder.build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            final int responseCode = response.code();
            if (responseCode == getResponseCode() || responseCode == 416) {
                transferData(response);
            } else {
                Log.i(getClass().getSimpleName() + " 154 DownloaderTaskImpl",
                        "(206_download)response code is: " + responseCode);
                throw new DownloadException(DownloadStatus.STATUS_FAILED,
                        "UnSupported response code:" + responseCode);
            }
        } catch (ProtocolException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Protocol error", e);
        } catch (IOException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "IO error", e);
        } finally {
            log.warn("Downloader {} ", "finally finished" + mThreadInfo.getFinished() + " ;line 257");
            if (response != null) {
                response.close();
            }
        }
    }

    private void setHttpHeader(Map<String, String> httpHeaders, Request.Builder requestBuilder) {
        if (httpHeaders != null) {
            for (String key : httpHeaders.keySet()) {
                requestBuilder.addHeader(key, httpHeaders.get(key));
            }
        }
    }

    private void transferData(Response response) throws DownloadException {
        InputStream inputStream;
        RandomAccessFile raf = null;
        try {
            inputStream = response.body().byteStream();
            final long offset = mThreadInfo.getStart() + mThreadInfo.getFinished();
            try {
                raf = getFile(mDownloadInfo.getDir(), mDownloadInfo.getName(), offset);
            } catch (IOException e) {
                throw new DownloadException(DownloadStatus.STATUS_FAILED, "FilmFile error", e);
            }
            transferData(inputStream, raf);
        } finally {
            try {
                IOCloseUtils.close(response);
                IOCloseUtils.close(raf);
            } catch (IOException e) {
                Log.d("IOCloseUtils", "199 IOException");
                e.printStackTrace();
            }
        }
    }

    //region unused old methods uses UrlHttpConnection

    private void executeDownload1() throws DownloadException {
        final URL url;
        try {
            url = new URL(mThreadInfo.getUri());
        } catch (MalformedURLException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Bad url.", e);
        }

        HttpURLConnection httpConnection = null;
        try {
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setConnectTimeout(Constants.HTTP.CONNECT_TIME_OUT);
            httpConnection.setReadTimeout(Constants.HTTP.READ_TIME_OUT);
            httpConnection.setRequestMethod(Constants.HTTP.GET);
            setHttpHeader(getHttpHeaders(mThreadInfo), httpConnection);
            final int responseCode = httpConnection.getResponseCode();
            Log.w("Downloader", "(206_download)response code is: " + responseCode);
            if (responseCode == getResponseCode()) {
                transferData(httpConnection);
            } else if (responseCode == 416) {
                transferData(httpConnection);
            } else {
                throw new DownloadException(DownloadStatus.STATUS_FAILED, "UnSupported response code:" + responseCode);
            }
        } catch (ProtocolException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Protocol error", e);
        } catch (IOException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "IO error", e);
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }

    private void setHttpHeader(Map<String, String> headers, URLConnection connection) {
        if (headers != null) {
            for (String key : headers.keySet()) {
                connection.setRequestProperty(key, headers.get(key));
            }
        }
    }

    private void transferData(HttpURLConnection httpConnection) throws DownloadException {
        InputStream inputStream = null;
        RandomAccessFile raf = null;
        try {
            try {
                inputStream = httpConnection.getInputStream();
            } catch (IOException e) {
                throw new DownloadException(DownloadStatus.STATUS_FAILED, "http get inputStream error", e);
            }
            final long offset = mThreadInfo.getStart() + mThreadInfo.getFinished();
            try {
                raf = getFile(mDownloadInfo.getDir(), mDownloadInfo.getName(), offset);
            } catch (IOException e) {
                throw new DownloadException(DownloadStatus.STATUS_FAILED, "FilmFile error", e);
            }
            transferData(inputStream, raf);
        } finally {
            try {
                IOCloseUtils.close(inputStream);
                IOCloseUtils.close(raf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //endregion

    private void transferData(InputStream inputStream, RandomAccessFile raf) throws DownloadException {
        final byte[] buffer = new byte[1024 * 16];
        while (true) {
            checkPausedOrCanceled();
            int len = -1;
            try {
                len = inputStream.read(buffer);
            } catch (IOException e) {
                throw new DownloadException(DownloadStatus.STATUS_FAILED, "Http inputStream read error", e);
            }
            if (len == -1) {
                break;
            }
            try {
                raf.write(buffer, 0, len);
                mThreadInfo.setFinished(mThreadInfo.getFinished() + len);
                synchronized (mOnDownloadListener) {
                    mDownloadInfo.setFinished(mDownloadInfo.getFinished() + len);
                    writeProgressToDB();
                    mOnDownloadListener.onDownloadProgress(mDownloadInfo.getFinished(), mDownloadInfo.getLength());
                }
            } catch (IOException e) {
                throw new DownloadException(DownloadStatus.STATUS_FAILED, "Fail write buffer to file", e);
            }
        }
    }

    private void writeProgressToDB() {
        int percent = getDownloadPercent();
        int previousPercent = mThreadInfo.getPreviousPercent();
        if (percent > previousPercent) {
            updateDB(mThreadInfo);
            mThreadInfo.setPreviousPercent(percent);
        }
    }


    private void checkPausedOrCanceled() throws DownloadException {
        if (mCommend == DownloadStatus.STATUS_CANCELED) {
            // cancel
            throw new DownloadException(DownloadStatus.STATUS_CANCELED, "Download canceled!");
        } else if (mCommend == DownloadStatus.STATUS_PAUSED) {
            // pause
            updateDB(mThreadInfo);
            throw new DownloadException(DownloadStatus.STATUS_PAUSED, "Download paused!");
        }
    }

    protected abstract void insertIntoDB(ThreadInfo info);

    protected abstract int getResponseCode();

    protected abstract void updateDB(ThreadInfo info);

    protected abstract Map<String, String> getHttpHeaders(ThreadInfo info);

    protected abstract RandomAccessFile getFile(File dir, String name, long offset) throws IOException;

    protected abstract String getTag();
}