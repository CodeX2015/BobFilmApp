package club.bobfilm.app.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.text.Html;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import club.bobfilm.app.Application;
import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.entity.AppUpdate;
import club.bobfilm.app.helpers.DBHelper;
import club.bobfilm.app.util.Utils;
import club.bobfilm.app.util.dirchooser.DirectoryChooserActivity;
import club.bobfilm.app.util.dirchooser.DirectoryChooserConfig;

/**
 * Created by CodeX on 09.09.2016.
 */
public class ActivitySettings extends BaseActivity implements View.OnClickListener {

    public static int LANG_RU = 0;
    public static int LANG_EN = 1;
    public static int LANG_UK = 2;

    private static final int REQUEST_DIRECTORY = 103;

    Logger log = LoggerFactory.getLogger(ActivitySettings.class);

    public static long AppId = 0;
    public static String DownloadPath = "";
    public static int AppLangId = LANG_RU;
    public static int AppLaunchesCount = 0;
    private static TextView mLangSelectedTV;
    public static boolean mAppNewVersionAvailable = false;
    public static AppUpdate mAppNewVersion;
    public static String mAppNewVersionUrl;
    private ArrayAdapter<String> mAdapter;
    private static String[] mData;
    int mSelectedLang;

    @Override
    protected void onResume() {
        super.onResume();
        Application.setCurrentActivity(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mLangSelectedTV = (TextView) findViewById(R.id.tv_choice_language_value);
        mData = getResources().getStringArray(R.array.app_locales);

        mLangSelectedTV.setText(mData[AppLangId]);

        // Display the fragment as the main content.
        //changeFragment(new SettingsPreferenceFragment());
    }


    void changeFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
    }

    @Override
    public void onClick(View view) {
        String toast = null;
        switch (view.getId()) {
            case R.id.ll_setting_language:
                toast = "language";
                LanguageSelector langSelector = new LanguageSelector();
                langSelector.show(getSupportFragmentManager(), "dialog_select_lang");
                //selectAppLanguage();
                break;
            case R.id.ll_setting_storage:
                toast = "storage";
                chooseDirectory();
                break;
            case R.id.ll_setting_clear_history:
                toast = "clear_history";
                clearHistory();
                break;
            case R.id.ll_setting_bypass_blocked:
                toast = "bypass ex.ua blocks";
                break;
            case R.id.ll_setting_app_update:
                //todo uncomment after paying
                showUpdateDialog();
                //toast = "app_update";
                break;
            case R.id.ll_setting_about:
                toast = "App version: " + BuildConfig.VERSION_NAME;
                AboutApp aboutApp = new AboutApp();
                aboutApp.show(getSupportFragmentManager(), "dialog_about_app");
                break;
            case R.id.ll_setting_license:
                toast = "license";
                //changeFragment(new FragmentLicense());
                super.startActivityLicense();
                break;
        }
        if (toast != null) {
            Toast.makeText(ActivitySettings.this, toast, Toast.LENGTH_LONG).show();
        }
    }

