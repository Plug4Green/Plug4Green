#!/bin/sh


MY=`dirname $0`

JAVA_OPTS="-Xmx4G -Xms4G -server -da"
#Define the classpath
JARS=`ls $MY/lib/*.jar`
 
for JAR in $JARS; do
 CLASSPATH=$JAR:$CLASSPATH
done
java $JAVA_OPTS -cp $CLASSPATH f4g.optimizer.Benchmark $*
