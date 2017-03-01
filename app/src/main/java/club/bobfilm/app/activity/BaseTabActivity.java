package club.bobfilm.app.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import club.bobfilm.app.Application;
import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.entity.FilmFile;
import club.bobfilm.app.helpers.BobFilmParser;
import club.bobfilm.app.service.DownloadService;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 11.05.2016.
 */
public class BaseTabActivity extends AppCompatActivity
        implements View.OnClickListener, LocationListener {

    public static final String EXTRA_TAB_POSITION = "extra_tab_position";
    private static final long LOCATION_REFRESH_TIME = 5000;
    private static final float LOCATION_REFRESH_DISTANCE = 10;
    private Logger log = LoggerFactory.getLogger(BaseTabActivity.class);
    private Toolbar mToolbar;
    private ImageView ivAppHome;
    private BroadcastReceiver mReceiver;
    public static String mLastLocation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Application.setCurrentActivity(this);
        if (getIntent().getBooleanExtra("Exit me", false)) {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }
        setContentView(R.layout.activity_main_tab);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        ivAppHome = (ImageView) findViewById(R.id.iv_app_home);
        if (ivAppHome != null) {
            ivAppHome.setOnClickListener(this);
        }
        if (getSupportActionBar() == null) {
            setSupportActionBar(mToolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        setDownloadUpdateReceiver();
        setAppSettings();

        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
//            mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE, this);
            //don't forget uncomment permission
//            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
//                    0, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = Utils.convertCoordinatesToCity(this, location.getLongitude(), location.getLatitude());
        log.warn("onLocationChanged");
        Utils.setLogData();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    protected void setAppSettings() {
        Utils.setAppSettings(this);
        BobFilmParser.setContext(this);
    }


    private void setDownloadUpdateReceiver() {
        // создаем BroadcastReceiver
        mReceiver = new BroadcastReceiver() {
            // действия при получении сообщений
            @Override
            public void onReceive(Context context, Intent intent) {
                log.info("receive download update message");
                final String action = intent.getAction();
                if (action == null || !action.equals(Utils.ACTION_DOWNLOAD_UPDATE_BROADCAST)) {
                    //noinspection UnnecessaryReturnStatement
                    return;
                }
                String downloadUrl = intent.getStringExtra(Utils.EXTRA_DOWNLOAD_URL);
                if (downloadUrl != null && !downloadUrl.equalsIgnoreCase("")) {
                    Utils.downloadFile(context, downloadUrl);
                }
            }
        };
        // создаем фильтр для BroadcastReceiver
        log.warn("Download update receiver registered");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Utils.ACTION_DOWNLOAD_UPDATE_BROADCAST);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(mReceiver, intentFilter);
    }

    protected int getPagerPosition() {
        if (getIntent() != null) {
            return getIntent().getIntExtra(EXTRA_TAB_POSITION, 0);
        } else {
            return 0;
        }
    }

//    public void onItemSelected(MenuItem item) {
//        // Handle navigation view item clicks here.
//        int id = item.getItemId();
//        Intent myIntent = null;
//        if (id == R.id.nav_main) {
//            // Handle the camera action
//            myIntent = new Intent(this, ActivityTabMain.class);
//            myIntent.putExtra("main", 0);
//        } else if (id == R.id.nav_downloads) {
//            myIntent = new Intent(this, ActivityTabArchive.class);
//            myIntent.putExtra(ActivityTabArchive.EXTRA_TAB_POSITION, ActivityTabArchive.FRAGMENT_DOWNLOADS);
//        } else if (id == R.id.nav_history) {
//            myIntent = new Intent(this, ActivityTabArchive.class);
//            myIntent.putExtra(ActivityTabArchive.EXTRA_TAB_POSITION, ActivityTabArchive.FRAGMENT_HISTORY);
//        } else if (id == R.id.nav_bookmarks) {
//            myIntent = new Intent(this, ActivityTabArchive.class);
//            myIntent.putExtra(ActivityTabArchive.EXTRA_TAB_POSITION, ActivityTabArchive.FRAGMENT_BOOKMARKS);
//        } else if (id == R.id.nav_donate) {
//            myIntent = new Intent(this, ActivityDonate.class);
//            myIntent.putExtra("donate", 0);
//            Utils.emptyMessage(this, getString(R.string.message_under_construction));
//        } else if (id == R.id.nav_settings) {
//            myIntent = new Intent(this, ActivitySettings.class);
//            myIntent.putExtra("settings", 0);
//        }
//        if (mDrawer != null) {
//            mDrawer.closeDrawer(GravityCompat.START);
//        }
//        if (myIntent != null) {
//            myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            myIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            startActivity(myIntent);
//        }
//    }

    @Override
    public void onBackPressed() {
        Utils.saveSettings(this);
        appExit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
//        searchMenuItem = menu.findItem(R.id.action_search);
//        mSearchView = (SearchView) searchMenuItem.getActionView();
//        mSearchView.setOnQueryTextListener(this);
//        MenuItemCompat.setOnActionExpandListener(searchMenuItem, this);
//        startMode(Mode.NORMAL);
        return true;
    }

    protected void startArchiveActivity(int tab) {
        Intent myIntent = new Intent(this, ActivityTabArchive.class);
        myIntent.putExtra(ActivityTabArchive.EXTRA_TAB_POSITION, tab);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivityForResult(myIntent, ActivityTabMain.REQUEST_ARCHIVE);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void startSettingsActivity() {
        //Intent myIntent = new Intent(this, ActivitySettings.class);
        Intent myIntent = new Intent(this, ActivitySettings.class);
        myIntent.putExtra("settings", 0);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(myIntent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_archive:
                //Toast.makeText(BaseTabActivity.this, "view downloads", Toast.LENGTH_SHORT).show();
                startArchiveActivity(ActivityTabArchive.FRAGMENT_DOWNLOADS);
                break;
            case android.R.id.home:
                //log.info("action bar home clicked");
                onBackPressed();
                break;
            case R.id.action_settings:
//            Utils.testDownloading(1, this);
//            Toast.makeText(BaseTabActivity.this, "view settings", Toast.LENGTH_SHORT).show();
                startSettingsActivity();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void appExit() {
        Utils.showMessage(-1, getString(R.string.dialog_app_exit_text),
                R.string.dialog_btn_yes, R.string.dialog_btn_no,
                dialogClickListener).show();

//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setMessage(getString(R.string.dialog_app_exit_text))
//                .setPositiveButton(R.string.dialog_btn_yes, dialogClickListener)
//                .setNegativeButton(R.string.dialog_btn_no, dialogClickListener)
//                .show();
    }

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
//                    BaseTabActivity.super.onBackPressed();
                    quit();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };

    private void quit() {
        Intent intent = new Intent(this, BaseTabActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("Exit me", true);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public void trialIsOver() {
        Utils.saveSettings(this);
        Utils.showMessage(R.string.dialog_header_information,
                getString(R.string.msg_trial_over), -1, -1, dialogClickListener).show();
    }

    //Listener popup menu play button
    PopupMenu.OnMenuItemClickListener mPopupDownloadListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            String urlVideoFile, fileName, lightFileName;

            String timestamp = String.valueOf(System.currentTimeMillis());


            fileName = timestamp + "_video file.avi";
            switch (item.getItemId()) {
                case R.id.popup_normal:
                    urlVideoFile = "http://truba.com/video/0486/485026.mp4";
                    //fileName = "video file normal.avi";
                    log.info("normal play {}", urlVideoFile);
                    break;
                case R.id.popup_light:
                    urlVideoFile = "http://truba.com/video/0486/485026.mp4";
                    lightFileName = "0fe7042513ec7e1a71.mp4";
                    fileName = fileName.substring(0, fileName.lastIndexOf("."))
                            + "_light" + lightFileName.substring(lightFileName.lastIndexOf("."));
                    log.info("light play {}", urlVideoFile);
                    break;
                default:
                    return false;
            }

            FilmFile file = new FilmFile(fileName, urlVideoFile);

            if (!DownloadService.isDownloading(file)) {
                Toast.makeText(BaseTabActivity.this, R.string.notice_download_start, Toast.LENGTH_SHORT).show();
                DownloadService.intentDownload(BaseTabActivity.this, file);
            } else {
                Toast.makeText(BaseTabActivity.this, R.string.notice_already_download, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
    };

    private void showPopup(View v, PopupMenu.OnMenuItemClickListener mPopupListener) {
        PopupMenu popup = new PopupMenu(this, v);
        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(mPopupListener);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.popup_menu_file, popup.getMenu());
        popup.show();
    }

    @Override
    public void onClick(View v) {
        Intent myIntent = null;
        switch (v.getId()) {
            case R.id.iv_app_home:
                if (BuildConfig.DEBUG) {
                    //Todo for debug download
                    showPopup(v, mPopupDownloadListener);
                } else {
                    myIntent = new Intent(this, ActivityTabMain.class);
                    myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK |
                            Intent.FLAG_ACTIVITY_NO_HISTORY);
                    myIntent.putExtra("main", 0);
                }
                break;
        }
        if (myIntent != null) {
//            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(myIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }

    /**
     * Called when returning to the activity
     */
    @Override
    public void onResume() {
        super.onResume();
        Application.setCurrentActivity(this);
    }

    @Override
    protected void onDestroy() {
        Utils.saveSettings(this);
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }
}
