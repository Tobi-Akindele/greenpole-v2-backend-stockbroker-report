#!/bin/bash

#Change OS time zone
mv -f /etc/localtime /etc/localtime.bak
ln -s /usr/share/zoneinfo/Africa/Lagos /etc/localtime

#download keystore from repo
set -e
cd /opt/greenpolestockbrokerreport/config/


cd /opt/greenpolestockbrokerreport/

java -jar -Dspring.profiles.active=docker greenpole-stockbroker-report-0.0.1-SNAPSHOT.jar