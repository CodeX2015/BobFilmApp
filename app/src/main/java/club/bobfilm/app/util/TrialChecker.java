package club.bobfilm.app.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import club.bobfilm.app.R;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by CodeX on 02.03.2017.
 */

public class TrialChecker {
    private static final String cryptoPass = "sup3rS3xy";
    private static final Logger log = LoggerFactory.getLogger(TrialChecker.class);
    private static final String APP_LAUNCHES_COUNT = "AppLaunchesCount";
    private static final String APP_INSTALL_ID = "AppInstallId";
    private static final int LAUNCH_COUNT_MULTIPLIER = 10;
    private static final int TRIAL_PERIOD_DAY = 30;

    private final DialogInterface.OnClickListener mDialogAction;

    private Context mContext;
    private long AppId;
    private int AppLaunchesCount;

    public TrialChecker(Context context, DialogInterface.OnClickListener dialogAction) {
        mContext = context;
        mDialogAction = dialogAction;
    }

    public boolean isAppActualYet() {
        if (isAppTrialOver()) {
            saveSettings();
            showTrialOverMsg();
            return false;
        }
        return true;
    }

    @SuppressLint("CommitPrefEdits")
    private void saveSettings() {
        //Todo convert value to string and encrypt
        //log.debug("saveSettings: LangId={}", ActivitySettings.AppLangId);
        SharedPreferences prefs = mContext.getSharedPreferences(Utils.PREF_APP_INSTALL_ID, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(APP_INSTALL_ID, AppId);
        editor.putInt(APP_LAUNCHES_COUNT, AppLaunchesCount);
        editor.commit();
    }

    private void loadSettings() {
        //Todo convert value from string and decrypt
        SharedPreferences prefs = mContext.getSharedPreferences(Utils.PREF_APP_INSTALL_ID, MODE_PRIVATE);
        AppId = prefs.getLong(APP_INSTALL_ID, 0);
        AppLaunchesCount = prefs.getInt(APP_LAUNCHES_COUNT, 0) + 1;
    }

    private void showTrialOverMsg() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, android.R.style.Theme_Dialog);
        builder.setTitle(R.string.dialog_header_information);
        CharSequence message = mContext.getString(R.string.msg_trial_over);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_btn_ok, mDialogAction)
                .create()
                .show();
    }

    private boolean isAppTrialOver() {
        int trialDay = TRIAL_PERIOD_DAY;
        int trialLaunch = trialDay * LAUNCH_COUNT_MULTIPLIER;
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/." + APP_INSTALL_ID;
        loadSettings();
        String installDate = convertMillisecondsToDate("", AppId);
        log.debug("App was installed at {}", installDate);
        if (AppLaunchesCount > trialLaunch) {
            log.debug("Trial period by AppLaunchesCount is over ({} > {})"
                    , AppLaunchesCount, trialLaunch);
            return true;
        }
        if (AppId != 0) {
            log.info("ID from prefs");
            log.debug("From prefs: AppId = {}", AppId);
        } else {
            if (checkFileExist(filePath)) {
                log.info("ID from file");
                String appId = readFromFile(filePath);
                AppId = Long.parseLong(appId);
                log.debug("From file: AppId = {}", AppId);
            } else {
                log.info("create new ID");
                AppId = System.currentTimeMillis();
                writeToFile(filePath, String.valueOf(AppId));
                log.debug("First run: AppId = {}", AppId);
            }
            saveSettings();
        }
        return isTrialPeriodOver(trialDay);
    }

    private boolean checkFileExist(String filePath) {
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

    private String readFromFile(String filePath) {
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

    private void writeToFile(String filePath, String body) {
        try {
            // Create a new output file stream
            java.io.File file = new java.io.File(filePath);
            //noinspection ResultOfMethodCallIgnored
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

    private boolean isTrialPeriodOver(int trialDay) {
        long currentTime = System.currentTimeMillis();
        long trialPeriod = TimeUnit.MILLISECONDS.convert(trialDay, TimeUnit.DAYS);
        long trialDateOver = AppId + trialPeriod;
        log.debug("install date {}, date over {}, current date {}, isTrialPeriodOver={}",
                convertMillisecondsToDate("", AppId),
                convertMillisecondsToDate("", trialDateOver),
                convertMillisecondsToDate("", currentTime),
                (currentTime > trialDateOver));
        return (currentTime > trialDateOver);
    }

    private String convertMillisecondsToDate(String dateFormat, long milliSeconds) {
        if (dateFormat == null || dateFormat.equalsIgnoreCase("")) {
            dateFormat = "dd/MM/yyyy";
        }
        @SuppressLint("SimpleDateFormat") DateFormat formatter = new SimpleDateFormat(dateFormat);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    @SuppressWarnings("TryWithIdenticalCatches")
    private String encryptDESString(String value) {
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
    private String decryptDESString(String value) {
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
}
