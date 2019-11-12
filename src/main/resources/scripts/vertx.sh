#!/bin/sh

# Instructions for SysVinit (Debian):
# 1) Put file to  /etc/init.d/ 
# 2) chmod +x /etc/init.d/vertx.sh
# 3) create logs directories:
#	mkdir /var/logs/
# 4)* enable port : 
#	 iptables -A INPUT -p tcp -s 79.187.47.205 --dport 8082 -j ACCEPT
# 5) enable script at system start:
#	ln -s /etc/init.d/vertx.sh /etc/rc5.d/S90vertx
# notes : for systemctl systems :
#	http://stackoverflow.com/questions/21503883/spring-boot-application-as-a-service/22121547#22121547
# 6) run scritp at system shutdown:
#	ln -s /etc/init.d/vertx.sh /etc/rc0.d/k09vertx

JAVA_HOME=/opt/jdk8/jdk1.8.0_112
SERVICE_NAME=Vertx
PATH_TO_JAR=/var/vertx/vertx-0.0.1-SNAPSHOT-fat.jar
PID_PATH_NAME=/tmp/vertx-pid
CONF_PATH=/var/vertx/my-application-conf.json
STD_OUT_PATH=/dev/null
ERROR_LOG=/var/logs/vertx.log
JAVA_OPTS="-server -Xms1g -Xmx1g"
TRUE=0
FALSE=1

#$1 - pid file name 
runs() {
	if [ -e $PID_PATH_NAME ]; then
  	PID=$(cat $PID_PATH_NAME);
	  	OUT=$(ps -p $PID >> /dev/null)
	  	STATUS=$?
		if [ $STATUS = "0" ];then
		  echo $TRUE;
		else
		 echo $FALSE;
	  	fi
	  else
	  	echo $FALSE;
	  fi
}

case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        RES=$(runs)
        if [ $RES = $FALSE ]; then
            nohup $JAVA_HOME/bin/java $JAVA_OPTS -jar $PATH_TO_JAR -conf $CONF_PATH /tmp 2>> $ERROR_LOG >> $STD_OUT_PATH &
                        echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
    	RES=$(runs)
        if [ $RES = $TRUE ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stoping ..."
            kill $PID;
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
            if [ -f $PID_PATH_NAME ]; then
            	rm $PID_PATH_NAME
            fi	
        fi
    ;;
    restart)
    	RES=$(runs)
        if [ $RES = $TRUE ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stopping ...";
            kill $PID;
            echo "$SERVICE_NAME stopped ...";
            rm $PID_PATH_NAME
            echo "$SERVICE_NAME starting ..."
            nohup $JAVA_HOME/bin/java $JAVA_OPTS -jar $PATH_TO_JAR -conf $CONF_PATH /tmp 2>> $ERROR_LOG >> $STD_OUT_PATH &
                        echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    status)
	  # Check the status of the process.
	  RES=$(runs)
	  if [ $RES = $TRUE ]; then
		  echo "$SERVICE_NAME Process is running"
		else
		  echo "$SERVICE_NAME Process is not running"
	  	fi
	;;
    *)
 		 echo "Usage: vartx.sh ( commands ... )"
 		 echo "  start            start Vertx"
 		 echo "  stop             stop Vertx"
 		 echo "  restart          restart Vertx"
 		 echo "  status           status of Vertx process"
  	;;
esac

