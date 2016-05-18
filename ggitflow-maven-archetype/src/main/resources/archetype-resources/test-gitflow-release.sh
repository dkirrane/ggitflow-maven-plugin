#!/bin/sh

# Release
mvn ggitflow:release-start
git commit --allow-empty -m "JIRA-5 release fix commit"
git commit --allow-empty -m "JIRA-6 release fix commit"
mvn ggitflow:release-finish

git log --branches --remotes --tags --graph --oneline --decorate
