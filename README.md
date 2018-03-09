Deploy artifacts

1. Install warc-hadoop to local maven repo: gradle install
2. Install language-detector to local maven repo: mvn -DskipTests=true -Dmaven.javadoc.skip=true install
3. Deploy patched artifacts to S3:
 cd 
