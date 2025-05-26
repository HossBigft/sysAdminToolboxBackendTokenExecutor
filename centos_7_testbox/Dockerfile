FROM centos:7

RUN sed -i 's|^mirrorlist=|#mirrorlist=|g' /etc/yum.repos.d/CentOS-*.repo && \
    sed -i 's|^#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*.repo && \
    yum clean all && yum makecache


RUN yum -y install which
RUN yum -y install sudo
RUN adduser secops

# to pass the check about plesk or bind executable presense
RUN mkdir -p /opt/isc/isc-bind/root/usr/sbin/ && ln -s /usr/bin/ls /opt/isc/isc-bind/root/usr/sbin/rndc

ADD secOpsDispatcher /home/secops/