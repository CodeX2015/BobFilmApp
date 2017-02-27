package club.bobfilm.app.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import club.bobfilm.app.Application;
import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.activity.ActivitySettings;
import club.bobfilm.app.activity.ActivitySplash;
import club.bobfilm.app.activity.BaseTabActivity;
import club.bobfilm.app.entity.AppUpdate;
import club.bobfilm.app.entity.FilmFile;
import club.bobfilm.app.helpers.HTMLParser;
import club.bobfilm.app.service.DownloadService;

/**
 * Created by CodeX on 20.04.2016.
 */
public class Utils {
    public static final String ARG_COMMENTS_URL = "comments";
    public static final String ARG_SUB_CATEGORIES = "subCategories";
    public static final String ARG_ADDRESS_LIST = "address_list";
    public static final String ARG_NEXT_PAGE_URL = "nextPageUrl";
    private static final int READ_BLOCK_SIZE = 100;
    public static final String ACTION_DOWNLOAD_UPDATE_BROADCAST = "downloadUpdateAction";
    public static final String EXTRA_DOWNLOAD_URL = "extraDownloadUrl";
    static String LOG_TAG = "Utils";
    public static final String ARG_FILMS_URL = "sectionUrl";
    public static final String ARG_FILM_URL = "item_details_url";
    public static final String ARG_SERIALIZABLE_SECTION = "section";
    public static final String ARG_FILM_DETAILS = "film";
    public static final String ARG_SEARCH_RESULTS = "searchResults";
    public static final String ARG_FILE = "file";
    private static final String PREF_LANGUAGE_ID = "LanguageId";
    private static final String PREF_DOWNLOAD_PATH = "DownloadPath";
    private static final String PREF_APP_LAUNCHES_COUNT = "AppLaunchesCount";
    private static final String PREF_APP_INSTALL_ID = "AppInstallId";
    private static String cryptoPass = "sup3rS3xy";

    private static final String IMAGE_PATTERN = "([^\\s]+(\\.(?i)(jpg|png|gif|bmp))$)";
    private static final String VIDEO_PATTERN = "(?i).*\\.(mp3|ape|cue|mp4|mpg|avi|mkv|asf|mov|qt|avchd|flv|wmv|vob|ifo|dub|m4v|m2ts|ts)";
    private final static String PREFS_NAME = "AppSettings";
    private static Logger log = LoggerFactory.getLogger(Utils.class);
    private static Context mContext;
    private static String mHeaderString;
    public static int mPrevMargin;

