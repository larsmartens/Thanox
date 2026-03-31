# ProxyBuilder Class Proxy Guide

## Purpose

This document explains how to create a class proxy with `com.android.dx.stock.ProxyBuilder` in this repository.

It is written so an AI coding tool can use it as an implementation contract.

It is also self-contained: the critical source code and usage patterns are embedded below so the document can be used outside this repository.

## What ProxyBuilder Does

`ProxyBuilder` generates a subclass of a concrete or abstract class at runtime and routes overridable method calls into a `java.lang.reflect.InvocationHandler`.

This is similar to `java.lang.reflect.Proxy`, but for classes instead of interfaces.

The most important behavior comes from the local `ProxyBuilder` implementation shown in the embedded excerpts below.

## Embedded Source: Builder Surface

These are the builder methods that matter during implementation.

```java
public ProxyBuilder<T> parentClassLoader(ClassLoader parent) {
    parentClassLoader = parent;
    return this;
}

public ProxyBuilder<T> handler(InvocationHandler handler) {
    this.handler = handler;
    return this;
}

public ProxyBuilder<T> dexCache(File dexCacheParent) {
    dexCache = new File(dexCacheParent, "v" + VERSION);
    dexCache.mkdir();
    return this;
}

public ProxyBuilder<T> implementing(Class<?>... interfaces) {
    List<Class<?>> list = this.interfaces;
    for (Class<?> i : interfaces) {
        if (!i.isInterface()) {
            throw new IllegalArgumentException("Not an interface: " + i.getName());
        }
        if (!list.contains(i)) {
            list.add(i);
        }
    }
    return this;
}

public ProxyBuilder<T> constructorArgValues(Object... constructorArgValues) {
    this.constructorArgValues = constructorArgValues;
    return this;
}

public ProxyBuilder<T> constructorArgTypes(Class<?>... constructorArgTypes) {
    this.constructorArgTypes = constructorArgTypes;
    return this;
}

public ProxyBuilder<T> onlyMethods(Method[] methods) {
    this.methods = methods;
    return this;
}

public ProxyBuilder<T> withSharedClassLoader() {
    this.sharedClassLoader = true;
    return this;
}

public ProxyBuilder<T> markTrusted() {
    this.markTrusted = true;
    return this;
}
```

## Embedded Source: Build Contract

This is the exact contract enforced by `build()`.

```java
public T build() throws IOException {
    check(handler != null, "handler == null");
    check(
            constructorArgTypes.length == constructorArgValues.length,
            "constructorArgValues.length != constructorArgTypes.length");
    Class<? extends T> proxyClass = buildProxyClass();
    Constructor<? extends T> constructor;
    try {
        constructor = proxyClass.getConstructor(constructorArgTypes);
    } catch (NoSuchMethodException e) {
        throw new IllegalArgumentException(
                "No constructor for "
                        + baseClass.getName()
                        + " with parameter types "
                        + Arrays.toString(constructorArgTypes));
    }
    T result;
    try {
        result = constructor.newInstance(constructorArgValues);
    } catch (InstantiationException e) {
        throw new AssertionError(e);
    } catch (IllegalAccessException e) {
        throw new AssertionError(e);
    } catch (InvocationTargetException e) {
        throw launderCause(e);
    }
    setInvocationHandler(result, handler);
    return result;
}
```

## Core Model

`ProxyBuilder` works by:

1. Generating a new subclass of the target class.
2. Overriding eligible methods.
3. Sending overridden method calls to the supplied `InvocationHandler`.
4. Letting the handler either:
   - return a custom value,
   - delegate to an existing original object with `method.invoke(original, args)`, or
   - call the generated proxy's super implementation with `ProxyBuilder.callSuper(proxy, method, args)`.

## Hard Requirements

Before using `ProxyBuilder`, verify all of these:

1. The target type is not `final`.
2. The target type is accessible from the class loader used to generate the proxy.
3. A valid dex cache directory is provided with `.dexCache(...)`.
4. A non-null invocation handler is provided with `.handler(...)`.
5. If the target has no no-arg constructor, both `.constructorArgTypes(...)` and `.constructorArgValues(...)` are provided and match exactly.

## What Methods Can Be Intercepted

`ProxyBuilder` only intercepts methods that are overridable in the generated subclass.

Intercepted by default:

- public instance methods
- protected instance methods
- package-private methods only when shared class loader mode makes them inheritable in practice for the generated proxy
- abstract methods

Never intercepted:

- `final` methods
- `static` methods
- `private` methods
- `finalize()`

Important implication:

If the behavior you need is implemented in a `final`, `static`, or `private` method, `ProxyBuilder` is the wrong mechanism.

## Embedded Source: Method Selection Rules

