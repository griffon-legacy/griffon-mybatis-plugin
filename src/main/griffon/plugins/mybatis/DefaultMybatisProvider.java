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

import org.apache.ibatis.session.SqlSessionFactory;

/**
 * @author Andres Almiray
 */
public class DefaultMybatisProvider extends AbstractMybatisProvider {
    private static final DefaultMybatisProvider INSTANCE;

    static {
        INSTANCE = new DefaultMybatisProvider();
    }

    public static DefaultMybatisProvider getInstance() {
        return INSTANCE;
    }

    @Override
    protected SqlSessionFactory getSessionFactory(String sessionFactoryName) {
        return MybatisHolder.getInstance().fetchSqlSessionFactory(sessionFactoryName);
    }
}