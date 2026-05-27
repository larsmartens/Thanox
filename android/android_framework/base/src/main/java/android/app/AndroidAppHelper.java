package android.app;

import github.tornaco.android.thanos.core.util.AppUtils;

public final class AndroidAppHelper {
    private AndroidAppHelper() {
    }

    public static String currentPackageName() {
        return AppUtils.currentProcessName();
    }
}
