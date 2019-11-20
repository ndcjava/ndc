#!/bin/sh

PWD=$(cd `dirname $0`;pwd);
opsdir=$1
cd ${PWD}
git pull
mvn clean package -Dmaven.test.skip=true
cp -f ./dubbo-server-cart/target/dubbo-server-cart-1.0-SNAPSHOT.jar ${opsdir}/dubbo-server-cart/dubbo-server.jar
cp -f ./dubbo-server-order/target/dubbo-server-order-1.0-SNAPSHOT.jar ${opsdir}/dubbo-server-order/dubbo-server.jar
cp -f ./dubbo-server-pay/target/dubbo-server-pay-1.0-SNAPSHOT.jar ${opsdir}/dubbo-server-pay/dubbo-server.jar
cp -f ./dubbo-client-one/target/dubbo-client-one-1.0-SNAPSHOT.war ${opsdir}/webserver/tomcat8/webapps/
cd ${opsdir}/webserver/tomcat8/webapps
unzip -oq dubbo-client-one-1.0-SNAPSHOT.war -d dubbo-client
rm -rf dubbo-client-one-1.0-SNAPSHOT.war
