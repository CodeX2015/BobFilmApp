package club.bobfilm.app;

import android.support.v7.app.AppCompatActivity;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import club.bobfilm.app.helpers.downloader.DownloadConfiguration;
import club.bobfilm.app.helpers.downloader.DownloadManager;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 04.12.2015.
 */

public class Application extends android.app.Application {
    private static Logger log = LoggerFactory.getLogger(Application.class);
    private static AppCompatActivity mCurrentActivity;

    public static AppCompatActivity getCurrentActivity() {
        return mCurrentActivity;
    }

    public static void setCurrentActivity(AppCompatActivity mCurrentActivity) {
        Application.mCurrentActivity = mCurrentActivity;
    }

    @Override
    public void onCreate() {
        setDefaultUncaughtExceptionHandler();
        super.onCreate();

        initPicassoBuilder();
        initDownloader();
    }

    private void initPicassoBuilder() {
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso built = builder.build();
        //todo for debug picasso
        if (BuildConfig.DEBUG) {
            built.setIndicatorsEnabled(true);
            built.setLoggingEnabled(true);
        }
        Picasso.setSingletonInstance(built);
    }

    private void initDownloader() {
        DownloadConfiguration configuration = new DownloadConfiguration();
        configuration.setMaxThreadNum(10);
        configuration.setThreadNum(4);
        DownloadManager.getInstance().init(getApplicationContext(), configuration);
    }

    private static void setDefaultUncaughtExceptionHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable ex) {
                    if (BuildConfig.DEBUG) {
                        ex.printStackTrace();
                    } else {
                        log.error(Utils.getErrorLogHeader() + new Object() {
                        }.getClass().getEnclosingMethod().getName(), ex);
                    }
                }
            });
        } catch (SecurityException ex) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), ex);
            }
        }
    }

}
