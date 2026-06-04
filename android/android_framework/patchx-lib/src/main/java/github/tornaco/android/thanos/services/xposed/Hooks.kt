package github.tornaco.android.thanos.services.xposed

import java.lang.reflect.Method

fun beforeConstruct(
    clazz: Class<*>,
    log: (String) -> Unit,
    beforeConstruct: (param: ThanoxHookParam) -> Unit,
) {
    val constructors = clazz.declaredConstructors
    constructors.forEach { constructor ->
        kotlin.runCatching {
            XposedRuntime.current().hookBefore(constructor) { param ->
                kotlin.runCatching {
                    beforeConstruct(param)
                }.onFailure {
                    log("beforeConstruct-$clazz-${it.stackTraceToString()}")
                }
            }
        }.onFailure {
            log("beforeConstruct-$clazz-${it.stackTraceToString()}")
        }
    }
}

fun afterConstruct(
    clazz: Class<*>,
    log: (String) -> Unit,
    afterConstruct: (param: ThanoxHookParam) -> Unit,
) {
    val constructors = clazz.declaredConstructors
    constructors.forEach { constructor ->
        kotlin.runCatching {
            XposedRuntime.current().hookAfter(constructor) { param ->
                kotlin.runCatching {
                    afterConstruct(param)
                }.onFailure {
                    log("afterConstruct-$clazz-${it.stackTraceToString()}")
                }
            }
        }.onFailure {
            log("afterConstruct-$clazz-${it.stackTraceToString()}")
        }
    }
}

fun beforeMethod(
    clazz: Class<*>,
    methodName: String,
    log: (String) -> Unit,
    beforeMethod: (param: ThanoxHookParam) -> Unit,
) {
    val methods = clazz.declaredMethods.filter { it.name == methodName }
    require(methods.isNotEmpty()) {
        "beforeMethod, unable to hook this method: $clazz#$methodName"
    }
    methods.forEach { method ->
        XposedRuntime.current().hookBefore(method) { param ->
            kotlin.runCatching {
                beforeMethod(param)
            }.onFailure {
                log("beforeHookedMethod-$clazz-$methodName ${it.stackTraceToString()}")
            }
        }
    }
    log("beforeMethod, hooked methods: $methods for method: $clazz#$methodName")
}

fun afterMethod(
    clazz: Class<*>,
    methodName: String,
    log: (String) -> Unit,
    afterMethod: (param: ThanoxHookParam) -> Unit,
) {
    val methods = clazz.declaredMethods.filter { it.name == methodName }
    require(methods.isNotEmpty()) {
        "afterMethod, unable to hook this method: $clazz#$methodName"
    }
    methods.forEach { method ->
        XposedRuntime.current().hookAfter(method) { param ->
            kotlin.runCatching {
                afterMethod(param)
            }.onFailure {
                log("afterHookedMethod-$clazz-$methodName-${it.stackTraceToString()}")
            }
        }
    }
    log("afterMethod, hooked methods: $methods for method: $clazz#$methodName")
}

fun afterMethod(
    method: Method,
    log: (String) -> Unit,
    afterMethod: (param: ThanoxHookParam) -> Unit,
) {
    XposedRuntime.current().hookAfter(method) { param ->
        kotlin.runCatching {
            afterMethod(param)
        }.onFailure {
            log("afterHookedMethod-$method-${it.stackTraceToString()}")
        }
    }
    log("afterMethod, hooked method: $method")
}
