UCM Plugin for Maven
================

Build, deploy and manage Oracle UCM components directly from maven.

Commands (Goals)
--------

### build

`mvn ucm:build`

Builds a component zip file into the default output folder.
can be overridden with <componentLocation>,
or <componentFileName> to change the file name

### deploy

`mvn ucm:deploy -Dserver=dev`

Builds and deploys the component zip to the server id in your configuration.
If no server is specified, the first server defined is used.

**Note:** Deploy does not automatically restart wcc. That is left up the user
so that multiple deploys can be run-before a restart,
 or avoided if a restart isn't deemed necessary.


### WCC Server Lifecycle

The plugin can query and control the basic lifecycle of WCC  
(when configured as a managed WLS instance with NodeManager.)

**Goals:**  
  
  * `mvn ucm:status` - gives the status info of the server.  
  * `mvn ucm:stop`   - forces WCC to shutdown.    
  * `mvn ucm:start`  - Starts WCC.  
  * `mvn ucm:suspend`- Suspends a running instance.  
  * `mvn ucm:resume` - resumes a suspended WCC instance.  
  * `mvn ucm:restart`- stops and restarts a WCC instance.    

### classpath

`mvn ucm:classpath`

Updates your component .hda classpath to reflect all maven source directories.

### lib

`mvn ucm:lib`

Updates your component .hda lib to reflect all maven dependencies.
Defaults to using the `$COMPONENT_DIR/lib/` folder,
but the lib folder can be configured with `<componentLibFolder>` config.
If configured the config must match the .hda file `componentLib` setting.

The lib files are assumed to be in the ${project.build.Directory}/Lib folder.
However, this is configurable with the <componentLibFolder> element.

### update-version

`mvn ucm:update-version`

This will increment your maven version and set your components version to match 


Configuration
-------------

In your projects pom.xml:

The plugin defines its own Lifecycle/Packaging **wcc**

To take full advantage of the plugin add `<packaging>wcc</packaging>`
to your *pom.xml*.

Setting that will automagically bind the plugin goals to the lifecycle,
so for example `mvn package` will call
`ucm:classpath`, `ucm:lib`, and `ucm:build` in order as part
of the normal build process


```xml
<plugin>
  <groupId>org.ucmtwine</groupId>
  <artifactId>ucm-maven-plugin</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <extensions>true</extensions> <!-- Allows WCC packaging type -->
  <configuration>
    <servers>
      <server>
        <id>dev</id>
        <url>http://dev.host.name/cs/idcplg</url>
        <username>sysadmin</username>
        <password>idc</password>
        <adminServer>
          <hostname>localhost</hostname> 
          <serverName>adminserver</serverName> 
          <wlsServerName>ucm</wlsServerName>
          <username>${server.username}</username>
          <password>${server.password}</password> 
        </adminServer>
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
    <!-- for ucm:lib -->
    <libFolder>target/lib</libFolder>
    <componentLibFolder>lib</componentLibFolder>
    <includeScope>runtime</includeScope>
    <excludeScope>provided</excludeScope>
    <!-- for ucm:deploy or ucm:build -->
    <componentLocation></componentLocation>
    <componentFileName></componentFileName><!-- Overrides zip file name, defaults to <componentName>.zip -->
    <componentLocation></componentLocation><!-- Overrides <componentFileName> and default output directory, defaults to <project.build.Directory>/<componentFileName> -->
    <componentName></componentName><!-- Overrides componentName, autodetected by default -->
    <componentZip></componentZip><!-- Overrides zip, defaults to <componentLocation> -->
  </configuration>
```

You can execute the build command on each maven install (without wcc packaging):

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
2. Install RIDC into your local maven repo
3. Download/Generate wlfullclient.jar from WebLogic Server. 
   http://docs.oracle.com/cd/E23943_01/web.1111/e13717/jarbuilder.htm#SACLT239
4. Install wlfullclient.jar 
   **NOTE:** The dependency is required, when building this plugin.  
             The dependency is required/used for controlling the WCC server lifecycle.

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

### archetype-generate

`mvn ucm:archetype-generate`

Command to generate a simple / blank Maven-UCM-Plugin project



Additional Notes
----------------
More information about Dates along with Help and configuration information 
can be found via the `help:describe` mojo.

`mvn help:describe -Dplugin=org.ucmtwine:ucm-maven-plugin -Ddetail=true`
  
  
  
  
  
  
  
-------

License (MIT)
-------------

Copyright (c) 2012 Tim Stirrat

Permission is hereby granted, free of charge, to any person obtaining a copy 
of this software and associated documentation files (the "Software"), 
to deal in the Software without restriction, including without limitation 
the rights to use, copy, modify, merge, publish, distribute, sublicense, 
and/or sell copies of the Software, and to permit persons to whom the Software 
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR 
OTHER DEALINGS IN THE SOFTWARE.
