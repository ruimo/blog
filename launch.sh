#!/bin/sh -x
. ./settings.conf
docker run -d -e APP_SECRET="${APP_SECRET}" -e BLOG_OPT="${BLOG_OPT}" --name blog -u `id -u blog` -p ${PUBLISH_PORT}:9000 -v /home/blog:/root -v /etc/timezone:/etc/timezone:ro ruimo/blog $*
