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

# Feature Start
runCmd mvn ggitflow:feature-start -DfeatureName=JIRA-1-Feature

runCmd git commit --allow-empty -m "JIRA-1 feature commit"
runCmd git commit --allow-empty -m "JIRA-2 feature commit"

echo -e "\n\n"
echo -e "Feature branch 1 created with some dummy commits"
