#!/bin/sh

# Feature
mvn ggitflow:feature-start -DfeatureName=JIRA-1-Feature1
git commit --allow-empty -m "JIRA-1 feature1 commit"
git commit --allow-empty -m "JIRA-2 feature1 commit"
mvn ggitflow:feature-finish -DfeatureName=feature/JIRA-1-Feature1

# Feature
mvn ggitflow:feature-start -DfeatureName=JIRA-2-Feature2
git commit --allow-empty -m "JIRA-3 feature2 commit"
git commit --allow-empty -m "JIRA-4 feature2 commit"
mvn ggitflow:feature-finish -DfeatureName=feature/JIRA-2-Feature2