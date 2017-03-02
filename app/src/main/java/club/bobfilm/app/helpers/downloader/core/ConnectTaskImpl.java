package club.bobfilm.app.helpers.downloader.core;

import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.helpers.downloader.Constants;
import club.bobfilm.app.helpers.downloader.DownloadException;
import club.bobfilm.app.helpers.downloader.architecture.ConnectTask;
import club.bobfilm.app.helpers.downloader.architecture.DownloadStatus;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Modified by CodeX on 2016/10/25.
 */
public class ConnectTaskImpl implements ConnectTask {

    private static Logger log = LoggerFactory.getLogger(ConnectTaskImpl.class);

    private final String mUri;
    private final OnConnectListener mOnConnectListener;

    private volatile int mStatus;

    private volatile long mStartTime;

    public ConnectTaskImpl(String uri, OnConnectListener listener) {
        this.mUri = uri;
        this.mOnConnectListener = listener;
    }

    public void cancel() {
        mStatus = DownloadStatus.STATUS_CANCELED;
    }

    @Override
    public boolean isConnecting() {
        return mStatus == DownloadStatus.STATUS_CONNECTING;
    }

    @Override
    public boolean isConnected() {
        return mStatus == DownloadStatus.STATUS_CONNECTED;
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
        mStatus = DownloadStatus.STATUS_CONNECTING;
        mOnConnectListener.onConnecting();
        try {
            preExecuteConnection();
        } catch (DownloadException e) {
            handleDownloadException(e);
        }
    }

    private void preExecuteConnection() throws DownloadException {
        if (mUri == null) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Bad url.");
        }
        executeConnection();

//            Log.d("Downloader", "Open httpS connection");
//            executeHttpsConnection();
    }

    private void executeConnection() throws DownloadException {
        mStartTime = System.currentTimeMillis();
        Response response = null;
        final URL url;
        try {
            url = new URL(mUri);
        } catch (MalformedURLException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Bad url.", e);
        }
        try {
            final OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(Constants.HTTP.CONNECT_TIME_OUT, TimeUnit.MILLISECONDS)
                    .readTimeout(Constants.HTTP.READ_TIME_OUT, TimeUnit.MILLISECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    //for resumed download '1' in original '0'
                    .addHeader("Range", "bytes=" + 1 + "-")
                    .build();
            response = client.newCall(request).execute();
            final int responseCode = response.code();
            log.warn("executeConnection RESPONSE CODE {}", responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 416) {
                parseResponse(response, false);
            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                parseResponse(response, true);
            } else {
                Log.i(getClass().getSimpleName() + " 109 ConnectTaskImp",
                        "(206_check_http)response code is: " + responseCode);
                throw new DownloadException(DownloadStatus.STATUS_FAILED,
                        "UnSupported response code:" + responseCode);
            }
        } catch (ProtocolException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Protocol error", e);
        } catch (IOException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "IO error", e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private void parseResponse(Response response, boolean isAcceptRanges)
            throws DownloadException {
        long length;
        String contentLength = response.header("Content-Length");
        if (TextUtils.isEmpty(contentLength) || contentLength.equals("0") || contentLength.equals("-1")) {
            length = response.body().contentLength();
        } else {
            length = Long.parseLong(contentLength);
        }

        try {
            //todo fix for download from GDRIVE
            HttpUrl requestUrl = response.request().url();
//            HttpUrl requestUrl = response.priorResponse().networkResponse().request().url();
            if (requestUrl.toString().contains("drive.google")) {
                Log.d("Downloader", "length = " + length);
                length = 4 * 1024 * 1024;
            }
        } catch (Exception ex) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                throw new DownloadException(DownloadStatus.STATUS_FAILED, "response or request is null");
            }
        }

        if (length <= 0) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "length <= 0");
        }
        checkCanceled();
        //Successful
        mStatus = DownloadStatus.STATUS_CONNECTED;
        final long timeDelta = System.currentTimeMillis() - mStartTime;
        mOnConnectListener.onConnected(timeDelta, length, isAcceptRanges);
    }

    //region old unused methods

    private void executeHttpConnection() throws DownloadException {
        mStartTime = System.currentTimeMillis();
        HttpURLConnection httpConnection = null;
        final URL url;
        try {
            url = new URL(mUri);
        } catch (MalformedURLException e) {
            throw new DownloadException(DownloadStatus.STATUS_FAILED, "Bad url.", e);
        }
        try {
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setConnectTimeout(Constants.HTTP.CONNECT_TIME_OUT);
            httpConnection.setReadTimeout(Constants.HTTP.READ_TIME_OUT);
            httpConnection.setRequestMethod(Constants.HTTP.GET);
//            for resumed download '1' in original '0'
            httpConnection.setRequestProperty("Range", "bytes=" + 1 + "-");
            final int responseCode = httpConnection.getResponseCode();
            Log.w("Downloader", "(206_check_http)response code is: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                parseResponse(httpConnection, false);
            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                parseResponse(httpConnection, true);
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

    private void parseResponse(HttpURLConnection httpConnection, boolean isAcceptRanges)
            throws DownloadException {
        long length;
        String contentLength = httpConnection.getHeaderField("Content-Length");
        if (TextUtils.isEmpty(contentLength) || contentLength.equals("0") || contentLength.equals("-1")) {
            length = httpConnection.getContentLength();
        } else {
            length = Long.parseLong(contentLength);
        }

        if (length <= 0) {
            //todo fix for download from GDRIVE
            //throw new DownloadException(DownloadStatus.STATUS_FAILED, "length <= 0");
            Log.d("Downloader", "length = " + length);
            length = 4 * 1024 * 1024;
        }

        checkCanceled();

        //Successful
        mStatus = DownloadStatus.STATUS_CONNECTED;
        final long timeDelta = System.currentTimeMillis() - mStartTime;
        mOnConnectListener.onConnected(timeDelta, length, isAcceptRanges);
    }

    //endregion

    private void checkCanceled() throws DownloadException {
        if (isCanceled()) {
            // cancel
            throw new DownloadException(DownloadStatus.STATUS_CANCELED, "Download paused!");
        }
    }

    private void handleDownloadException(DownloadException e) {
        switch (e.getErrorCode()) {
            case DownloadStatus.STATUS_FAILED:
                synchronized (mOnConnectListener) {
                    mStatus = DownloadStatus.STATUS_FAILED;
                    mOnConnectListener.onConnectFailed(e);
                }
                break;
            case DownloadStatus.STATUS_CANCELED:
                synchronized (mOnConnectListener) {
                    mStatus = DownloadStatus.STATUS_CANCELED;
                    mOnConnectListener.onConnectCanceled();
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown state");
        }
    }
}
