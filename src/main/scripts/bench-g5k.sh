#!/bin/sh
if [ -f $OAR_NODEFILE ]; then
	WORKERS="/tmp/$(basename $0).$$.tmp"
	cat $OAR_NODEFILE|sort|uniq|tail -n +2 > $WORKERS
else
	echo "This script must be launched from the main node that contains the variable \$OAR_NODEFILE"
	exit 1
fi

if [ $# -eq 1 -and $1 -eq "clean" ]; then
    taktuk --connector /usr/bin/oarsh -o connector -o status -f $WORKERS \
    broadcast exec [ "killall java" ] 2>&1 >> clean.log
    exit 0
elif [ $# -ne 2 ]; then
    echo "Usage: $0 type output_file"
    exit 1
fi

ROOT=`pwd`
PORT=8080
MASTER=`hostname -s`
OUTPUT=$2
TYPE=$1

#set -x
prg=`dirname $0`
function runJob {
    $prg/benchmark_daemon.sh $1 &
    echo "Dispatcher launched"
    sleep 5
    echo "Starting workers"
    taktuk --connector /usr/bin/oarsh -o connector -o status -f $WORKERS \
    broadcast exec [ "cd $ROOT/$prg; ./benchmark_client.sh $MASTER:$PORT" ] 2>&1 >> taktuk.$$.log
    echo "Workers are terminated"
    sleep 15
    echo "Cleaning"
    killall java
}

case ${TYPE} in

all)
    echo "-- Benching every SLA --"
    for SLA in void buf buf_migs buf_migs_vcpu; do
        $0 ${SLA} ${OUTPUT}
    done
    ;;
*)
    SLA="config/sla_${TYPE}.xml"
    if [ ! -e ${SLA} ]; then
        echo "Unknown SLA ${TYPE}: '${SLA}' not found"
        exit 1;
    fi
    echo "-- Benching SLA ${TYPE} (${SLA}) --"
    for SIZE in 500 1000 1500 2000 2500; do
        o="$OUTPUT/s${SIZE}-${TYPE}.txt"
        echo "size=${SIZE};  output=$o"
        if [ -e $o ]; then
            echo "Skipping"
        else
            runJob "-i bench${SIZE} -sla ${SLA} -o $o -t 0 -p ${PORT}"
        fi
    done
    ;;
esac
rm -rf $WORKERS