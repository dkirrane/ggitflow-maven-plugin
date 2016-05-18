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

# Release
runCmd mvn ggitflow:release-start
runCmd git commit --allow-empty -m "JIRA-5 release fix commit"
runCmd git commit --allow-empty -m "JIRA-6 release fix commit"
runCmd mvn ggitflow:release-finish

git log --branches --remotes --tags --graph --oneline --decorate
