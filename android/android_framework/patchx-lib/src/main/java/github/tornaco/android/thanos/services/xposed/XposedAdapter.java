package github.tornaco.android.thanos.services.xposed;

import java.lang.reflect.Member;
import java.util.function.Consumer;

public interface XposedAdapter {

    void log(String message);

    void log(String message, Throwable throwable);

    void hookBefore(Member member, Consumer<ThanoxHookParam> callback);

    void hookAfter(Member member, Consumer<ThanoxHookParam> callback);
}
