FROM debian:bullseye

RUN apt-get update
RUN apt install -y wget unzip make git
RUN apt install -y openjdk-17-jdk
RUN apt install -y python3 python3-pip
RUN apt install -y autoconf
RUN apt install -y maven
RUN pip3 install frida-tools
RUN pip3 install Jinja2
RUN pip3 install bashlex
RUN pip3 install pyparsing
RUN wget https://raw.githubusercontent.com/gdraheim/docker-systemctl-replacement/master/files/docker/systemctl3.py -O /usr/bin/systemctl && chmod +x /usr/bin/systemctl


RUN mkdir /jackson
RUN wget https://search.maven.org/remotecontent?filepath=com/fasterxml/jackson/core/jackson-databind/2.13.0-rc2/jackson-databind-2.13.0-rc2.jar -O /jackson/jackson-databind.jar
RUN wget https://search.maven.org/remotecontent?filepath=com/fasterxml/jackson/core/jackson-core/2.13.0-rc2/jackson-core-2.13.0-rc2.jar -O /jackson/jackson-core.jar
RUN wget https://search.maven.org/remotecontent?filepath=com/fasterxml/jackson/core/jackson-annotations/2.13.0-rc2/jackson-annotations-2.13.0-rc2.jar -O /jackson/jackson-annotations.jar


RUN mkdir /workdir

ENV CLASSPATH="$CLASSPATH:/bytebuddy"
ENV MAIN_HOST=localhost
ENV JDWP_PORT=1000
ENV FRIDA_SERV_PORT=1500