package github.tornaco.android.thanos.services.xposed;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ThanoxHookParamTest {

    @Test
    public void setResultSkipsOriginalInBeforeHook() {
        ThanoxHookParam param = new ThanoxHookParam(null, null, new Object[0], null, null, true);

        param.setResult("blocked");

        assertTrue(param.isResultChanged());
        assertTrue(param.isSkipOriginal());
        assertFalse(param.isThrowableChanged());
    }

    @Test
    public void setThrowableSkipsOriginalInBeforeHook() {
        ThanoxHookParam param = new ThanoxHookParam(null, null, new Object[0], null, null, true);

        param.setThrowable(new IllegalStateException("blocked"));

        assertTrue(param.isThrowableChanged());
        assertTrue(param.isSkipOriginal());
        assertFalse(param.isResultChanged());
    }

    @Test
    public void setResultDoesNotSkipOriginalInAfterHook() {
        ThanoxHookParam param = new ThanoxHookParam(null, null, new Object[0], null, null, false);

        param.setResult("override");

        assertTrue(param.isResultChanged());
        assertFalse(param.isSkipOriginal());
    }

    @Test
    public void setThrowableDoesNotSkipOriginalInAfterHook() {
        ThanoxHookParam param = new ThanoxHookParam(null, null, new Object[0], null, null, false);

        param.setThrowable(new IllegalStateException("override"));

        assertTrue(param.isThrowableChanged());
        assertFalse(param.isSkipOriginal());
    }
}
