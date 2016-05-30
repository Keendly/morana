FROM anapsix/alpine-java:jre8
MAINTAINER MoOmEeN <moomeen@gmail.com>

ENV PROJECT_DIR /opt/jarilo

RUN mkdir -p $PROJECT_DIR
RUN mkdir $PROJECT_DIR/log

COPY target/jarilo*.jar $PROJECT_DIR/jarilo.jar
ENV JAR_PATH $PROJECT_DIR/jarilo.jar

WORKDIR /tmp
RUN wget "http://kindlegen.s3.amazonaws.com/kindlegen_linux_2.6_i386_v2_9.tar.gz" && \
    tar -zxf kindlegen_linux_2.6_i386_v2_9.tar.gz --no-same-owner && \
    cp kindlegen $PROJECT_DIR && \
    rm -r *

ENV KINDLEGEN_PATH $PROJECT_DIR/kindlegen
RUN chmod +x $KINDLEGEN_PATH

CMD java -Xmx100m -jar $JAR_PATH $KINDLEGEN_PATH
