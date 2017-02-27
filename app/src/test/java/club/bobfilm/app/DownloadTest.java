package club.bobfilm.app;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;


/**
 * Created by CodeX on 12.09.2016.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class DownloadTest {
    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        //you other setup here
    }

    @Test
    public void downloadTest() {
//        Assert.assertTrue(true);
    }
}
