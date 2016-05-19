# Setup
# Add the following to your Maven settings.xml

<pluginGroups>
   <pluginGroup>com.dkirrane.maven.plugins</pluginGroup>
</pluginGroups>

# Initialise Git repo
git init . && git add . && git commit --allow-empty -m "Initial commit"

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
