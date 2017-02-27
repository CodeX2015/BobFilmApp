package club.bobfilm.app;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by CodeX on 05.05.2016.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class UtilsTest {
    @Test
    public void qwe(){
        Assert.assertTrue(2+2==4);
    }
}
