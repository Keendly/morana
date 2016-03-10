FROM java:8
MAINTAINER MoOmEeN <moomeen@gmail.com>

ENV PROJECT_DIR /opt/jarilo

RUN echo $HOME

RUN mkdir -p $PROJECT_DIR
RUN mkdir $PROJECT_DIR/log

COPY target/jarilo*.jar $PROJECT_DIR/jarilo.jar
ENV JAR_PATH $PROJECT_DIR/jarilo.jar

RUN wget "http://kindlegen.s3.amazonaws.com/kindlegen_linux_2.6_i386_v2_9.tar.gz" && \
    tar -zxf --no-same-owner kindlegen_linux_2.6_i386_v2_9.tar.gz && \
    cp kindlegen $PROJECT_DIR

ENV KINDLEGEN_PATH $PROJECT_DIR/kindlegen
RUN chmod +x $KINDLEGEN_PATH

CMD java -jar $JAR_PATH $KINDLEGEN_PATH
