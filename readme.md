UCM Plugin for Maven
================

Build, deploy and manage Oracle UCM components directly from maven.

Commands (Goals)
--------

### build

`mvn ucm:build`

Builds a component zip file into the current folder.

### deploy

`mvn ucm:deploy -Dserver=dev`

Builds and deploys the component zip to the server id in your configuration. If no server is specified, the first server defined is used.

### classpath

`mvn ucm:classpath`

Updates your component .hda classpath to reflect all maven depencies. Defaults to using the `$COMPONENT_DIR/lib/` folder, but the lib folder can be configured with `<libFolder>` config.

Configuration
-------------

In your project's pom.xml:

```xml
<plugin>
  <groupId>org.ucmtwine</groupId>
  <artifactId>ucm-maven-plugin</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <configuration>
    <servers>
      <server>
        <id>dev</id>
        <url>http://dev.host.name/cs/idcplg</url>
        <username>sysadmin</username>
        <password>idc</password>
      </server>
      <server>
        <id>test</id>
        <url>http://test.host.name/cs/idcplg</url>
        <username>weblogic</username>
        <password>weblogic1</password>
      </server>
    </servers>
  </configuration>
</plugin>
```

Optional parameters (defaults shown)

```xml
<!-- plugin -->
  <configuration>
    <!-- for ucm:classpath -->
    <libFolder>lib</libFolder>
    <includeScope>runtime</includeScope>
    <excludeScope>provided</excludeScope>
    <!-- for ucm:deploy or ucm:build -->
    <componentName></componentName><!-- Overrides componentName, autodetected by default -->
    <componentZip></componentZip><!-- Overrides zip, defaults to <componentName>.zip -->
  </configuration>
```

You can execute the build command on each maven install:

```xml
<!-- plugin -->
  <executions>
  	<execution>
  		<id>build-zip</id>
  		<phase>install</phase>
  		<goals>
  			<goal>build</goal>
  		</goals>
  	</execution>
  </executions>
```

Installing the plugin
---------------------

Until this is up on Maven central, you need to install into your local repository using `mvn`.

*Requires Maven on your PATH*

1. Download [RIDC](http://www.oracle.com/technetwork/middleware/webcenter/content/downloads/index.html) 11.1.1
1. Install RIDC into your local maven repo

  ```
  mvn install:install-file -DgroupId=com.oracle.ucm -DartifactId=ridc -Dpackaging=jar -Dversion=11.1.1 -Dfile=oracle.ucm.ridc-11.1.1.jar -DgeneratePom=true
  ```

1. Download ucm-maven-plugin-1.0.0-SNAPSHOT.jar from the downloads section
1. Download the pom.xml from github source
1. Install with Maven

  ```
  mvn install:install-file -DgroupId=org.ucmtwine -DartifactId=ucm-maven-plugin -Dpackaging=jar -Dversion=1.0.0-SNAPSHOT -Dfile=ucm-maven-plugin-1.0.0-SNAPSHOT.jar -DpomFile=pom.xml
  ```


Planned Features for 1.0
------------------------

### update-version

`mvn ucm:update-version`

This will update your component's version to reflect your maven project version.


License (MIT)
-------------

Copyright (c) 2012 Tim Stirrat

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
