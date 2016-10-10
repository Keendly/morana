FROM anapsix/alpine-java:jre8
MAINTAINER MoOmEeN <moomeen@gmail.com>

ENV PROJECT_DIR /opt/jarilo

RUN mkdir -p $PROJECT_DIR

COPY target/jarilo*.jar $PROJECT_DIR/jarilo.jar
ENV JAR_PATH $PROJECT_DIR/jarilo.jar

WORKDIR /tmp
RUN wget "http://kindlegen.s3.amazonaws.com/kindlegen_linux_2.6_i386_v2_9.tar.gz" && \
    tar -zxf kindlegen_linux_2.6_i386_v2_9.tar.gz --no-same-owner && \
    cp kindlegen $PROJECT_DIR && \
    rm -r *

ENV KINDLEGEN_PATH $PROJECT_DIR/kindlegen
RUN chmod +x $KINDLEGEN_PATH

CMD java -Dcom.sun.management.jmxremote.port=9999 \
         -Dcom.sun.management.jmxremote.rmi.port=9999 \
         -Dcom.sun.management.jmxremote.authenticate=false \
         -Dcom.sun.management.jmxremote.ssl=false \
         -Djava.rmi.server.hostname='52.51.160.213' \
         -Dcom.sun.management.jmxremote.local.only=false \
         -XX:+UnlockCommercialFeatures -XX:+FlightRecorder \
         -Xmx100m -XX:+PrintGCDateStamps -Xloggc:$PROJECT_DIR/log/gc.log -XX:MaxDirectMemorySize=50m -jar $JAR_PATH \
         --kindlegen $KINDLEGEN_PATH --inboundQueue https://sqs.eu-west-1.amazonaws.com/625416862388/generation-queue --outboundTopic arn:aws:sns:eu-west-1:625416862388:GenerateEbookResult
