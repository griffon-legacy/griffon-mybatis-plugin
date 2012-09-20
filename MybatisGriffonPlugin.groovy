/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by getApplication()licable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */
 
/**
 * @author Andres Almiray
 */
class MybatisGriffonPlugin {
    // the plugin version
    String version = '0.6'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '1.1.0 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [datasource: '0.4']
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, gtk
    List toolkits = []
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = ''
    // URL where source can be found
    String source = 'https://github.com/griffon/griffon-mybatis-plugin'

    List authors = [
        [
            name: 'Andres Almiray',
            email: 'aalmiray@yahoo.com'
        ]
    ]
    String title = 'Mybatis support'
    String description = '''
The Mybatis plugin enables lightweight access to datasources using [Mybatis][1].
This plugin does NOT provide domain classes nor dynamic finders like GORM does.

Usage
-----
Upon installation the plugin will generate the following artifacts in `$appdir/griffon-app/conf`:

 * DataSource.groovy - contains the datasource and pool definitions. Its format is equal to GORM's requirements.
 Additional configuration for this artifact is explained in the [DataSource Plugin][2].
 * MybatisConfig.groovy - contains SessionFactory definitions.
 * BootstrapMybatis.groovy - defines init/destroy hooks for data to be manipulated during app startup/shutdown.

A new dynamic method named `withSqlSession` will be injected into all controllers,
giving you access to a `org.apache.ibatis.session.SqlSession` object, with which you'll be able
to make calls to the database. Remember to make all database calls off the EDT
otherwise your application may appear unresponsive when doing long computations
inside the EDT.

This method is aware of multiple databases. If no databaseName is specified when calling
it then the default database will be selected. Here are two example usages, the first
queries against the default database while the second queries a database whose name has
been configured as 'internal'

    package sample
    class SampleController {
        def queryAllDatabases = {
            withSqlSession { sessionFactoryName, sqlSession -> ... }
            withSqlSession('internal') { sessionFactoryName, sqlSession -> ... }
        }
    }
    
This method is also accessible to any component through the singleton `griffon.plugins.mybatis.MybatisConnector`.
You can inject these methods to non-artifacts via metaclasses. Simply grab hold of a particular metaclass and call
`MybatisEnhancer.enhance(metaClassInstance, mybatisProviderInstance)`.

Configuration
-------------
### Dynamic method injection

The `withSqlSession()` dynamic method will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.mybatis.injectInto = ['controller', 'service']

### Events

The following events will be triggered by this addon

 * MybatisConnectStart[config, databaseName] - triggered before connecting to the database
 * MybatisConnectEnd[databaseName, sessionFactory] - triggered after connecting to the database
 * MybatisDisconnectStart[databaseName, sessionFactory] - triggered before disconnecting from the database
 * MybatisDisconnectEnd[databaseName] - triggered after disconnecting from the database

### Multiple Databases

The config file `MybatisConfig.groovy` defines a default sessionFactory block. As the name
implies this is the sessionFactory used by default, however you can configure named sessionFactories
by adding a new config block. For example connecting to a database whose name is 'internal'
can be done in this way

    sessionFactories {
        internal {
            someConfigurationProperty = someValue
        }
    }

This block can be used inside the `environments()` block in the same way as the
default database block is used.

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/mybatis][3]

Scripts
-------

 * **create-mybatis-class** - creates a new class along with a Mapper interface and its companion XML Mapper.

Testing
-------
The `withSqlSession()` dynamic method will not be automatically injected during unit testing, because addons are simply not initialized
for this kind of tests. However you can use `MybatisEnhancer.enhance(metaClassInstance, mybatisProviderInstance)` where 
`mybatisProviderInstance` is of type `griffon.plugins.mybatis.SqlSessionProvider`. The contract for this interface looks like this

    public interface SqlSessionProvider {
        Object withSqlSession(Closure closure);
        Object withSqlSession(String sessionFactoryName, Closure closure);
        <T> T withSqlSession(CallableWithArgs<T> callable);
        <T> T withSqlSession(String sessionFactoryName, CallableWithArgs<T> callable);
    }

It's up to you define how these methods need to be implemented for your tests. For example, here's an implementation that never
fails regardless of the arguments it receives

    class MySqlSessionProvider implements SqlSessionProvider {
        Object withSqlSession(String sessionFactoryName = 'default', Closure closure) { null }
        public <T> T withSqlSession(String sessionFactoryName = 'default', CallableWithArgs<T> callable) { null }
    }

This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            MybatisEnhancer.enhance(service.metaClass, new MySqlSessionProvider())
            // exercise service methods
        }
    }


[1]: http://www.mybatis.org/java.html
[2]: /plugin/datasource
[3]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/mybatis
'''
}
