FROM centos:8

RUN sed -i 's|^mirrorlist=|#mirrorlist=|g' /etc/yum.repos.d/CentOS-*.repo && \
    sed -i 's|^#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*.repo && \
    dnf clean all && dnf makecache

RUN dnf -y install dnf-plugins-core && \
    dnf config-manager --set-enabled powertools && \
    dnf -y install gcc glibc-devel zlib-devel libstdc++-static tar which && \
    dnf clean all


WORKDIR /tmp
ADD https://download.oracle.com/graalvm/24/latest/graalvm-jdk-24_linux-x64_bin.tar.gz graalvm.tar.gz

RUN tar -xzf graalvm.tar.gz -C /usr/lib && \
    rm graalvm.tar.gz && \
    mv /usr/lib/graalvm-* /usr/lib/graalvm

ENV JAVA_HOME=/usr/lib/graalvm
ENV PATH=$JAVA_HOME/bin:$PATH


WORKDIR /app
COPY pom.xml /app/
COPY .mvn /app/.mvn
COPY mvnw /app/mvnw

# Preload dependencies
RUN ./mvnw dependency:go-offline -B
