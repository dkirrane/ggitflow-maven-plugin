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

# Hotfix/Patch 3.0.0.0 - Hotfix/Patch version = 3.0.0.1-SNAPSHOT
runCmd mvn ggitflow:hotfix-start
runCmd git commit --allow-empty -m "JIRA-7 hotfix commit"
runCmd git commit --allow-empty -m "JIRA-8 hotfix commit"
runCmd mvn ggitflow:hotfix-finish

git log --branches --remotes --tags --graph --oneline --decorate