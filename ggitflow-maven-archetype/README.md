Maven Archetype that will get you started with the ggitflow-maven-plugin

For example, run the following command to create a sample multi-module project
with ggitflow-maven-plugin configured:

mvn archetype:generate \
    -DinteractiveMode=true \
    -DarchetypeGroupId=com.dkirrane.maven.archetype \
    -DarchetypeArtifactId=ggitflow-maven-archetype \
    -DarchetypeVersion=1.0-SNAPSHOT \
    -DarchetypeRepository=https://oss.sonatype.org/content/repositories/snapshots