#!/bin/sh

# Support/ServicePack 3.0.0.0 - Support/ServicePack version = 3.0.1.0-SNAPSHOT
mvn ggitflow:support-start

git log --branches --remotes --tags --graph --oneline --decorate