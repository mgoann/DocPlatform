                    DocPlatform Community Edition


You need JDK 1.7, Maven 3.0.3, Ant 1.7 to build this sources

0) Open a command shell to the folder where you unzipped the archive

1) go into folder: build/poms
   luanch the command: deployfile.bat

1) go into folder: build/poms
   launch the command: mvn install

2) go into folder: community/logicaldoc/
   launch the command: mvn -Dmaven.test.skip=true install
   
   you should find the .war archive containing the web application
