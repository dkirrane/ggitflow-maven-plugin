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

echo -e "\n\n"
git log --branches --remotes --tags --graph --oneline --decorate
echo -e "\n\n"
echo -e "Release branch created with some dummy commits"
read -p "Press [Enter] key to finish the release..."

runCmd mvn ggitflow:release-finish

echo -e "\n\n"
git log --branches --remotes --tags --graph --oneline --decorate

echo -e "\n\n"
read -p "Release finished. Please verify and then manually push the merge to remote develop and master..."
