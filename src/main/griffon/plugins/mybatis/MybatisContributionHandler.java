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

/**
 * @author Andres Almiray
 */
public interface MybatisContributionHandler {
    void setMybatisProvider(MybatisProvider provider);

    MybatisProvider getMybatisProvider();

    <R> R withSqlSession(Closure<R> closure);

    <R> R withSqlSession(String sessionFactoryName, Closure<R> closure);

    <R> R withSqlSession(CallableWithArgs<R> callable);

    <R> R withSqlSession(String sessionFactoryName, CallableWithArgs<R> callable);
}