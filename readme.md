ucm-maven-plugin
----------------

Build and deploy Oracle UCM components directly from maven.

Installing
==========

Until this is up on Maven central, you need to manually install into your repository:

```
mvn install:install-file -DgroupId=org.ucmtwine -DartifactId=ucm-maven-plugin -Dpackaging=jar -Dversion=1.0.0-SNAPSHOT -Dfile=ucm-maven-plugin-1.0.0-SNAPSHOT.jar -DpomFile=pom.xml
```

You also need RIDC 11.1.1 in your repository which you can install like so:

mvn install:install-file -DgroupId=oracle-ucm -DartifactId=ridc -Dpackaging=jar -Dversion=11.1.1 -Dfile=oracle.ucm.ridc-11.1.1.jar -DpomFile=ridc.pom.xml

Usage
=====

Include the plugin in your project's pom.xml:

```xml
<build>
  <!-- ... -->
  <plugins>
    <!-- ... -->
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

### build a component

`mvn ucm:build`

Builds a component zip file into the current folder.

### deploy a component

`mvn ucm:deploy -Dserver=dev`

Builds and deploys the component zip to the server id in your configuration. If no server is specified, the first server defined is used.

License (MIT)
-------------

Copyright (c) 2012 Tim Stirrat

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
