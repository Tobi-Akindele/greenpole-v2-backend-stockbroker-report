FROM openjdk:11-jre-slim

FROM maven:3.6.3-jdk-11 AS MAVEN_BUILD


COPY setup.sh /root/greenpolestockbrokerreport/setup.sh
RUN chmod +x /root/greenpolestockbrokerreport/setup.sh
RUN /root/greenpolestockbrokerreport/setup.sh


COPY pom.xml /build/
COPY src /build/src/
WORKDIR /build/
RUN mvn package -U -Dmaven.test.skip=true
RUN ls /build/target
RUN cp /build/target/greenpole-stockbroker-report-0.0.1-SNAPSHOT.jar /opt/greenpolestockbrokerreport



WORKDIR /


COPY install.sh /root/greenpolestockbrokerreport/install.sh
RUN chmod +x /root/greenpolestockbrokerreport/install.sh
CMD  /root/greenpolestockbrokerreport/install.sh