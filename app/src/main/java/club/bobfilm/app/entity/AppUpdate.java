package club.bobfilm.app.entity;

import java.io.Serializable;

/**
 * Created by CodeX on 14.10.2016.
 */
public class AppUpdate implements Serializable {
    private String mVersion;
    private String mDownloadUrl;

    private boolean isVersionNewer;

    public AppUpdate(String version, String newApkUrl) {
        mVersion = version;
        mDownloadUrl = newApkUrl;

    }
    public boolean isVersionNewer() {
        return isVersionNewer;
    }

    public void isVersionNewer(boolean versionNewer) {
        isVersionNewer = versionNewer;
    }

    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public void setDownloadUrl(String mDownloadUrl) {
        this.mDownloadUrl = mDownloadUrl;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String mVersion) {
        this.mVersion = mVersion;
    }
}