This excerpt is the reason for the interception limits.

```java
if ((method.getModifiers() & Modifier.FINAL) != 0) {
    seenFinalMethods.add(entry);
    sink.remove(entry);
    continue;
}
if ((method.getModifiers() & STATIC) != 0) {
    continue;
}
if (!Modifier.isPublic(method.getModifiers())
        && !Modifier.isProtected(method.getModifiers())
        && (!sharedClassLoader || Modifier.isPrivate(method.getModifiers()))) {
    continue;
}
if (method.getName().equals("finalize") && method.getParameterTypes().length == 0) {
    continue;
}
```

## Constructor Behavior

`build()` creates an instance of the generated proxy class.

Defaults:

- `build()` uses the target class no-arg constructor.

If the target constructor requires parameters, always supply both:

- `.constructorArgTypes(...)`
- `.constructorArgValues(...)`

The lengths must match exactly.

Example from this repo:

```java
return ProxyBuilder.forClass(IFWHelper.INSTANCE.ifwClass(systemServerClassLoader))
        .dexCache(dexCacheDir)
        .constructorArgTypes(
                IFWHelper.INSTANCE.amsInterfaceClass(systemServerClassLoader),
                Handler.class)
        .constructorArgValues(amsInterface, handler)
        .withSharedClassLoader()
        .markTrusted()
        .handler(invocationHandler)
        .build();
```

## Delegation Patterns

There are two valid proxy patterns in this repository.

### Pattern A: Wrap an Existing Original Object

Use this when the system already created a live object and you want to replace a field with a proxy that forwards most calls to the original object.

This is the main pattern used in this repo.

Inside the invocation handler:

```java
method.setAccessible(true);
return method.invoke(original, args);
```

Use this pattern when:

- a framework service already exists,
- you are replacing a field such as `mUsageStatsService`, `mPackageManagerInt`, or `mIntentFirewall`,
- you need selective interception before forwarding to the original.

### Pattern B: Call the Generated Super Implementation

Use this when you want the proxy instance itself to call its superclass implementation, not a separate original object.

Inside the invocation handler:

```java
return ProxyBuilder.callSuper(proxy, method, args);
```

Use this pattern when:

- there is no separate original instance to delegate to,
- the proxy itself should behave like a subclass with selective overrides.

## Repository-Specific Rules

When proxying Android framework or system-server classes in this repository, use the following defaults unless there is a strong reason not to.

1. Use `BaseProxyFactory` to standardize dex cache creation.
2. Use `.withSharedClassLoader()` for framework classes loaded by the system server class loader.
3. Use `.markTrusted()` for generated proxies that may touch hidden or blacklisted framework APIs.
4. Resolve the target class with the same class loader as the original framework object.
5. If replacing an existing object field, keep a reference to the original object and delegate to it from the handler.
6. Set `method.setAccessible(true)` before reflective invocation when needed.

Reason for `.withSharedClassLoader()`:

- It makes the generated proxy use the target class's class loader, which is important for package visibility and framework type compatibility.

Reason for `.markTrusted()`:

- The local `ProxyBuilder` implementation marks generated classes trusted to avoid hidden-API access failures on newer Android versions.

## Embedded Source: BaseProxyFactory Helper

This repository uses a helper factory to standardize the dex cache path.

```java
public abstract class BaseProxyFactory<T> {

    public final T newProxy(T original, File baseDataDir) {
        try {
            return onCreateProxy(original, dxCacheDir(baseDataDir));
        } catch (Throwable e) {
            XLog.e(e, "BaseProxyFactory fail create proxy by %s for %s", getClass(), original);
            return null;
        }
    }

    protected abstract T onCreateProxy(T original, File dexCacheDir) throws Exception;

    private File dxCacheDir(File baseDir) throws IOException {
        File dx = new File(baseDir, "dx");
        XLog.i("BaseProxyFactory Using dxCacheDir as dx dir: %s", dx);
        Files.createParentDirs(new File(dx, "dummy"));
        return dx;
    }
}
```

## Standard Implementation Recipe For This Repo

If an AI tool is asked to add a new `ProxyBuilder`-based hook in this repo, it should follow this sequence.

1. Identify the exact target class to proxy.
2. Identify the class loader that loaded that target class.
3. Determine whether an original live instance already exists.
4. Create a small factory extending `BaseProxyFactory<T>` when the proxy is created from a base data directory.
5. Build the proxy with:
   - `ProxyBuilder.forClass(targetClass)`
   - `.dexCache(dexCacheDir)`
   - optional constructor args
   - `.withSharedClassLoader()` for framework types
   - `.markTrusted()` for framework or hidden API access
   - `.handler(...)`
   - `.build()`
