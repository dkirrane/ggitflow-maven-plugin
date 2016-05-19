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

echo -e "\n\n"
git log --branches --remotes --tags --graph --oneline --decorate
echo -e "\n\n"
echo -e "Hotfix branch created with some dummy commits"
read -p "Press [Enter] key to finish the hotfix..."

runCmd mvn ggitflow:hotfix-finish

echo -e "\n\n"
git log --branches --remotes --tags --graph --oneline --decorate

echo -e "\n\n"
read -p "Hotfix finished. Please verify and then manually push the merge to remote develop and master..."