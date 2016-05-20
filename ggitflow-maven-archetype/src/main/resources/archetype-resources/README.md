# Setup

mvn org.apache.maven.plugins:maven-dependency-plugin:get \
    -DrepoUrl=https://oss.sonatype.org/content/groups/public \
    -Dartifact=com.dkirrane.maven.plugins:ggitflow-maven-plugin:1.7

# Add the following to your Maven settings.xml

        <pluginGroups>
           <pluginGroup>com.dkirrane.maven.plugins</pluginGroup>
        </pluginGroups>

    otherwise use full path

        mvn com.dkirrane.maven.plugins:ggitflow-maven-plugin:1.7:<goal>

# Initialise clean Git repo
./clear-git-history.sh

# Feature
mvn ggitflow:feature-start
mvn ggitflow:feature-finish

# Release
mvn ggitflow:release-start
mvn ggitflow:release-finish

# Hotfix
mvn ggitflow:hotfix-start
mvn ggitflow:hotfix-finish

# Support
mvn ggitflow:support-start
