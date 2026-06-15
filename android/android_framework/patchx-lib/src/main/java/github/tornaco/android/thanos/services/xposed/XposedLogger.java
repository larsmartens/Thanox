package github.tornaco.android.thanos.services.xposed;

public class XposedLogger {
  public static final String LOG_PREFIX = "Thanos_";

  public static boolean isDebug() {
    return Boolean.TRUE;
  }

  public static void log(String tag, String f, Object... a) {
    if (isDebug()) XposedRuntime.current().log(tag + "\t" + String.format(f, a));
  }
}