6. In the handler, intercept only the method names or signatures you actually need.
7. Forward every untouched call to the original object or to `ProxyBuilder.callSuper(...)`.
8. Replace the original field or return the proxy to the caller.

## Copy-Ready Template: Wrap Existing Object

```java
private static class TargetProxyFactory extends BaseProxyFactory<Object> {
    private final ClassLoader classLoader;

    TargetProxyFactory(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    protected Object onCreateProxy(Object original, File dexCacheDir) throws Exception {
        if (original == null) {
            return null;
        }

        Class<?> targetClass = resolveTargetClass(classLoader);

        return ProxyBuilder.forClass(targetClass)
                .dexCache(dexCacheDir)
                .withSharedClassLoader()
                .markTrusted()
                .handler(new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        method.setAccessible(true);

                        if ("targetMethod".equals(method.getName())) {
                            Object shortCircuit = tryHandle(args);
                            if (shortCircuit != null) {
                                return shortCircuit;
                            }
                        }

                        return method.invoke(original, args);
                    }
                })
                .build();
    }
}
```

## Copy-Ready Template: Use Super Instead Of Original

```java
Class<?> targetClass = resolveTargetClass(classLoader);

Object proxy = ProxyBuilder.forClass(targetClass)
        .dexCache(dexCacheDir)
        .withSharedClassLoader()
        .markTrusted()
        .handler(new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("targetMethod".equals(method.getName())) {
                    return customValue();
                }
                return ProxyBuilder.callSuper(proxy, method, args);
            }
        })
        .build();
```

## Optional Builder Features

### `.implementing(Class<?>... interfaces)`

Use this when the generated proxy must also implement extra interfaces.

Every supplied type must be an interface.

### `.onlyMethods(Method[] methods)`

Use this only when you intentionally want to proxy a restricted method subset.

Default behavior is usually better because it automatically collects all eligible methods.

### `.parentClassLoader(ClassLoader parent)`

Use this when you are not using shared class loader mode and must control the proxy generation parent loader explicitly.

In this repo, framework hooks usually prefer `.withSharedClassLoader()` instead.

### `.buildProxyClass()`

Use this only when you need the generated class itself, not an already-instantiated proxy.

If you use `buildProxyClass()`, you must later call:

```java
ProxyBuilder.setInvocationHandler(instance, handler);
```

## Embedded Source: callSuper

This is how `ProxyBuilder.callSuper(...)` works internally.

```java
public static Object callSuper(Object proxy, Method method, Object... args) throws Throwable {
    try {
        return proxy
                .getClass()
                .getMethod(superMethodName(method), method.getParameterTypes())
                .invoke(proxy, args);
    } catch (InvocationTargetException e) {
        throw e.getCause();
    }
}
```

Practical meaning:

- `callSuper` only works on a proxy instance generated by `ProxyBuilder`
- it calls the generated `super$...` bridge method, not an unrelated original object
- use it when you want subclass-style pass-through behavior

## Failure Modes And Their Meaning

### `IllegalArgumentException: handler == null`

You forgot to call `.handler(...)`.

### `IllegalArgumentException: constructorArgValues.length != constructorArgTypes.length`

Constructor metadata does not match constructor values.

### `IllegalArgumentException: No constructor for ...`

The supplied constructor signature does not exist on the target class.

### `UnsupportedOperationException: cannot proxy inaccessible class ...`

The target class is not accessible from the chosen class loader arrangement.

Typical fix in this repo:

- resolve the target class from the system-server class loader,
- use `.withSharedClassLoader()`,
- keep the proxy creation in the same framework-loading context as the original object.

### Hidden API or access errors at runtime

Typical fix in this repo:

- keep `.markTrusted()` on framework proxies,
- use the correct framework class loader,
- avoid mixing app-side types with system-server-side types.

### Target method not intercepted

Check these first:

1. The method may be `final`, `private`, or `static`.
2. The method may be package-private but not inheritable with the chosen class loader strategy.
3. The call may happen during constructor execution before the invocation handler is installed.
4. The actual runtime signature may differ from the guessed one.

## Constructor Leak Caveat

If the target class calls overridable methods from its constructor, the handler may not intercept those constructor-time calls.

Reason:

- the generated instance exists before the handler is stored into the proxy field.

Do not rely on constructor-time interception.

## Embedded Working Examples

These examples are taken from the real project code and show the patterns that actually work.

### Example 1: Wrap Existing Service Object

This example proxies `PackageManagerInternal` and forwards most calls to the existing original object.

