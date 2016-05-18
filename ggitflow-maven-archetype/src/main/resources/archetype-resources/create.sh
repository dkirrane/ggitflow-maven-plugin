#!/bin/sh
##
# Removes all Git history 
# Reconstructs the repo with only the current content 
# Pushes to origin
##
# Major.Minor.ServicePack.Patch.SprintNumber-SNAPSHOT
# 3.0.0.0-SNAPSHOT

# Feature
mvn ggitflow:feature-start -DfeatureName=JIRA-1-Feature
git commit --allow-empty -m "JIRA-xxx feature commit"
git commit --allow-empty -m "JIRA-xxx feature commit"
mvn ggitflow:feature-finish -DfeatureName=feature/JIRA-1-Feature

# Feature
mvn ggitflow:feature-start -DfeatureName=JIRA-2-Feature
git commit --allow-empty -m "JIRA-xxx feature commit"
git commit --allow-empty -m "JIRA-xxx feature commit"
mvn ggitflow:feature-finish -DfeatureName=feature/JIRA-2-Feature

# Release
mvn ggitflow:release-start
git commit --allow-empty -m "JIRA-xxx release fix commit"
git commit --allow-empty -m "JIRA-xxx release fix commit"
mvn ggitflow:release-finish

# Hotfix/Patch 3.0.0.0 - Hotfix/Patch version = 3.0.0.1-SNAPSHOT
mvn ggitflow:hotfix-start
git commit --allow-empty -m "JIRA-xxx hotfix commit"
git commit --allow-empty -m "JIRA-xxx hotfix commit"
mvn ggitflow:hotfix-finish

# Support/ServicePack 3.0.0.0 - Support/ServicePack version = 3.0.1.0-SNAPSHOT
mvn ggitflow:support-start