#!/bin/sh
sbt universal:packageZipTarball
docker build --no-cache -t ruimo/blog .
