package com.yujing.security;

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnerIdGuardInterceptorTest {

    private final OwnerIdGuardInterceptor interceptor = new OwnerIdGuardInterceptor();

    @OwnerScoped
    interface ScopedMapper {
        Object find(long ownerId);
    }

    interface MethodScopedMapper {
        @OwnerScoped
        Object load(long ownerId);
    }

    interface UnscopedMapper {
        Object find(long id);
    }

    @Test
    void rejectsOwnerScopedStatementWithoutOwnerIdPredicate() throws Throwable {
        RecordingExecutor executor = new RecordingExecutor();
        assertThatThrownBy(() -> interceptor.intercept(executor.invocation(
                ScopedMapper.class.getName() + ".find", "select * from notes where id = ?")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing an owner_id predicate");
        assertThat(executor.calls).hasValue(0);
    }

    @Test
    void allowsOwnerScopedStatementWithOwnerIdPredicate() throws Throwable {
        RecordingExecutor executor = new RecordingExecutor();
        interceptor.intercept(executor.invocation(
                ScopedMapper.class.getName() + ".find", "select * from notes where owner_id = ? and id = ?"));
        assertThat(executor.calls).hasValue(1);
    }

    @Test
    void ignoresMappersThatAreNotOwnerScoped() throws Throwable {
        RecordingExecutor executor = new RecordingExecutor();
        interceptor.intercept(executor.invocation(
                UnscopedMapper.class.getName() + ".find", "select * from app_versions where platform = ?"));
        assertThat(executor.calls).hasValue(1);
    }

    @Test
    void honorsMethodLevelOwnerScopedAnnotation() throws Throwable {
        RecordingExecutor executor = new RecordingExecutor();
        assertThatThrownBy(() -> interceptor.intercept(executor.invocation(
                MethodScopedMapper.class.getName() + ".load", "select * from notes where id = ?")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(executor.calls).hasValue(0);
    }

    private static final class RecordingExecutor implements InvocationHandler {
        private final AtomicInteger calls = new AtomicInteger();
        private final Executor executor = (Executor) Proxy.newProxyInstance(
                RecordingExecutor.class.getClassLoader(), new Class<?>[]{Executor.class}, this);

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            calls.incrementAndGet();
            return null;
        }

        Invocation invocation(String statementId, String sql) throws Exception {
            Configuration configuration = new Configuration();
            MappedStatement statement = new MappedStatement.Builder(
                    configuration,
                    statementId,
                    new StaticSqlSource(configuration, sql, List.of()),
                    SqlCommandType.SELECT)
                    .build();
            Method method = Executor.class.getMethod("query",
                    MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class);
            return new Invocation(executor, method, new Object[]{statement, null, RowBounds.DEFAULT, null});
        }
    }
}
