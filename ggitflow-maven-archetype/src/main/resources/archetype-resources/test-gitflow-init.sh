#!/bin/sh

# Init
mvn ggitflow:init

git log --branches --remotes --tags --graph --oneline --decorate