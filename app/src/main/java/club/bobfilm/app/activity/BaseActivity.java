package club.bobfilm.app.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import club.bobfilm.app.R;
import club.bobfilm.app.entity.Film;
import club.bobfilm.app.entity.Section;
import club.bobfilm.app.util.Utils;

/**
 * Created by CodeX on 11.05.2016.
 */
public class BaseActivity extends AppCompatActivity {

    private Logger log = LoggerFactory.getLogger(BaseActivity.class);

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_details, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent myIntent = null;
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        //noinspection SimplifiableIfStatement
        if (item.getItemId() == R.id.action_settings) {
            //myIntent = new Intent(this, ActivitySettings.class);
            myIntent = new Intent(this, ActivitySettings.class);
            myIntent.putExtra("settings", 0);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        if (item.getItemId() == R.id.action_archive) {
            myIntent = new Intent(this, ActivityTabArchive.class);
            myIntent.putExtra(ActivityTabArchive.EXTRA_TAB_POSITION, ActivityTabArchive.FRAGMENT_DOWNLOADS);
        }
        if (myIntent != null) {
//            myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(myIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    protected void isCommentsExists(String filmReviews, String filmReviewsUrl) {
        if (!filmReviews.equalsIgnoreCase(getResources().getString(R.string.no_reviews))) {
            startActivityComments(filmReviewsUrl);
        } else {
            Toast.makeText(this, R.string.msg_no_reviews, Toast.LENGTH_SHORT).show();
        }
    }

    protected void checkDetails(final Film film, Section category, ArrayList<Film> addressList) {
        if (!film.isHasArticle()) {
            startActivityDetails(film);
        } else {
            //noinspection unchecked
//            getListOfSubCategories(film);
            startActivitySubCategories(film, category, addressList);
        }
    }

    protected void startActivityComments(String commentsUrl) {
        Intent myIntent = new Intent(this, ActivityComments.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        myIntent.putExtra(Utils.ARG_COMMENTS_URL, commentsUrl);
        startActivity(myIntent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    protected void startActivityLicense(){
        Intent intent = new Intent(this, ActivitySettings.FragmentLicense.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    protected void startActivityDetails(Film film) {
        Intent myIntent = new Intent(this, ActivityDetails.class);
        myIntent.putExtra(Utils.ARG_FILM_DETAILS, film);
        this.startActivityForResult(myIntent, ActivityTabMain.REQUEST_FILMS_DETAILS);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    protected void startActivitySubCategories(Film film, Section category, ArrayList<Film> addressList) {
        Intent myIntent = new Intent(this, ActivitySubCategories.class);
        myIntent.putExtra(Utils.ARG_SUB_CATEGORIES, film);
        myIntent.putExtra(Utils.ARG_SERIALIZABLE_SECTION, category);
        myIntent.putExtra(Utils.ARG_ADDRESS_LIST, addressList);

//        myIntent.putExtra(Utils.ARG_NEXT_PAGE_URL, film.getNextPageUrl());
//        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(myIntent);
//        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

}
