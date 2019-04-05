FROM openjdk:alpine

ENV APP_HOME /app/

COPY target/re-agent*.jar $APP_HOME/re-agent.jar

# copy sigar libs
COPY sigar-libs $APP_HOME/sigar-libs

WORKDIR $APP_HOME
CMD java -jar re-agent.jar
