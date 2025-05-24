# syntax=docker/dockerfile:1

ARG targetarch=aarch64

FROM debian:12 AS base

RUN apt-get update
RUN apt-get upgrade -y

RUN apt-get install -y \
    apt-transport-https \
    ca-certificates \
    chromium \
    chromium-driver \
    curl \
    firefox-esr \
    git \
    gradle \
    openjdk-17-jdk \
    openjdk-17-jdk-headless \
    rsync \
    software-properties-common \
    sudo \
    weasyprint \
    wget

FROM base AS graalvm

ARG graalvmarch=aarch64

ADD https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.2/graalvm-community-jdk-21.0.2_linux-${graalvmarch}_bin.tar.gz /tmp/graalvm.tar.gz
RUN tar -xzf /tmp/graalvm.tar.gz -C /usr/lib/jvm && \
    rm /tmp/graalvm.tar.gz

ENV GRAALVM_HOME=/usr/lib/jvm/graalvm-community-openjdk-21.0.2+13.1
ENV JAVA_HOME=/usr/lib/jvm/graalvm-community-openjdk-21.0.2+13.1

FROM graalvm AS xmluser

ARG UID=501
ARG GID=20

ENV GRADLE_USER_HOME=/xmlcalabash/gradle
ENV SAXON_HOME=/private

# Configure environment

RUN echo "GID: ${GID}" && if getent group ${GID} > /dev/null; [ $? -ne 0 ]; \
    then groupadd --gid=${GID} xmluser; fi && \
    useradd --uid=${UID} --gid=${GID} --shell /bin/bash --create-home xmluser --home-dir /opt/xmluser && \
    echo 'xmluser ALL=NOPASSWD: ALL' >> /etc/sudoers.d/50-xmluser && \
    sudo -u xmluser mkdir /opt/xmluser/bin

ENV PATH=/opt/xmluser/bin:$PATH

RUN mkdir /src
RUN chown xmluser /src

CMD ["/bin/bash"]

