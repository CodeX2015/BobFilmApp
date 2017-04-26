package club.bobfilm.app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

/**
 * Created by CodeX on 10.03.2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=23)
public class SiteParseTest {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        //you other setup here
    }

    private Document getParsedSite(final String url) {
        String mUserAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36 OPR/43.0.2442.991";
        Document docForParsing;
        try {
            ShadowLog.d("ParseSiteTest", "Try open url=" + url);
            docForParsing = Jsoup
                    .connect(url)
                    .timeout(5000)
                    .userAgent(mUserAgent)
                    .get();
            return docForParsing;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Test
    public void parseSite() {
        Document siteForParse = getParsedSite("https://sakh.tv/watch/the_expanse/02/07/baibako/");
        if (siteForParse == null) {
            ShadowLog.i("ParseSiteTest", "nothing for parsing");
            return;
        }
        Elements videoFile = siteForParse.select("span.jwvideo");
        ShadowLog.i("ParseSiteTest", videoFile.html());
        String nn = "";

    }
}
