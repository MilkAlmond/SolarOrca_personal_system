package com.yujing.security;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data-layer guard for D13: every statement on an @OwnerScoped mapper must
 * contain an owner_id predicate. Because owner ids are only obtained from
 * CurrentUserProvider, client-supplied owner_id values are never authoritative.
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                        CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class OwnerIdGuardInterceptor implements Interceptor {

    private final Map<String, Boolean> ownerScopedCache = new ConcurrentHashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        if (isOwnerScoped(statement.getId())) {
            Object parameter = invocation.getArgs()[1];
            String sql = statement.getBoundSql(parameter).getSql().toLowerCase(Locale.ROOT);
            if (!sql.contains("owner_id")) {
                throw new IllegalStateException(
                        "Owner-scoped statement is missing an owner_id predicate: " + statement.getId());
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    private boolean isOwnerScoped(String statementId) {
        return ownerScopedCache.computeIfAbsent(statementId, id -> {
            int separator = id.lastIndexOf('.');
            if (separator < 0) {
                return false;
            }
            String className = id.substring(0, separator);
            String methodName = id.substring(separator + 1);
            try {
                Class<?> mapper = Class.forName(className);
                if (mapper.isAnnotationPresent(OwnerScoped.class)) {
                    return true;
                }
                Method[] methods = mapper.getMethods();
                return Arrays.stream(methods)
                        .anyMatch(method -> method.getName().equals(methodName)
                                && method.isAnnotationPresent(OwnerScoped.class));
            } catch (ClassNotFoundException ex) {
                return false;
            }
        });
    }
}
