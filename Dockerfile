FROM java:8
MAINTAINER The Guardian

RUN apt-get --yes update && apt-get --yes install \
  ruby \
  nginx
