FROM azul/zulu-openjdk:17
MAINTAINER Shisei Hanai<shanai@jp.ibm.com>

RUN apt-get update
RUN apt-get install imagemagick -y

ADD target/universal /opt/blog
RUN cd /opt/blog && \
  cmd=$(basename *.tgz .tgz) && \
  tar xf ${cmd}.tgz && \
  echo printenv > launch.sh && \
  echo "ls -lh /opt/blog" >> launch.sh && \
  echo 'kill -9 `cat /opt/blog/$cmd/RUNNING_PID`' && \
  echo rm -f /opt/blog/$cmd/RUNNING_PID >> launch.sh && \
  echo /opt/blog/$cmd/bin/blog-server -Duser.home=/root -DapplyEvolutions.default=true -Dplay.http.secret.key=\${APP_SECRET} \$BLOG_OPT >> launch.sh && \
  chmod +x launch.sh && \
  chmod -R 777 /opt/blog

EXPOSE 9000

ENTRYPOINT ["/bin/bash", "-c", "/opt/blog/launch.sh"]