```java
private Object newProxy0(final Object original, File dexCacheDir) throws IOException {
    if (original == null) return null;

    return ProxyBuilder.forClass(
                    PackageManagerInternalHelper.INSTANCE
                            .packageManagerInternalClass(systemServerClassLoader))
            .dexCache(dexCacheDir)
            .withSharedClassLoader()
            .markTrusted()
            .handler(new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args)
                        throws Throwable {
                    method.setAccessible(true);
                    if ("resolveService".equals(method.getName())) {
                        try {
                            return handleCheckService(original, method, args);
                        } catch (Throwable e) {
                            XLog.e("handleCheckService error", e);
                        }
                    }
                    return method.invoke(original, args);
                }
            })
            .build();
}
```

Why this example matters:

- it uses the real framework class loader
- it uses shared class loader mode
- it uses trusted mode
- it intercepts one method and delegates the rest to `original`

### Example 2: Constructor Arguments Required

This example proxies `IntentFirewall`, which cannot be constructed with a no-arg constructor.

```java
return ProxyBuilder.forClass(IFWHelper.INSTANCE.ifwClass(systemServerClassLoader))
        .dexCache(dexCacheDir)
        .constructorArgTypes(
                IFWHelper.INSTANCE.amsInterfaceClass(systemServerClassLoader),
                Handler.class)
        .constructorArgValues(amsInterface, handler)
        .withSharedClassLoader()
        .markTrusted()
        .handler(new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                method.setAccessible(true);
                if ("checkBroadcast".equals(method.getName())) {
                    Boolean hookRes = handleCheckBroadcast(args);
                    if (hookRes != null && !hookRes) {
                        return false;
                    }
                }

                if ("checkStartActivity".equals(method.getName())) {
                    Boolean hookRes = handleCheckStartActivity(args);
                    if (hookRes != null && !hookRes) {
                        return false;
                    }
                }

                return method.invoke(local, args);
            }
        })
        .build();
```

Why this example matters:

- it shows correct constructor argument wiring
- it shows selective short-circuiting
- it still preserves default behavior through `method.invoke(local, args)`

### Example 3: Minimal Intercept-And-Delegate Pattern

This example proxies `UsageStatsManagerInternal` and intercepts one signature while delegating everything else.

```java
protected Object onCreateProxy(Object original, File dexCacheDir) throws Exception {
    return ProxyBuilder.forClass(UsageStatsManagerInternalHelper.INSTANCE.usmInternalClass(classLoader))
            .dexCache(dexCacheDir)
            .withSharedClassLoader()
            .markTrusted()
            .handler(new UsageStatsManagerInvocationHandler(original))
            .build();
}

@Override
public Object invoke(Object o, Method method, Object[] args) throws Throwable {
    if (XposedHelpersExt.matchMethodNameAndArgs(method, "reportEvent",
            ComponentName.class,
            int.class,
            int.class,
            int.class,
            ComponentName.class)) {
        handleReportEvent(args);
    }
    return tryInvoke(original, method, args);
}
```

Why this example matters:

- it shows signature-based interception instead of name-only interception
- it keeps the handler small
- it delegates all untouched behavior through a single path

## AI Tool Implementation Rules

If an AI tool is asked to implement a new class proxy with `ProxyBuilder` in this repository, it should obey these rules.

1. Never proxy a class until you verify it is not `final`.
2. Always identify the real runtime class loader of the target type.
3. For Android framework and system-server types in this repo, default to `.withSharedClassLoader().markTrusted()`.
4. If a live original object already exists, default to delegating with `method.invoke(original, args)`.
5. If no original object exists, use `ProxyBuilder.callSuper(proxy, method, args)` for pass-through behavior.
6. If the target constructor needs arguments, provide matching arg types and values explicitly.
7. Use a secure writable dex cache directory.
8. Replace only the necessary field or injection point; do not change unrelated initialization paths.
9. Intercept the minimum necessary method set.
10. Preserve existing behavior for all untouched methods.

## Recommended Decision Table

Use this decision table before writing code.

| Question | If yes | If no |
| --- | --- | --- |
| Is there already a live original instance? | Wrap and delegate to `original` | Use `callSuper` or reconsider design |
| Is the target a framework/system-server class? | Use shared class loader and trusted mode | Consider explicit parent loader only if needed |
| Does the target need constructor arguments? | Supply matching arg types and values | Use default no-arg construction |
| Are the methods to intercept overridable? | Proceed | `ProxyBuilder` is not suitable |
| Are hidden APIs involved? | Keep trusted mode and correct loader | Normal reflective path may be enough |

## Minimal Safe Checklist

Before considering the implementation complete, verify:

1. Proxy creation returns non-null.
2. The replaced field now holds the proxy instance.
3. The intended target method reaches the invocation handler.
4. Untouched methods still behave exactly as before.
5. No constructor mismatch or class-loader access error occurs.
