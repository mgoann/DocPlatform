﻿mvn install:install-file -Dfile=jpf-1.5.1.jar -DgroupId=net.sf.jpf -DartifactId=jpf -Dversion=1.5.1 -Dpackaging=jar 
mvn install:install-file -Dfile=bcmail-jdk16-146.jar -DgroupId=bouncycastle -DartifactId=bcmail-jdk16 -Dversion=146 -Dpackaging=jar 
mvn install:install-file -Dfile=bcprov-jdk16-146.jar -DgroupId=bouncycastle -DartifactId=bcprov-jdk16 -Dversion=146 -Dpackaging=jar 
mvn install:install-file -Dfile=bctsp-jdk16-146.jar -DgroupId=bouncycastle -DartifactId=bctsp-jdk16 -Dversion=146 -Dpackaging=jar  