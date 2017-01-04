FROM java:8-jdk
MAINTAINER Shisei Hanai<shanai@jp.ibm.com>

RUN apt-get update
RUN apt-get upgrade -y
RUN apt-get dist-upgrade -y

ADD target/universal /opt/blog
RUN cd /opt/blog && \
  cmd=$(basename *.tgz .tgz) && \
  tar xf ${cmd}.tgz && \
  echo printenv > launch.sh && \
  echo /opt/blog/$cmd/bin/blog-server -DapplyEvolutions.default=true -Dplay.crypto.secret=\${APP_SECRET} >> launch.sh && \
  chmod +x launch.sh

EXPOSE 9000

ENTRYPOINT ["/bin/bash", "-c", "/opt/blog/launch.sh"]