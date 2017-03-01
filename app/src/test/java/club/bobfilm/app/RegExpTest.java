package club.bobfilm.app;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import club.bobfilm.app.entity.FilmFile;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 23)
public class RegExpTest {
    static final Context mContext = RuntimeEnvironment.application;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        //you other setup here
    }

    String DETAILS_PATTERN = ".*?(ex.ru/)?(\\d)";
    //"(www.ex.ru\\/)+(?:\\\\d{0,9})+(\\\\?r)";

    public String[] getReviews() {
        return new String[]{
                "отзывов: 8",
                "отзывов: 1",
                "отзывов: 8",
                "отзывов: ",
                "отзывов:",
                "статей: 8",
                "статей: 1",
                "статей: 8",
                "статей: ",
                "статей:",
                "responds: 8",
                "responds: 1",
                "responds: 8",
                "responds: ",
                "responds:",
                "articles: 8",
                "articles: 1",
                "articles: 8",
                "articles: ",
                "articles:",
                ""
        };
    }

    @Config(qualifiers = "ru")
    @Test
    public void TestParseString() {
        for (String review : getReviews()) {
            ShadowLog.d("Parse_Result", parseReviewString(review));
        }
    }

    //    public static void setLocale(final String language )
//    {
//        final Locale locale = new Locale( language );
//        Robolectric.shadowOf(mContext.getResources().getConfiguration() ).setLocale( locale );
//        Locale.setDefault( locale );
//    }


    private static BufferedReader loadJsonObject(String link) throws Exception {
        String exampleBody = "{\"playlist\":[{\"comment\":\"Bobfilm\"," +
                "\"file\":\"http://bf.vbfcdn.net/" +
                "videos/B2EHO4nUU5IOfSd02W08qA,1488357153/" +
                "drugoy_mir_sleduyuschee_pokolenie_2016.flv\"}]}";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .header("Accept-Charset", "UTF-8")
                .header("Content-Type", "application/json")
                .url(link)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new Exception("HTTP_RESPONSE_CODE: " + response.code());
        }
        return new BufferedReader(new InputStreamReader(response.body().byteStream()));
    }

    @Test
    public void parsePlaylist() {
        Gson gson = new Gson();
        String url = "http://bobfilm.club/pl/playlist041838.txt";
        ArrayList<FilmFile> files;
        try {
            BufferedReader in = loadJsonObject(url);
            JsonObject jRequest = new Gson().fromJson(in, JsonObject.class);
            JsonArray playlist = jRequest.getAsJsonArray("playlist");
            files = new Gson().fromJson(playlist, new TypeToken<ArrayList<FilmFile>>() {
            }.getType());
            in.close();
            ShadowLog.d("Test_Parse_String", "1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        ShadowLog.d("Test_Parse_String", "1");
    }

    private String parseReviewString(String strReviews) {
        String strReviewsDefaultValue = mContext.getResources().getString(R.string.no_reviews);
        String trReviewsDefault = mContext.getResources().getString(R.string.parser_films_responds);

        String sReviews = "";
        String sReviewsCount = "";
        String sReviewsMask = mContext.getString(R.string.parser_films_responds_mask);

        //noinspection StatementWithEmptyBody
        if (strReviews.contains(sReviewsMask) && strReviews.contains(":")) {
            sReviews = strReviews.substring(0, strReviews.indexOf(":"));
            int indexOfColon = strReviews.indexOf(":");
            sReviewsCount = strReviews.substring(indexOfColon + 2 > strReviews.length()
                    ? indexOfColon + 1 : indexOfColon + 2);
        } else {
            //log.warn("string \'{}\' not contains \'{}\' and \':\'",
            //                                  strReviews, sReviewsMask);
        }
        boolean hasArticles = false;
        String sArticlesMask = mContext.getString(R.string.parser_films_articles_mask);
        if (strReviews.toLowerCase().contains(sArticlesMask.toLowerCase())) {
            hasArticles = true;
//            String parseRes = String.format("%1$s sArticlesMask: %2$s, strReviews: %3$s, true",
//                    strReviews, sArticlesMask, strReviews);
//            ShadowLog.d("Test_Parse_String", parseRes);
        }
        sReviews = (sReviewsCount.equalsIgnoreCase("")
                || sReviewsCount.equalsIgnoreCase("0")) ?
                strReviewsDefaultValue : sReviewsCount;

        String parseRes = String.format("%1$s sArticlesMask: %2$s, strReviews: %3$s, true",
                strReviews, sArticlesMask, strReviews);
        ShadowLog.d("Test_Parse_String", parseRes);

        return sReviews;
    }

    //@Test
    public void isDetailsLink() {
        int matchesCount = 0;
        int noMatchesCount = 0;
        String[] links = getLinks();
        for (String link : links) {
            Matcher matcher = Pattern.compile(DETAILS_PATTERN).matcher(link);
            boolean result = matcher.matches();
            if (result) {
                matchesCount++;
                ShadowLog.d("TEST_PATTERN", "Matches: " + link);
            } else {
                noMatchesCount++;
                ShadowLog.d("TEST_PATTERN", "NoMatches: " + link);
            }
        }
        ShadowLog.d("TEST_PATTERN", "matches links: " + matchesCount + " noMatchesCount: " + noMatchesCount);
    }

    public String[] getLinks() {
        return new String[]{
                "http://www.ex.ru/104143522?r=2,23775",
                "http://www.ex.ru/1022?r=2,23775",
                "http://www.ex.ru/ru/video/",
                "http://www.ex.ru",
                "http://www.ya.ru",
                "https://www.ex.ru",
                "https://www.ya.ru",
                "http://www.ex.ru/"
        };
    }


    //@Test
    public void checkRandom() {
        for (int i = 0; i < 5; i++) {
            getRandom();
        }
//        Assert.assertTrue(true);
    }


    public void getRandom() {
        int min = 2;
        int max = 6;
        Random random = new Random();
        int result = random.nextInt(max - min + 1) + min;
        ShadowLog.d("TEST_SPLIT", "rnd: " + result);
    }

    public String getSearchHints() {
        return "solidworks solace 2015 soldier of fortune soldier s girl 2003 solace solus solidworks 2016 solidworks 2015 solidworks 2012 на русском solo andata";

    }

    //    @Test
    public void splitByMask() {
        int matchesCount = 0;
        int noMatchesCount = 0;
        String mask = "sol";
        String searchHints = getSearchHints();
        String[] result = searchHints.split(" " + mask);
        for (int i = 0; i < result.length; i++) {
            if (i > 0) {
                result[i] = mask + result[i];
            }
            ShadowLog.d("TEST_SPLIT", "hints count: " + result.length + " resultItem: " + result[i]);
        }
    }

    public String[] getFiles() {
        return new String[]{
                "[Озвучка SoftBox] Блистательное обольщение 01 серия 720p.mp4",
                "[Озвучка SoftBox] Блистательное обольщение 02 серия 720p.mp4",
                "[Озвучка SoftBox] Блистательное обольщение 02 серия 720p.mkv",
                "[Озвучка SoftBox] Блистательное обольщение 02 серия .mp4",
                "Блистательное обольщение 02 серия.mp4",
                "Блистательное обольщение 02 серия.MP4",
                "Блистательное обольщение 02 серия[].MP4",
                "Блистательное обольщение 02 серия .MP4",
                "Блистательное обольщение 02 серия 3D.MP4",
                "Блистательное обольщение 02 серия 3D.Mp4",
                "Блистательное обольщение 02 серия 3D..Mp4",
                "Блистательное обольщение 02 серия 3D.8.Mp4",
                "Блистательное обольщение 02 серия 3D.8.imG",
                ".imG 3D.8.mp4",
                ".mp4 3D.8.iomg",
                ".mp4",
                " .mp4",
                "1.mp4",
                ".mp4.img"
        };
    }

    public String[] getPatterns() {
        return new String[]{
                "/\\.(?:mp4)$/i",
//                "^.*\\.(?!mp4$)[^.]+$",
                "/\\.(mp4)$/",
                "(\\.(?i)(mp4))$",
                "^[^.]*(\\.(?i)(mp4))?$",
                "(?i).*\\.(img|mkv)"
        };
    }

    String VIDEO_PATTERN = "(?i).*\\.(mp4|mpg|avi|mkv|asf|mov|qt|avchd|flv|wmv|vob|ifo|dub|m4v)";

    //    @Test
    public void isVideo() {
        int matchesCount = 0;
        int noMatchesCount = 0;
        String[] files = getFiles();
        for (String file : files) {
            Matcher matcher = Pattern.compile(VIDEO_PATTERN).matcher(file);
            boolean result = matcher.matches();
            if (result) {
                matchesCount++;
            } else {
                noMatchesCount++;
                ShadowLog.d("TEST_PATTERN", "NoMatches: " + file);
            }
        }
        ShadowLog.d("TEST_PATTERN", "matches files: " + matchesCount + " noMatchesCount: " + noMatchesCount);
    }

    //    @Test
    public void findPattern() {
        String[] patterns = getPatterns();
        String[] files = getFiles();
        for (String pattern : patterns) {
            for (String file : files) {
                Matcher matcher = Pattern.compile(pattern).matcher(file);
                boolean result = matcher.matches();
                if (result) {
                    ShadowLog.d("TEST_PATTERN", "file: " + file + " pattern: " + pattern + " result: " + result);
                }
            }
        }
    }

}