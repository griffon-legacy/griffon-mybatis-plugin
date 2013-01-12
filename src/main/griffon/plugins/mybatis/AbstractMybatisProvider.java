/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.plugins.mybatis;

import griffon.util.CallableWithArgs;
import groovy.lang.Closure;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static griffon.util.GriffonNameUtils.isBlank;

/**
 * @author Andres Almiray
 */
public abstract class AbstractMybatisProvider implements MybatisProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMybatisProvider.class);
    private static final String DEFAULT = "default";

    public <R> R withSqlSession(Closure<R> closure) {
        return withSqlSession(DEFAULT, closure);
    }

    public <R> R withSqlSession(String sessionFactoryName, Closure<R> closure) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = DEFAULT;
        if (closure != null) {
            SqlSessionFactory sf = getSessionFactory(sessionFactoryName);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing SQL stament on sqlSession '" + sessionFactoryName + "'");
            }
            SqlSession session = sf.openSession(true);
            try {
                return closure.call(sessionFactoryName, session);
            } finally {
                session.close();
            }
        }
        return null;
    }

    public <R> R withSqlSession(CallableWithArgs<R> callable) {
        return withSqlSession(DEFAULT, callable);
    }

    public <R> R withSqlSession(String sessionFactoryName, CallableWithArgs<R> callable) {
        if (isBlank(sessionFactoryName)) sessionFactoryName = DEFAULT;
        if (callable != null) {
            SqlSessionFactory sf = getSessionFactory(sessionFactoryName);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing SQL stament on sqlSession '" + sessionFactoryName + "'");
            }
            SqlSession session = sf.openSession(true);
            try {
                callable.setArgs(new Object[]{sessionFactoryName, session});
                return callable.call();
            } finally {
                session.close();
            }
        }
        return null;
    }

    protected abstract SqlSessionFactory getSessionFactory(String sessionFactoryName);
}