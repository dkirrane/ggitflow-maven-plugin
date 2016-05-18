#!/bin/bash

function runCmd {
    echo "\$ $@" ; "$@" ;
    local status=$?
    if [ $status -ne 0 ]; then
        echo "Failed to run with $1" >&2
        exit
    fi
    return $status
}

# Feature 1
runCmd mvn ggitflow:feature-start -DfeatureName=JIRA-1-Feature1
runCmd git commit --allow-empty -m "JIRA-1 feature1 commit"
runCmd git commit --allow-empty -m "JIRA-2 feature1 commit"
runCmd mvn ggitflow:feature-finish -DfeatureName=feature/JIRA-1-Feature1

git log --branches --remotes --tags --graph --oneline --decorate

# need to push merged 'Feature 1' in order to start 'Feature 2'
runCmd git push origin develop

# Feature 2
runCmd mvn ggitflow:feature-start -DfeatureName=JIRA-2-Feature2
runCmd git commit --allow-empty -m "JIRA-3 feature2 commit"
runCmd git commit --allow-empty -m "JIRA-4 feature2 commit"
runCmd mvn ggitflow:feature-finish -DfeatureName=feature/JIRA-2-Feature2

git log --branches --remotes --tags --graph --oneline --decorate