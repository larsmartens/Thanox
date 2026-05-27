package github.tornaco.android.thanos.services.xposed.compat;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import io.github.libxposed.api.XposedInterface;

public final class Api101Bridge {
    private static final Map<Member, MemberContext> MEMBER_CONTEXTS = new IdentityHashMap<>();

    private Api101Bridge() {
    }

    public static synchronized XC_MethodHook.Unhook hookMember(Member member, XC_MethodHook callback) {
        MemberContext context = MEMBER_CONTEXTS.get(member);
        if (context == null) {
            context = createContext(member);
            MEMBER_CONTEXTS.put(member, context);
        }
        context.callbacks.add(callback);
        return callback.new Unhook(member, context.handle);
    }

    public static void log(int priority, String tag, String msg, Throwable tr) {
        XposedInterface xposed = RuntimeHolder.getXposed();
        if (xposed != null) {
            if (tr == null) {
                xposed.log(priority, tag, msg);
            } else {
                xposed.log(priority, tag, msg, tr);
            }
            return;
        }
        if (tr == null) {
            Log.println(priority, tag, msg);
        } else {
            Log.println(priority, tag, msg + '\n' + Log.getStackTraceString(tr));
        }
    }

    private static MemberContext createContext(Member member) {
        XposedInterface xposed = RuntimeHolder.requireXposed();
        LegacyHooker hooker = new LegacyHooker(member);
        XposedInterface.HookHandle handle;
        if (member instanceof Method method) {
            handle = xposed.hook(method).intercept(hooker);
        } else if (member instanceof Constructor<?> constructor) {
            handle = xposed.hook(constructor).intercept(hooker);
        } else {
            throw new IllegalArgumentException("Unsupported hook member: " + member);
        }
        return new MemberContext(member, hooker, handle);
    }

    private static final class MemberContext {
        final Member member;
        final LegacyHooker hooker;
        final XposedInterface.HookHandle handle;
        final java.util.List<XC_MethodHook> callbacks = new java.util.concurrent.CopyOnWriteArrayList<>();

        MemberContext(Member member, LegacyHooker hooker, XposedInterface.HookHandle handle) {
            this.member = member;
            this.hooker = hooker;
            this.handle = handle;
            this.hooker.bind(this);
        }
    }

    private static final class LegacyHooker implements XposedInterface.Hooker {
        private final Member member;
        private MemberContext context;

        LegacyHooker(Member member) {
            this.member = member;
        }

        void bind(MemberContext context) {
            this.context = context;
        }

        @Override
        public Object intercept(XposedInterface.Chain chain) throws Throwable {
            XC_MethodHook.MethodHookParam beforeParam = new XC_MethodHook.MethodHookParam();
            beforeParam.method = member;
            beforeParam.thisObject = chain.getThisObject();
            beforeParam.args = chain.getArgs().toArray(new Object[0]);

            for (XC_MethodHook callback : context.callbacks) {
                callback.callBeforeHookedMethod(beforeParam);
            }

            Object result = null;
            Throwable throwable = null;
            if (beforeParam.returnEarly) {
                result = beforeParam.result;
                throwable = beforeParam.throwable;
            } else {
                try {
                    result = chain.proceed(beforeParam.args);
                } catch (Throwable t) {
                    throwable = t;
                }
            }

            XC_MethodHook.MethodHookParam afterParam = new XC_MethodHook.MethodHookParam();
            afterParam.method = member;
            afterParam.thisObject = chain.getThisObject();
            afterParam.args = beforeParam.args;
            afterParam.result = result;
            afterParam.throwable = throwable;
            afterParam.returnEarly = beforeParam.returnEarly;

            for (XC_MethodHook callback : context.callbacks) {
                callback.callAfterHookedMethod(afterParam);
            }

            if (afterParam.throwable != null) {
                throw afterParam.throwable;
            }
            return afterParam.result;
        }
    }
}
