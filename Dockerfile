FROM java:8
MAINTAINER MoOmEeN <moomeen@gmail.com>

ENV PROJECT_DIR /opt/jarilo

RUN mkdir -p $PROJECT_DIR
RUN mkdir $PROJECT_DIR/log

COPY $CIRCLE_ARTIFACTS/jarilo*.jar $PROJECT_DIR/jarilo.jar
ENV JAR_PATH $PROJECT_DIR/jarilo.jar

CMD java -jar $JAR_PATH $KINDLEGEN_PATH
