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

# Support/ServicePack 3.0.0.0 - Support/ServicePack version = 3.0.1.0-SNAPSHOT
runCmd mvn ggitflow:support-start

git log --branches --remotes --tags --graph --oneline --decorate