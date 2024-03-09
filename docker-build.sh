#!/bin/sh -ex
./build.sh
docker build --no-cache -t ruimo/blog .
