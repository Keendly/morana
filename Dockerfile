FROM java:8
MAINTAINER MoOmEeN <moomeen@gmail.com>

RUN mkdir -p /opt/jindle
RUN mkdir /opt/jindle/log

RUN wget "http://kindlegen.s3.amazonaws.com/kindlegen_linux_2.6_i386_v2_9.tar.gz" && \
    tar -zxf kindlegen_linux_2.6_i386_v2_9.tar.gz && \
    cp kindlegen /opt/jindle/

RUN chmod +x /opt/jindle/kindlegen

COPY target/jindle*.jar /opt/jindle/jindle.jar

ENTRYPOINT ["java", "-jar", "/opt/jindle/jindle.jar", "/opt/jindle/kindlegen"]
