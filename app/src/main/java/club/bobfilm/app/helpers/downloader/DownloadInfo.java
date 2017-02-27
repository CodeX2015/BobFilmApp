package club.bobfilm.app.helpers.downloader;

import java.io.File;

/**
 * Modified by CodeX on 16-10-26.
 */
public class DownloadInfo {
    private String name;
    private String uri;
    private File dir;
    private int progress;
    private long length;
    private long finished;
    private boolean acceptRanges;
    private int status;

    //TODO Добалены новые поля
    private int stillDownloadingThreadsCount;
    private int completedThreadsCount;
    private int failedThreadsCount;

    public DownloadInfo() {
    }

    public DownloadInfo(String name, String uri, File dir) {
        this.name = name;
        this.uri = uri;
        this.dir = dir;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public File getDir() {
        return dir;
    }

    public void setDir(File dir) {
        this.dir = dir;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getFinished() {
        return finished;
    }

    public void setFinished(long finished) {
        this.finished = finished;
    }

    public boolean isAcceptRanges() {
        return acceptRanges;
    }

    public void setAcceptRanges(boolean acceptRanges) {
        this.acceptRanges = acceptRanges;
    }

    public int getStillDownloadingThreadsCount() {
        return stillDownloadingThreadsCount;
    }

    public void setStillDownloadingThreadsCount(int stillDownloadingThreadsCount) {
        this.stillDownloadingThreadsCount = stillDownloadingThreadsCount;
    }

    public int getCompletedThreadsCount() {
        return completedThreadsCount;
    }

    public void setCompletedThreadsCount(int completedThreadsCount) {
        this.completedThreadsCount = completedThreadsCount;
    }

    public int getFailedThreadsCount() {
        return failedThreadsCount;
    }

    public void setFailedThreadsCount(int failedThreadsCount) {
        this.failedThreadsCount = failedThreadsCount;
    }
}
