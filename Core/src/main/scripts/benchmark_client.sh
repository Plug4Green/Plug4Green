#!/bin/sh


MY=`dirname $0`

JAVA_OPTS="-Xmx10G -Xms10G -server -da"
#Define the classpath
JARS=`ls $MY/lib/*.jar`
 
for JAR in $JARS; do
 CLASSPATH=$JAR:$CLASSPATH
done
java $JAVA_OPTS -cp $CLASSPATH org.f4g.optimizer.BenchClient $*
