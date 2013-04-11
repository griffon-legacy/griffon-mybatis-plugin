/*
 * Copyright 2011-2013 the original author or authors.
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

package griffon.plugins.mybatis

import javax.sql.DataSource
import org.apache.ibatis.mapping.Environment
import org.apache.ibatis.session.Configuration
import org.apache.ibatis.session.SqlSessionFactory
import org.apache.ibatis.session.SqlSessionFactoryBuilder
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory

import griffon.core.GriffonApplication
import griffon.util.ConfigUtils
import griffon.plugins.datasource.DataSourceHolder
import griffon.plugins.datasource.DataSourceConnector

/**
 * @author Andres Almiray
 */
@Singleton
final class MybatisConnector {
    private static final String DEFAULT = 'default'
    private final Set<Class> mappers = [] as LinkedHashSet
    private bootstrap

    ConfigObject createConfig(GriffonApplication app) {
        if (!app.config.pluginConfig.mybatis) {
            app.config.pluginConfig.mybatis = ConfigUtils.loadConfigWithI18n('MybatisConfig')
        }
        app.config.pluginConfig.mybatis
    }

    private ConfigObject narrowConfig(ConfigObject config, String dataSourceName) {
        if (config.containsKey('sessionFactory') && dataSourceName == DEFAULT) {
            return config.sessionFactory
        } else if (config.containsKey('sessionFactories')) {
            return config.sessionFactories[dataSourceName]
        }
        return config
    }

    SqlSessionFactory connect(GriffonApplication app, ConfigObject config, String dataSourceName = DEFAULT) {
        if (MybatisHolder.instance.isSqlSessionFactoryAvailable(dataSourceName)) {
            return MybatisHolder.instance.getSqlSessionFactory(dataSourceName)
        }

        ConfigObject dsconfig = DataSourceConnector.instance.createConfig(app)
        DataSourceConnector.instance.connect(app, dsconfig, dataSourceName)

        config = narrowConfig(config, dataSourceName)
        app.event('MybatisConnectStart', [config, dataSourceName])
        SqlSessionFactory sessionFactory = createSqlSessionFactory(config, dataSourceName)
        MybatisHolder.instance.setSqlSessionFactory(dataSourceName, sessionFactory)
        bootstrap = app.class.classLoader.loadClass('BootstrapMybatis').newInstance()
        bootstrap.metaClass.app = app
        resolveMybatisProvider(app).withSqlSession(dataSourceName) { dsName, sqlSession -> bootstrap.init(dsName, sqlSession) }
        app.event('MybatisConnectEnd', [dataSourceName, sessionFactory])
        sessionFactory
    }

    void disconnect(GriffonApplication app, ConfigObject config, String dataSourceName = DEFAULT) {
        if (!MybatisHolder.instance.isSqlSessionFactoryAvailable(dataSourceName)) return

        SqlSessionFactory sessionFactory = MybatisHolder.instance.getSqlSessionFactory(dataSourceName)
        app.event('MybatisDisconnectStart', [dataSourceName, sessionFactory])
        resolveMybatisProvider(app).withSqlSession(dataSourceName) { dsName, sqlSession -> bootstrap.destroy(dsName, sqlSession) }
        MybatisHolder.instance.disconnectSqlSessionFactory(dataSourceName)
        app.event('MybatisDisconnectEnd', [dataSourceName])
        ConfigObject dsconfig = DataSourceConnector.instance.createConfig(app)
        DataSourceConnector.instance.disconnect(app, dsconfig, dataSourceName)
    }

    MybatisProvider resolveMybatisProvider(GriffonApplication app) {
        def mybatisProvider = app.config.mybatisProvider
        if (mybatisProvider instanceof Class) {
            mybatisProvider = mybatisProvider.newInstance()
            app.config.mybatisProvider = mybatisProvider
        } else if (!mybatisProvider) {
            mybatisProvider = DefaultMybatisProvider.instance
            app.config.mybatisProvider = mybatisProvider
        }
        mybatisProvider
    }

    private SqlSessionFactory createSqlSessionFactory(ConfigObject config, String dataSourceName) {
        DataSource dataSource = DataSourceHolder.instance.getDataSource(dataSourceName)
        Environment environment = new Environment(dataSourceName, new JdbcTransactionFactory(), dataSource)
        Configuration configuration = new Configuration(environment)
        config.each { propName, propValue ->
            configuration[propName] = propValue
        }
        if (mappers.isEmpty()) {
            readMappers()
        }
        mappers.each { Class mapper -> configuration.addMapper(mapper) }

        new SqlSessionFactoryBuilder().build(configuration)
    }

    private void readMappers() {
        ClassLoader cl = Thread.currentThread().contextClassLoader
        Enumeration urls = cl.getResources('META-INF/mybatis/mappers.txt')
        urls.each { url ->
            url.eachLine { text ->
                text.trim().split(/,/).each { className -> 
                    mappers << cl.loadClass(className)
                }
            }
        }
    }
}