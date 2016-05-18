#!/bin/sh

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

BASE_DIR=$( git rev-parse --show-toplevel )
cd $BASE_DIR
 
$SCRIPT_DIR/clear-gitflow-config.sh

##
# Removes all Git history 
# Reconstructs the repo with only the current content 
# Pushes to origin
##

git checkout develop

git fetch --all

ORIGIN=$(git config --get remote.origin.url)
echo "Origin URL '$ORIGIN'"

##
# Delete remote tags
##
TAGS=(`git ls-remote --tags --refs --quiet | sed -e 's|.*refs/tags/||g'`)
echo "Remote Tags '$TAGS'"
for i in "${TAGS[@]}"
do
   echo "Deleting Tag '$i'" 
   git push --delete origin $i -f
done

git fetch --all --prune

##
# Delete remote branches
##
REMOTE_BRNS=(`git ls-remote --heads --refs --quiet | sed -e 's|.*refs/heads/||g'`)
echo "Remote Branches '${REMOTE_BRNS}'"
for i in "${REMOTE_BRNS[@]}"
do
   echo "Deleting '$i'" 
   git push --delete origin $i -f
done

git fetch --all --prune

mvn versions:set -DgenerateBackupPoms=false -DnewVersion=1.0-SNAPSHOT

rm -rf .git
git init
git add .
git commit -m "Initial commit. pom version 1.0-SNAPSHOT"

# git remote add origin https://github.com/dkirrane/ggitflow-test1.git
git remote add origin $ORIGIN
git push origin master --force

git fetch --all