################################################################################
#
# Maven Archetype that will get you started with the ggitflow-maven-plugin
#
################################################################################

# Manually download the plugin
mvn org.apache.maven.plugins:maven-dependency-plugin:get \
    -DrepoUrl=https://oss.sonatype.org/content/groups/public \
    -Dartifact=com.dkirrane.maven.plugins:ggitflow-maven-plugin:1.6-SNAPSHOT

For example, run the following command to create a sample multi-module project
with ggitflow-maven-plugin configured:

##############################
# Using Released version
##############################

# Interactive mode
mvn archetype:generate \
    -DinteractiveMode=true \
    -DarchetypeGroupId=com.dkirrane.maven.archetype \
    -DarchetypeArtifactId=ggitflow-maven-archetype \
    -DarchetypeVersion=1.6-SNAPSHOT \
    -DarchetypeRepository=https://oss.sonatype.org/content/repositories/snapshots


# Batch mode
mvn archetype:generate \
    -DinteractiveMode=false \
    -DarchetypeGroupId=com.dkirrane.maven.archetype \
    -DarchetypeArtifactId=ggitflow-maven-archetype \
    -DarchetypeVersion=1.6-SNAPSHOT \
    -DarchetypeRepository=https://oss.sonatype.org/content/repositories/snapshots \
    -DgroupId=com.mycompany -DartifactId=my-proj -Dversion=1.0-SNAPSHOT


########################################
# Using Staged version for verification
########################################
#   1. Deploy to stage https://oss.sonatype.org/index.html#stagingRepositories
#   2. Close stage to make it available in the Staging repo
#   3. Run below command & test
#   4. If happy Release otherwise Drop and redeploy to test fixes

rm -Rf C:\.m2\com\dkirrane
mvn -U archetype:generate \
    -DinteractiveMode=false \
    -DarchetypeGroupId=com.dkirrane.maven.archetype \
    -DarchetypeArtifactId=ggitflow-maven-archetype \
    -DarchetypeVersion=1.6 \
    -DarchetypeRepository=https://oss.sonatype.org/content/groups/staging \
    -DgroupId=com.mycompany -DartifactId=my-proj -Dversion=1.0-SNAPSHOT