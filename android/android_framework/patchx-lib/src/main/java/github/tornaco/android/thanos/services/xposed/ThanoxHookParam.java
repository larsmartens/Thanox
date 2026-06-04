package github.tornaco.android.thanos.services.xposed;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class ThanoxHookParam {
    public final Member method;
    public final Object thisObject;
    public final Object[] args;
    public final boolean isBefore;

    public Object result;
    public Throwable throwable;

    private boolean resultChanged;
    private boolean throwableChanged;
    private boolean skipOriginal;

    public ThanoxHookParam(Member method, Object thisObject, Object[] args,
                           Object result, Throwable throwable, boolean isBefore) {
        this.method = method;
        this.thisObject = thisObject;
        this.args = args;
        this.result = result;
        this.throwable = throwable;
        this.isBefore = isBefore;
    }

    public void setResult(Object result) {
        this.result = result;
        this.resultChanged = true;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
        this.throwableChanged = true;
    }

    public boolean isResultChanged() {
        return resultChanged;
    }

    public boolean isThrowableChanged() {
        return throwableChanged;
    }

    public boolean isSkipOriginal() {
        return skipOriginal;
    }

    public Method getMethodOrNull() {
        return method instanceof Method ? (Method) method : null;
    }

    public void returnAndSkip(Object value) {
        setResult(value);
        this.skipOriginal = true;
    }

    public void throwAndSkip(Throwable value) {
        setThrowable(value);
        this.skipOriginal = true;
    }
}
