Plug4Green
==========

Plug4Green is a software focusing on saving energy in Data Centres optimizing the ICT resources and is meant to be used by Data Centres operators.


Install
------------

The following components/tools needs to be install before starting to develop on the F4G projects:
* The Java environment, jdk 1.7
* The Tomcat server 
* apache-maven

In a console, type:

    mvn install

To skip the tests, you can type:

    mvn install -DskipTests


Deploy
------


Tomcat:
The jar resulting from the build of F4G project will be deployed on the Tomcat server and will be accessed and 
shared by one or more web applications. This requires that it is deployed in a specific folder as described 
in the following.
1.	If they do not exist, create the folders:
*	[TOMCAT_HOME]/common/lib
*	[TOMCAT_HOME]/common/classes (currently not used, might be needed in the future)

2.	Configure the “catalina.properties” file as follows:
*	Open the file [TOMCAT_HOME]/conf/catalina.properties
*	Append  the following string to the  “common.loader” property:
“,${catalina.home}/common/classes,${catalina.home}/common/lib/*.jar”


The “InitServlet”  contains a target to package the web application into a war file.
When the “Ant” task is executed on a “build.xml” file, it retrieves some global configuration data from the 
“config/commons/build.properties” file.
The only element in the “build.properties” file that is dependent on the local configuration, is the entry: 
"tomcat.home=[to be set to your tomcat home]"
This entry is used by the ant target "deploy" to put the generated jars in the "${TOMCAT_HOME}/common/lib" of the Tomcat server.
Rename "local.build.properties.template" to "local.build.properties" and configure this property in the "local.build.properties" 
file in the “Commons” project.

Usage:
If Configured FIT4Green to start automatically then simply start your Tomcat and FIT4Green plug-in will start automatically.
Open the web application F4gGui with: http://localhost:8080/F4gGui click the start button to start the plug-in.
Use this web application to manage the FIT4Green plug-in.


To do
-----


steps remaining:
-put the Web application sources in src/main/webapp

-configure tomcat (The PLUGIN: http://mojo.codehaus.org/tomcat-maven-plugin/usage.html)
	and F4Gui ,GWT (http://gwt-maven.googlecode.com/svn/docs/maven-googlewebtoolkit2-plugin/setup.html)

-deploy with tomcat (the deployement: http://mojo.codehaus.org/tomcat-maven-plugin/deployment.html)



Copyright 2012 FIT4Green project.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
