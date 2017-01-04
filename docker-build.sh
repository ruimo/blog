#!/bin/sh
bin/activator universal:packageZipTarball
docker build --no-cache -t ruimo/blog .
