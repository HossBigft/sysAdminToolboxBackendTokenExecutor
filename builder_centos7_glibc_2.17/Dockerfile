FROM centos:7

RUN sed -i 's|^mirrorlist=|#mirrorlist=|g' /etc/yum.repos.d/CentOS-*.repo && \
    sed -i 's|^#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*.repo && \
    yum clean all && yum makecache


RUN yum -y install yum-utils epel-release && \
    yum -y install \
        gcc \
        glibc-devel \
        zlib-devel \
        libstdc++-static \
        tar \
        which && \
    yum clean all


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

RUN mkdir -p /build-output && chmod 755 /build-output
