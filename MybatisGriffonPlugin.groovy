/*
 * Copyright 2011-2013 the original author or authors.
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
    String version = '1.0.0'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '1.2.0 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [datasource: '1.0.0']
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
Upon installation the plugin will generate the following artifacts in
`$appdir/griffon-app/conf`:

 * DataSource.groovy - contains the datasource and pool definitions. Its format
   is equal to GORM's requirements.
   Additional configuration for this artifact is explained in the [datasource][2] plugin.
 * MybatisConfig.groovy - contains SessionFactory definitions.
 * BootstrapMybatis.groovy - defines init/destroy hooks for data to be manipulated
   during app startup/shutdown.

A new dynamic method named `withSqlSession` will be injected into all controllers,
giving you access to a `org.apache.ibatis.session.SqlSession` object, with which
you'll be able to make calls to the database. Remember to make all database calls
off the UI thread otherwise your application may appear unresponsive when doing
long computations inside the UI thread.

This method is aware of multiple databases. If no databaseName is specified
when calling it then the default database will be selected. Here are two example
usages, the first queries against the default database while the second queries
a database whose name has been configured as 'internal'

    package sample
    class SampleController {
        def queryAllDatabases = {
            withSqlSession { sessionFactoryName, session -> ... }
            withSqlSession('internal') { sessionFactoryName, session -> ... }
        }
    }

The following list enumerates all the variants of the injected method

 * `<R> R withSqlSession(Closure<R> stmts)`
 * `<R> R withSqlSession(CallableWithArgs<R> stmts)`
 * `<R> R withSqlSession(String sessionFactoryName, Closure<R> stmts)`
 * `<R> R withSqlSession(String sessionFactoryName, CallableWithArgs<R> stmts)`

These methods are also accessible to any component through the singleton
`griffon.plugins.mybatis.MybatisEnhancer`. You can inject these methods to
non-artifacts via metaclasses. Simply grab hold of a particular metaclass and
call `MybatisEnhancer.enhance(metaClassInstance)`.

This plugin relies on the facilities exposed by the [datasource][2] plugin.

Configuration
-------------
### MybatisAware AST Transformation

The preferred way to mark a class for method injection is by annotating it with
`@griffon.plugins.mybatis.MybatisAware`. This transformation injects the
`griffon.plugins.mybatis.MybatisContributionHandler` interface and default
behavior that fulfills the contract.

### Dynamic Method Injection

Dynamic methods will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.mybatis.injectInto = ['controller', 'service']

Dynamic method injection will be skipped for classes implementing
`griffon.plugins.mybatis.MybatisContributionHandler`.

### Events

The following events will be triggered by this addon

 * MybatisConnectStart[config, databaseName] - triggered before connecting to the database
 * MybatisConnectEnd[databaseName, sessionFactory] - triggered after connecting to the database
 * MybatisDisconnectStart[databaseName, sessionFactory] - triggered before disconnecting from the database
 * MybatisDisconnectEnd[databaseName] - triggered after disconnecting from the database

### Multiple Databases

The config file `MybatisConfig.groovy` defines a default sessionFactory block.
As the name implies this is the sessionFactory used by default, however you can
configure named sessionFactories by adding a new config block. For example
connecting to a database whose name is 'internal' can be done in this way

    sessionFactories {
        internal {
            someConfigurationProperty = someValue
        }
    }

This block can be used inside the `environments()` block in the same way as the
default sessionFactory block is used.

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/mybatis][3]

Scripts
-------

 * **create-mybatis-class** - creates a new class along with a Mapper interface
and its companion XML Mapper.

Testing
-------

Dynamic methods will not be automatically injected during unit testing, because
addons are simply not initialized for this kind of tests. However you can use
`MybatisEnhancer.enhance(metaClassInstance, mybatisProviderInstance)` where
`mybatisProviderInstance` is of type `griffon.plugins.mybatis.MybatisProvider`.
The contract for this interface looks like this

    public interface MybatisProvider {
        <R> R withSqlSession(Closure<R> closure);
        <R> R withSqlSession(CallableWithArgs<R> callable);
        <R> R withSqlSession(String sessionFactoryName, Closure<R> closure);
        <R> R withSqlSession(String sessionFactoryName, CallableWithArgs<R> callable);
    }

It's up to you define how these methods need to be implemented for your tests.
For example, here's an implementation that never fails regardless of the
arguments it receives

    class MyMybatisProvider implements MybatisProvider {
        public <R> R withSqlSession(Closure<R> closure) { null }
        public <R> R withSqlSession(CallableWithArgs<R> callable) { null }
        public <R> R withSqlSession(String sessionFactoryName, Closure<R> closure) { null }
        public <R> R withSqlSession(String sessionFactoryName, CallableWithArgs<R> callable) { null }
    }

This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            MybatisEnhancer.enhance(service.metaClass, new MyMybatisProvider())
            // exercise service methods
        }
    }

On the other hand, if the service is annotated with `@MybatisAware` then usage
of `MybatisEnhancer` should be avoided at all costs. Simply set
`mybatisProviderInstance` on the service instance directly, like so, first the
service definition

    @griffon.plugins.mybatis.MybatisAware
    class MyService {
        def serviceMethod() { ... }
    }

Next is the test

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            service.mybatisProvider = new MyMybatisProvider()
            // exercise service methods
        }
    }

Tool Support
------------

### DSL Descriptors

This plugin provides DSL descriptors for Intellij IDEA and Eclipse (provided
you have the Groovy Eclipse plugin installed). These descriptors are found
inside the `griffon-mybatis-compile-x.y.z.jar`, with locations

 * dsdl/mybatis.dsld
 * gdsl/mybatis.gdsl

### Lombok Support

Rewriting Java AST in a similar fashion to Groovy AST transformations is
possible thanks to the [lombok][4] plugin.

#### JavaC

Support for this compiler is provided out-of-the-box by the command line tools.
There's no additional configuration required.

#### Eclipse

Follow the steps found in the [Lombok][4] plugin for setting up Eclipse up to
number 5.

 6. Go to the path where the `lombok.jar` was copied. This path is either found
    inside the Eclipse installation directory or in your local settings. Copy
    the following file from the project's working directory

         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/mybatis-<version>/dist/griffon-mybatis-compile-<version>.jar .

 6. Edit the launch script for Eclipse and tweak the boothclasspath entry so
    that includes the file you just copied

        -Xbootclasspath/a:lombok.jar:lombok-pg-<version>.jar:\
        griffon-lombok-compile-<version>.jar:griffon-mybatis-compile-<version>.jar

 7. Launch Eclipse once more. Eclipse should be able to provide content assist
    for Java classes annotated with `@MybatisAware`.

#### NetBeans

Follow the instructions found in [Annotation Processors Support in the NetBeans
IDE, Part I: Using Project Lombok][5]. You may need to specify
`lombok.core.AnnotationProcessor` in the list of Annotation Processors.

NetBeans should be able to provide code suggestions on Java classes annotated
with `@MybatisAware`.

#### Intellij IDEA

Follow the steps found in the [Lombok][4] plugin for setting up Intellij IDEA
up to number 5.

 6. Copy `griffon-mybatis-compile-<version>.jar` to the `lib` directory

         $ pwd
           $USER_HOME/Library/Application Support/IntelliJIdea11/lombok-plugin
         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/mybatis-<version>/dist/griffon-mybatis-compile-<version>.jar lib

 7. Launch IntelliJ IDEA once more. Code completion should work now for Java
    classes annotated with `@MybatisAware`.


[1]: http://www.mybatis.org/java.html
[2]: /plugin/datasource
[3]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/mybatis
[4]: /plugin/lombok
[5]: http://netbeans.org/kb/docs/java/annotations-lombok.html
'''
}