    public static String convertInputStreamToString(InputStream is) {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder total = new StringBuilder();
        String line;
        try {
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return total.toString();
    }

    public static Bitmap convertInputStreamToBitmap(InputStream is) {
        return BitmapFactory.decodeStream(is);
    }

    public static void setImageViewBitmap(final Context context,
                                          final String imageUrl,
                                          final ImageView imageView,
                                          @Nullable final ViewFlipper vfProgress) {
        if (imageUrl== null || imageUrl.equalsIgnoreCase("") || imageView == null) {
            return;
        }
        log.info("image: {}", imageUrl);
        PicassoBigCache.INSTANCE.getPicassoBigCache(context)
                .load(imageUrl)
                .centerCrop()
                .fit()
                .priority(Picasso.Priority.HIGH)
                /*.networkPolicy(NetworkPolicy.OFFLINE)*/
                .placeholder(R.drawable.no_picture)
                .error(R.drawable.no_picture)
//                .resize(imageView.getWidth(), imageView.getHeight())
                //.error(R.raw.loading)
                .into(imageView, new Callback() {
                            @Override
                            public void onSuccess() {
                                //log.debug("Picasso, download and cached");
                                if (vfProgress != null) {
                                    new Handler().postDelayed(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    vfProgress.setDisplayedChild(1);
                                                }
                                            }, context.getResources().getInteger(R.integer.activity_splash_time)
                                    );
                                }
                            }

                            @Override
                            public void onError() {
                                log.debug("Picasso, Could not fetch image, try more");
                                Picasso.with(context)
                                        .load(imageUrl)
                                        .centerCrop()
                                        .fit()
                                        .priority(Picasso.Priority.HIGH)
                                        .placeholder(R.drawable.no_picture)
                                        .error(R.drawable.no_picture)
                                        .into(imageView);
                                if (vfProgress != null) {
                                    new Handler().postDelayed(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    vfProgress.setDisplayedChild(1);
                                                }
                                            }, context.getResources().getInteger(R.integer.activity_splash_time)
                                    );
                                }
                            }
                        }
                );
    }

    /**
     * Convert Dp to Pixel
     */
    public static int dpToPx(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
        return (int) px;
    }

    public static float pxToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        //noinspection UnnecessaryLocalVariable
        float dp = px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }

    public static boolean isServiceRunning(String serviceClassName, Context context) {
        final ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services =
                activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)) {
                return true;
            }
        }
        return false;
    }

    private static void getAssetsList(Context ctx) {
        AssetManager myAssetManager = ctx.getAssets();
        try {
            String[] Files = myAssetManager.list(""); // массив имен файлов
            StringBuilder result = new StringBuilder();
            String prefix = "";
            for (String file : Files) {
                result.append(prefix);
                prefix = "\n";
                result.append(file);
            }
        } catch (IOException error) {
            log.error(new Object() {
            }.getClass().getEnclosingMethod().getName(), error);
        }
    }

    /**
     * Validate video with regular expression
     *
     * @param fileName file name of video file with extension for validation
     * @return true valid video, false invalid video
     */
    public static boolean isVideo(final String fileName) {
        Pattern pattern = Pattern.compile(VIDEO_PATTERN);
        Matcher matcher = pattern.matcher(fileName);
        boolean result = matcher.matches();
        if (!result) {
            log.debug("{}, false", fileName);
        }
        return matcher.matches();
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    @SuppressLint("DefaultLocale")
    public static String humanReadableByteCount(long bytes, boolean isDelim1000) {
        int unit = isDelim1000 ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (isDelim1000 ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (isDelim1000 ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String getDownloadPerSize(long finished, long total) {
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format((float) finished / (1024 * 1024)) + "Mb/" + df.format((float) total / (1024 * 1024)) + "Mb";
    }

    public static int getToolbarHeight(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{R.attr.actionBarSize});
        int toolbarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        return toolbarHeight;
    }

    public static void emptyMessage(final Context context, String message, final boolean isFinish) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.dialog_header_information)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(R.string.dialog_btn_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                if (isFinish) {
                                    ((AppCompatActivity) context).finish();
                                }
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static AlertDialog showMessage(int titleId, String message,
                                   int btnPositiveId, int btnNegativeId,
                                   DialogInterface.OnClickListener dialogAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Application.getCurrentActivity());
        if (titleId != -1) {
            builder.setTitle(titleId);
        }
        CharSequence resultMessage = message.contains("</") ? Html.fromHtml(message) : message;
        builder.setMessage(resultMessage)
                .setCancelable(false)
                .setPositiveButton(btnPositiveId == -1 ? R.string.dialog_btn_ok : btnPositiveId, dialogAction);
        if (btnNegativeId != -1) {
            builder.setNegativeButton(btnNegativeId, dialogAction);
        }
        return builder.create();
    }

    public static boolean checkSettings(Context context) {
        //SharedPreferences sett =  getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        //Toast.makeText(this, sett.getString("str", "_"), Toast.LENGTH_LONG).show();

        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        //settings.edit().clear().apply();
        return settings.getAll().size() > 3;
    }

    @SuppressLint("CommitPrefEdits")
    public static void saveSettings(Context context) {
        //Todo convert value to string and encrypt
        //log.debug("saveSettings: LangId={}", ActivitySettings.AppLangId);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putLong(PREF_APP_INSTALL_ID, ActivitySettings.AppId);
        editor.putInt(PREF_LANGUAGE_ID, ActivitySettings.AppLangId);
        editor.putString(PREF_DOWNLOAD_PATH, ActivitySettings.DownloadPath);
        editor.putInt(PREF_APP_LAUNCHES_COUNT, ActivitySettings.AppLaunchesCount);
        editor.commit();
    }

    public static void setAppSettings(Context context) {
        mContext = context;
        if (checkSettings(context)) {
            loadSettings(context);
        }
        changeLang(context, ActivitySettings.AppLangId);
    }

    public static void setLogData() {
        new Thread(new Runnable() {
            public void run() {
                setErrorLogHeader();
            }
        }).start();
    }

    public static void loadSettings(Context context) {
        //Todo convert value from string and decrypt
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        ActivitySettings.AppId = prefs.getLong(PREF_APP_INSTALL_ID, 0);
        ActivitySettings.DownloadPath = prefs.getString(PREF_DOWNLOAD_PATH, "");
        ActivitySettings.AppLangId = prefs.getInt(PREF_LANGUAGE_ID, 0);
        ActivitySettings.AppLaunchesCount = prefs.getInt(PREF_APP_LAUNCHES_COUNT, 0) + 1;
        //log.debug("loadSettings: LangId={}", ActivitySettings.AppLangId);
    }

    public static void changeLang(Context context, int lang) {
        HTMLParser.mSiteLanguage = context.getResources().getStringArray(R.array.site_lang)[lang];
        String[] langData = context.getResources().getStringArray(R.array.site_lang);
        Locale myLocale = new Locale(langData[lang]);
        //log.debug("changeLang: locale={}", langData[lang]);
        Locale.setDefault(myLocale);
        Configuration config = new Configuration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(myLocale);
        } else {
            config.locale = myLocale;
        }
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        saveSettings(context);
    }

    public static int getCurrentLanguage(Context context) {
        Locale current = context.getResources().getConfiguration().locale;
        String[] langData = context.getResources().getStringArray(R.array.site_lang);
        int lang = Arrays.asList(langData).indexOf(current.toString());
        return lang == -1 ? 0 : lang;
    }

    @SuppressWarnings("ConstantConditions")
    public static String getDownloadPath(Context context) {
        java.io.File downloadPath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        return downloadPath.getAbsolutePath();
    }

    public static void restartActivity(Context context) {
        AppCompatActivity activity = (AppCompatActivity) context;
        Intent intent = activity.getIntent();
        activity.finish();
        activity.overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    public static void restartApp(Context context) {
        saveSettings(context);
        AppCompatActivity activity = (AppCompatActivity) context;
        Intent i = activity.getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(activity.getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NO_HISTORY);
        System.exit(0);
        activity.startActivity(i);
    }

    public static void restartApp2(Context context) {
        saveSettings(context);
        Intent mStartActivity = new Intent(context, ActivitySplash.class);
        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NO_HISTORY);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);

    }

    @SuppressLint("SimpleDateFormat")
    public static void testDownloading(int filesCount, final Context context) {
        FilmFile file;
        for (int i = filesCount; i > 0; i--) {
//            file = new FilmFile("http://zv.fm/download/2949940", "http://zv.fm/download/2949940");
            file = new FilmFile(/*"FilmFile - " + String.valueOf(i - 1) + "_" +
                    new SimpleDateFormat("HH.mm.ss").format(new Date()) + ".mp4"*/"0fe7042513ec7e1a71.mp4",
                    "http://truba.com/video/0486/485026.mp4"/*"http://truba.com/video/0492/491443.mp4"*/);


//            file = new FilmFile("FilmFile - " + String.valueOf(i + 1) +"_"+ new SimpleDateFormat("HH.mm.ss").format(new Date()) + ".mp3", "http://storage.mp3.cc/download/43993610/Tk4xRllUSjRvUVppZHcwc1FXdSs3TDhGNmVLOG1GdVc1eHE4SCtqeEZDT0xlQ3B3Y0M4Y09vNXpUbUdVRlgydTZ2d25nUGVxWC9NUXpTNGpEMDk2MkNjaVhEdEFBRGwrTzlVNDh2WlZua25VWkh5WCtaWWIzVVk1ZlRLOGtQdVk/Arty_Feat._Conrad_Sewell-Braver_Love_BassBoosted_by_NeoAndreE_(mp3.cc).mp3");
//            file = new FilmFile("FilmFile - " + String.valueOf(i) + ".mp4", "http://www.dailymotion.com/cdn/H264-320x240/video/x2hubtz.mp4?auth=1464616635-2562-uokdqinp-4e023747bd2af9c146ae795d3442fc75");

            DownloadService.intentDownload(context, file);
        }
    }

    public static void shareTextUrl(Context context, String subject, String text) {
        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Add data to the intent, the receiving app will decide
        // what to do with it.
        share.putExtra(Intent.EXTRA_SUBJECT, subject);
        share.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(share, context.getResources().getString(R.string.action_send_to)));
    }

    public static void playVideo(@Nullable String path, Context context) {
        if (path == null || path.equals("")) {
            path = "http://www.ex.ua/get/241488313";
        }
        Uri videoFile = Uri.parse(path);
        Intent intent = new Intent(Intent.ACTION_VIEW, videoFile);
        intent.setDataAndType(videoFile, "video/*");
        //context.startActivity(Utils.resolveIntent(context, intent));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    public static void openUrl(@Nullable String url, Context context) {
        if (url == null || url.equals("")) {
            url = "http://www.example.com";
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
    }

    @Nullable
    public static Intent resolveIntent(@NonNull Context ctx, @NonNull Intent intent) {
        //Retrieve the activities that can handle the file
        final PackageManager packageManager = ctx.getPackageManager();
        List<ResolveInfo> info = packageManager.queryIntentActivities(intent, 0);
        if (info.isEmpty()) {
            // No registered applications, try open with wildcard mime type
            info = packageManager.queryIntentActivities(intent, 0);
            if (info.isEmpty()) {
                // No registered applications at all
                return null;
            }
        }
        return Intent.createChooser(intent, ctx.getString(club.bobfilm.app.R.string.dialog_choose_app));
    }

    public static boolean checkFileExist(String filePath) {
        try {
            if (filePath == null) {
                return false;
            }
            java.io.File file = new java.io.File(filePath);
            return file.exists();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage(), ex);
        }
        return false;
    }

    public static void deleteFile(String filePath) {
        try {
            if (filePath == null || !checkFileExist(filePath)) {
                return;
            }
            java.io.File fdelete = new java.io.File(filePath);
            if (fdelete.exists()) {
                if (fdelete.delete()) {
                    log.info("file Deleted :" + filePath);
                } else {
                    log.info("file not Deleted :" + filePath);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage(), ex);
        }
    }

    public static String getErrorLogHeader() {
        log.info("get: log header {}", mHeaderString);
        if (mHeaderString == null)
            setErrorLogHeader();
        return mHeaderString;
    }

    public static void setErrorLogHeader() {
        String manufacturer = Build.MANUFACTURER;
        String device = Build.DEVICE;
        String model = Build.MODEL;
        String androidVer = Build.VERSION.RELEASE;
        int appVer = BuildConfig.VERSION_CODE;
        String appVerName = BuildConfig.VERSION_NAME;
        String location = BaseTabActivity.mLastLocation;
        String ipAddress = null;
        String macAddress = null;
        String imei = null;
        try {
            ipAddress = getIPAddress(true);
            macAddress = getMACAddress(null);
            imei = getIMEI(mContext);
        } catch (Exception ex) {
            imei = ex.getMessage();
        }

        String separatorStart = "=============================";
        String separatorEnd = "=============================";

        mHeaderString = String.format(Locale.ENGLISH,
                "\n%1$s\nMANUFACTURER: %2$s\nDEVICE: %3$s\nMODEL: %4$s\n" +
                        "ANDROID: %5$s\nAPP VER.: %6$d\nVER. NAME: %7$s\n" +
                        "IP: %8$s\nMAC: %9$s\nIMEI/MEID: %10$s\nLOCATION: %11$s\n%12$s\n",
                separatorStart, manufacturer, device, model, androidVer,
                appVer, appVerName, ipAddress, macAddress, imei, location, separatorEnd);
        log.info("set: log header {}", mHeaderString);
    }

    @SuppressLint("HardwareIds")
    static String getIMEI(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    /**
     * Returns MAC address of the given interface name.
     *
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac == null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx = 0; idx < mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } // for now eat exceptions
        return "";
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } // for now eat exceptions
        return "";
    }

    public static int getIndexOfItem(List<FilmFile> ListItems, FilmFile item) {
        if (ListItems != null && ListItems.size() > 0) {
            for (FilmFile listFiles : ListItems) {
                if (item.equals(listFiles)) {
                    return ListItems.indexOf(listFiles);
                }
            }
        }
        return -1;
    }

    public static int generateRandomValueByRange() {
        int min = 2;
        int max = 6;
        Random random = new Random();
        //        min + (int) (Math.random() * ((max - min) + 1));
        return random.nextInt(max - min + 1) + min;
    }

    public static String convertMillisecondsToDate(String dateFormat, long milliSeconds) {
        if (dateFormat == null || dateFormat.equalsIgnoreCase("")) {
            dateFormat = "dd/MM/yyyy";
        }
        @SuppressLint("SimpleDateFormat") DateFormat formatter = new SimpleDateFormat(dateFormat);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    public static boolean isAppBad(Context context) {
        int trialDay = 30;
        int trialLaunch = trialDay * 5;
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/" + ".cacheImageId";
        loadSettings(context);
        String installDate = convertMillisecondsToDate("", ActivitySettings.AppId);
        log.debug("App was installed at {}", installDate);
        if (ActivitySettings.AppLaunchesCount > trialLaunch) {
            log.debug("Trial period by AppLaunchesCount is over ({} > {})"
                    , ActivitySettings.AppLaunchesCount, trialLaunch);
            return true;
        }
        if (ActivitySettings.AppId != 0) {
            log.info("ID from prefs");
            log.debug("From prefs: AppId = {}", ActivitySettings.AppId);
            return isTrialOver(trialDay);
        } else {
            if (checkFileExist(filePath)) {
                log.info("ID from file");
                String appId = readFromFile(filePath);
                ActivitySettings.AppId = Long.parseLong(appId);
                saveSettings(context);
                log.debug("From file: AppId = {}", ActivitySettings.AppId);
                return isTrialOver(trialDay);
            } else {
                log.info("create new ID");
                ActivitySettings.AppId = System.currentTimeMillis();
                writeToFile(context, filePath, String.valueOf(ActivitySettings.AppId));
                saveSettings(context);
                log.debug("First run: AppId = {}", ActivitySettings.AppId);
                return isTrialOver(trialDay);
            }
        }
    }

    private static boolean isTrialOver(int trialDay) {
        long currentTime = System.currentTimeMillis();
        long trialPeriod = TimeUnit.MILLISECONDS.convert(trialDay, TimeUnit.DAYS);
        long trialDateOver = ActivitySettings.AppId + trialPeriod;
        log.debug("install date {}, date over {}, current date {}, isTrialOver={}",
                convertMillisecondsToDate("", ActivitySettings.AppId),
                convertMillisecondsToDate("", trialDateOver),
                convertMillisecondsToDate("", currentTime),
                (currentTime > trialDateOver));
        return (currentTime > trialDateOver);
    }

    public static String readFromFile(String filePath) {
        try {
            FileReader fRd = new FileReader(filePath);
            BufferedReader reader = new BufferedReader(fRd);
            String str;
            StringBuilder buffer = new StringBuilder();

            while ((str = reader.readLine()) != null) {
                buffer.append(str);
            }
            fRd.close();
            return decryptDESString(buffer.toString());
        } catch (Throwable t) {
            log.debug("buffer reader fail");
        }
        return null;
    }

    public static void writeToFile(Context context, String filePath, String body) {
        try {
            // Create a new output file stream
            java.io.File file = new java.io.File(filePath);
            file.getParentFile().mkdirs();
            FileOutputStream writeFile = new FileOutputStream(file);
            OutputStreamWriter outputWriter = new OutputStreamWriter(writeFile);
            outputWriter.write(encryptDESString(body));
            outputWriter.flush();
            outputWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            log.error("IOException WriteToFile error: ", ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Exception WriteToFile error: ", ex);
        }
    }

    public static String encodeToUtf(String str) {
        // returns "%D0%BA%D1%83%D1%82%D1%8F%D1%82%D0%B0.%D1%80%D1%84"
        String result = null;
        try {
            result = URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String encryptBase64String(String input) {
        // Simple encryption, not very strong!
        return Base64.encodeToString(input.getBytes(), Base64.DEFAULT);
    }

    public static String decryptBase64String(String input) {
        return new String(Base64.decode(input, Base64.DEFAULT));
    }

    @SuppressWarnings("TryWithIdenticalCatches")
    public static String encryptDESString(String value) {
        try {
            DESKeySpec keySpec = new DESKeySpec(cryptoPass.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] clearText = value.getBytes("UTF8");
            // Cipher is not thread safe
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            String encryptedValue = Base64.encodeToString(cipher.doFinal(clearText), Base64.DEFAULT);
            log.debug("Encrypted: {} -> {}", value, encryptedValue);
            return encryptedValue;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return value;
    }

    @SuppressWarnings("TryWithIdenticalCatches")
    public static String decryptDESString(String value) {
        try {
            DESKeySpec keySpec = new DESKeySpec(cryptoPass.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encryptedPwdBytes = Base64.decode(value, Base64.DEFAULT);
            // cipher is not thread safe
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedValueBytes = (cipher.doFinal(encryptedPwdBytes));

            String decryptedValue = new String(decryptedValueBytes);
            log.debug("Decrypted: {} -> {}", value, decryptedValue);
            return decryptedValue;

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        return netInfo != null;
    }

    public static AppUpdate getAppUpdate(String responseXml) {
        String currentVersion = BuildConfig.VERSION_NAME;

        String version = responseXml.contains("n>") ?
                responseXml.substring(responseXml.indexOf("n>") + 2, responseXml.indexOf("</v")) : responseXml;
        String newApkUrl = responseXml.contains("l>") ?
                responseXml.substring(responseXml.indexOf("l>") + 2, responseXml.indexOf("</u")) : responseXml;
        log.info("\nresult={}\nversion={}\nnewApkUrl={}\n", responseXml, version, newApkUrl);
        AppUpdate newApp = new AppUpdate(version, newApkUrl);
        log.info("current is {}, server is: {}", currentVersion, version);
        if (currentVersion.equalsIgnoreCase(version)) {
            return null;
        }
        int[] currentSeparated = convert(currentVersion.split("\\."));
        int[] versionSeparated = convert(version.split("\\."));
        if (currentSeparated.length != versionSeparated.length) {
            //log.debug("server: {} format unsupported current: {}", currentVersion, version);
            return null;
        } else {
            for (int i = 0; i < versionSeparated.length; i++) {
                if (versionSeparated[i] > currentSeparated[i]) {
                    log.info("sep{} > cur{}", versionSeparated, currentSeparated);
                    return newApp;
                }
                if (versionSeparated[i] == currentSeparated[i] && i + 1 < versionSeparated.length) {
                    //log.info("sep{} = cur{}", versionSeparated, currentSeparated);
                    if (versionSeparated[i + 1] > currentSeparated[i + 1]) {
                        log.info("sep i={} {} > cur{}", i, versionSeparated, currentSeparated);
                        return newApp;
                    }
                }
            }
        }
        return null;
    }

    private static int[] convert(String[] string) {
        int number[] = new int[string.length];

        for (int i = 0; i < string.length; i++) {
            number[i] = Integer.parseInt(string[i]); // error here
        }
        return number;
    }


    private static void showUpdateDialog(final Context context, final String newApkUrl) {
        log.info("show dialog update available");
        showMessage(R.string.dialog_header_information,
                context.getString(R.string.msg_update_available),
                R.string.dialog_btn_update, R.string.dialog_btn_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int btnId) {
                        switch (btnId) {
                            case DialogInterface.BUTTON_POSITIVE:
                                downloadFile(context, newApkUrl);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                }
        ).show();
    }

    public static void pushNotify(Context context, String contentTitle, PendingIntent contentIntent) {

        NotificationManagerCompat notifyManager = NotificationManagerCompat.from(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setOngoing(false);
        builder.setAutoCancel(true);
        builder.setWhen(System.currentTimeMillis());

        if (contentTitle != null) {
            builder.setContentTitle(contentTitle);
            builder.setTicker(contentTitle);
        }
        if (contentIntent != null) {
            builder.setContentIntent(contentIntent);
        }

        log.debug("Notification update");

        notifyManager.notify(123321, builder.build());
    }

    private void sendDownloadUpdateBroadCast(Context context) {
        log.info("sending broadcast about update");
        Intent intent = new Intent();
        intent.setAction(Utils.ACTION_DOWNLOAD_UPDATE_BROADCAST);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void installApp(Context context, String apkPath) {
        log.info("installApp: {}", apkPath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new java.io.File(apkPath)), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static PendingIntent createInstallAppPI(Context context, String apkPath) {
        log.info("createInstallAppPI: {}", apkPath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new java.io.File(apkPath)),
                "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    public static PendingIntent createDownloadUpdatePI(Context context, String newApkUrl) {
        //Download update intent
        Intent downloadReceive = new Intent();
        downloadReceive.setAction(Utils.ACTION_DOWNLOAD_UPDATE_BROADCAST);
        downloadReceive.putExtra(EXTRA_DOWNLOAD_URL, newApkUrl);
        return PendingIntent.getBroadcast(context, 123321, downloadReceive, PendingIntent.FLAG_ONE_SHOT);
    }

    public static boolean isDetailsLink(final String url) {
        String urlPattern = ".*?(ex.ru/)?(\\d)";
        Pattern pattern = Pattern.compile(urlPattern);
        Matcher matcher = pattern.matcher(url);
        boolean result = matcher.matches();
        if (!result) {
            log.debug("{}, is not details link", url);
        }
        return matcher.matches();
    }

    public static void downloadFile(final Context context, String newApkUrl) {
        DownloadService.intentDownloadOneFile(context, newApkUrl);

//        HTMLParser.downloadFile(newApkUrl, new HTMLParser.LoadListener() {
//            @Override
//            public void OnLoadComplete(Object result) {
//                String apkPath = (String) result;
//                Utils.installApp(context, apkPath);
//            }
//
//            @Override
//            public void OnLoadError(Exception ex) {
//                log.debug("downloadFile: {}", ex.getMessage());
//            }
//        });
    }

    public static String convertDate(String date, @Nullable String format_in, @Nullable String format_out) {
//        date = "2015-03-22T19:59:29.315188";
        if (date == null) {
            //noinspection ConstantConditions
            return date;
        }
        if (format_in == null) {
            //format_in = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";
            format_in = "dd MMMMM yyyy";
        }
        if (format_out == null) {
            format_out = "dd.MM.yyyy";
        }
//        format_out = "dd MMM";

        SimpleDateFormat formatInput = new SimpleDateFormat(format_in, Locale.getDefault());
        SimpleDateFormat formatOutput = new SimpleDateFormat(format_out, Locale.getDefault());

        String resultDate = date;
        try {
            resultDate = formatOutput.format(formatInput.parse(date));

        } catch (Exception e) {
            log.debug("unable convert {} to {}", date, format_out);
            e.printStackTrace();
        }
        return resultDate;
    }

    public static String convertCoordinatesToCity(Context context, double lng, double lat) {
        Geocoder gcd = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(lat, lng, 1);
            if (addresses.size() > 0) {
                String address = String.format("%1$s, %2$s, %3$s, д. %4$s",
                        addresses.get(0).getCountryName(), addresses.get(0).getLocality(),
                        addresses.get(0).getThoroughfare(), addresses.get(0).getSubThoroughfare());
                return address;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    public static String extractDigitsFromString(String str){
        return str.replaceAll("[^0-9]", "");
    }

    public static Spanned underlineText(String text) {
        Spanned underlineText;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//            underlineText = Html.fromHtml("<u>" + text + "</u>", Html.FROM_HTML_MODE_LEGACY);
//        } else {
            //noinspection deprecation
            underlineText = Html.fromHtml("<u>" + text + "</u>");
//        }
        return underlineText;
    }
}
