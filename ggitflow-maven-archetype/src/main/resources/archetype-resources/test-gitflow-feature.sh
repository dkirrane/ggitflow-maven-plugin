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

echo -e "\n\n"
echo -e "Feature branch 1 created with some dummy commits"
read -p "Press [Enter] key to finish the feature..."

runCmd mvn ggitflow:feature-finish -DfeatureName=feature/JIRA-1-Feature1

read -p "Feature 1 finished. Please verify and then manually push the merge to remote develop..."

read -r -p "Do you want to create another Feature branch? [Y/n]" choice
if [[ $choice =~ ^([nN][oO]|[nN])$ ]]; then
    exit 1
fi

# Feature 2
runCmd mvn ggitflow:feature-start -DfeatureName=JIRA-2-Feature2

runCmd git commit --allow-empty -m "JIRA-3 feature2 commit"
runCmd git commit --allow-empty -m "JIRA-4 feature2 commit"

echo -e "\n\n"
echo -e "Feature branch 2 created with some dummy commits"
read -p "Press [Enter] key to finish the feature..."

runCmd mvn ggitflow:feature-finish -DfeatureName=feature/JIRA-2-Feature2

read -p "Feature 1 finished. Please verify and then manually push the merge to remote develop..."