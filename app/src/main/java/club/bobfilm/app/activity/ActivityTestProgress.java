package club.bobfilm.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import club.bobfilm.app.Application;
import club.bobfilm.app.ProgressBarIndeterminateDeterminate;
import club.bobfilm.app.R;

/**
 * Created by CodeX on 06.10.2016.
 */

public class ActivityTestProgress extends AppCompatActivity implements View.OnClickListener {
    private Logger log = LoggerFactory.getLogger(getClass());
    private ProgressBarIndeterminateDeterminate mProgress;
    private Button mProgressUp;
    private Button mProgressDown;
    int progress = 0;

    @Override
    protected void onRestart() {
        super.onRestart();
        Application.setCurrentActivity(this);
    }



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Application.setCurrentActivity(this);
        setContentView(R.layout.activity_test_progress);
        mProgress = (ProgressBarIndeterminateDeterminate) findViewById(R.id.pb_download_progress);
    }


    @Override
    public void onClick(View v) {
        log.info("{} button pressed", v.getId());
        switch (v.getId()) {
            case R.id.btn_progress_up:
                progress = progress + 5;
                mProgress.setProgress(progress);
                break;
            case R.id.btn_progress_down:
                progress = progress - 5;
                mProgress.setProgress(progress);
                break;
            case R.id.btn_progress_reset:
                mProgress.setIndeterminate(true);
                break;
            default:
                log.info("unknown button pressed");
                break;
        }
    }
}
