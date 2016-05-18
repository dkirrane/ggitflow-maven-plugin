#!/bin/sh

# Hotfix/Patch 3.0.0.0 - Hotfix/Patch version = 3.0.0.1-SNAPSHOT
mvn ggitflow:hotfix-start
git commit --allow-empty -m "JIRA-7 hotfix commit"
git commit --allow-empty -m "JIRA-8 hotfix commit"
mvn ggitflow:hotfix-finish