    private void showUpdateDialog() {
        if (mAppNewVersionAvailable) {
            Utils.showMessage(R.string.dialog_header_information,
                    getString(R.string.msg_update_available),
                    R.string.dialog_btn_update, R.string.dialog_btn_cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int btnId) {
                            switch (btnId) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    Utils.downloadFile(ActivitySettings.this, mAppNewVersionUrl);
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    dialog.cancel();
                                    break;
                            }
                        }
                    }).show();
        } else {
            Utils.emptyMessage(this, getString(R.string.msg_no_new_version), false);
        }
    }

    private void chooseDirectory() {
        final Intent chooserIntent = new Intent(this, DirectoryChooserActivity.class);
        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .initialDirectory(DownloadPath)
                .newDirectoryName("new folder")
                .allowReadOnlyDirectory(true)
                .allowNewDirectoryNameModification(true)
                .build();

        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);

        // REQUEST_DIRECTORY is a constant integer to identify the request, e.g. 0
        startActivityForResult(chooserIntent, REQUEST_DIRECTORY);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void clearHistory() {
        DBHelper.getInstance(this).dbWorker(DBHelper.ACTION_DELETE_ALL, DBHelper.FN_HISTORY, null, null);
    }

    @Override
    public void onBackPressed() {
        Utils.saveSettings(this);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DIRECTORY) {
            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                DownloadPath = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
            } else {
                // Nothing selected
                log.info("Request directory: Nothing selected");
            }
        }
    }

    public static class AboutApp extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    new ContextThemeWrapper(getActivity(), R.style.MyAlertDialogStyle));
            final View dialogLayout = getActivity().getLayoutInflater()
                    .inflate(R.layout.dialog_about_app, null);

            TextView appVersion = (TextView) dialogLayout.findViewById(R.id.tv_app_version);
            appVersion.setText(BuildConfig.VERSION_NAME);

            // Set the dialog title
            builder.setTitle(R.string.activity_setting_about)
                    .setView(dialogLayout);
            if (mAppNewVersionAvailable) {
                builder.setPositiveButton(R.string.dialog_btn_update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getActivity(), "Update app", Toast.LENGTH_SHORT).show();

                    }
                });
            } else {
                builder.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getActivity(), "OK", Toast.LENGTH_SHORT).show();
                        onCancel(dialog);
                    }
                });
            }
            return builder.create();
        }
    }

    public static class LanguageSelector extends DialogFragment implements View.OnClickListener {
        int mSelectedItem;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final View dialogLayout = getActivity().getLayoutInflater().inflate(R.layout.dialog_language_select, null);

            final RadioGroup rbgLanguage = (RadioGroup) dialogLayout.findViewById(R.id.rbg_select_language);
            ((RadioButton) rbgLanguage.getChildAt(AppLangId)).setChecked(true);

            final RadioButton rbRussian = (RadioButton) dialogLayout.findViewById(R.id.rb_russian);
            rbRussian.setText(mData[LANG_RU]);
            rbRussian.setOnClickListener(this);

            final RadioButton rbEnglish = (RadioButton) dialogLayout.findViewById(R.id.rb_english);
            rbEnglish.setText(mData[LANG_EN]);
            rbEnglish.setOnClickListener(this);

            final RadioButton rbUkrainian = (RadioButton) dialogLayout.findViewById(R.id.rb_ukrainian);
            rbUkrainian.setText(mData[LANG_UK]);
            builder.setTitle(R.string.activity_setting_choose_language);
            rbUkrainian.setOnClickListener(this);

            // Set the dialog title
            builder.setTitle(R.string.activity_setting_choose_language)
                    .setView(dialogLayout)
                    // Set the action buttons
                    .setPositiveButton(R.string.dialog_btn_select, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK, so save the mSelectedItems results somewhere
                            // or return them to the component that opened the dialog
                            if (AppLangId != mSelectedItem) {
                                AppLangId = mSelectedItem;
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mLangSelectedTV.setText(mData[AppLangId]);
                                        Toast.makeText(getActivity(),
                                                R.string.msg_change_language,
                                                Toast.LENGTH_SHORT).show();
                                        Utils.changeLang(getActivity(), AppLangId);

                                        /**Activity restart does not solve the problem,
                                         * because in the stack remain Activity to the previous locale
                                         * */
                                        //Utils.restartActivity(ActivitySettings.this);
                                        Utils.restartApp2(getActivity());
                                    }
                                });
                            }
                        }
                    });

            AlertDialog dialog = builder.create();

            return dialog;
        }

        @Override
        public void onClick(View v) {
            RadioButton rb = (RadioButton) v;
            switch (rb.getId()) {
                case R.id.rb_russian:
                    mSelectedItem = LANG_RU;
                    break;
                case R.id.rb_english:
                    mSelectedItem = LANG_EN;
                    break;
                case R.id.rb_ukrainian:
                    mSelectedItem = LANG_UK;
                    break;
                default:
                    break;
            }
        }
    }

    public static class SettingsPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }

    }

    public static class FragmentLicense extends BaseActivity {
        @Override
        protected void onResume() {
            super.onResume();
            Application.setCurrentActivity(this);
        }

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.fragment_license);
            TextView licenseTV = (TextView) findViewById(R.id.tv_license);
            licenseTV.setText(Html.fromHtml(getString(R.string.license_agreement)));
        }
    }
}
