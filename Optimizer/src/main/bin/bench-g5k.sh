#!/bin/sh
if [ $# -ne 3 ]; then
    echo "Usage: $0 type workers output_file"
    exit 1
fi

ROOT=`pwd`
PORT=8080
MASTER=`hostname -s`
OUTPUT=$3
WORKERS=$2
TYPE=$1

#set -x
prg=`dirname $0`
function runJob {
    $prg/benchmark_daemon.sh $1 &
echo "Dispatcher launched"
    sleep 5
echo "Starting workers"
   taktuk --connector /usr/bin/oarsh -o connector -o status -f $WORKERS \
broadcast exec [ "cd $ROOT/$prg; ./benchmark_client.sh $MASTER:$PORT" ] 2>&1 > taktuk.log
echo "Workers are terminated"
 sleep 15
echo "Cleaning"
    killall java
}

case ${TYPE} in

sla)
echo "-- TODO:Benching the impact of the sla--"
;;
size)
echo "-- Benching the impact of the datacenter size--"
    for SIZE in 500 1000 1500 2000 2500; do
	    o="$OUTPUT/s${SIZE}-void.txt"
	    echo "size=${SIZE};  output=$o"
	    if [ -d $o ]; then
		echo "Skipping"
	    else
		runJob "-i bench${SIZE} -sla config/sla_void.xml -o $o -t 0 -p ${PORT}"
	    fi
    done
;;
*)
echo "Unknown type ${TYPE}"
esac