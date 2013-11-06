#!/bin/bash

CMD=./target/universal/stage/bin/imageserver

echo -e "\033[1m"
figlet ImageServer
echo -e "\033[0m"

echo -e "\033[1mDeleting tag directories\033[0m"

rm -rf ./tmp/camel/*

echo -e "\033[1mStaging application\033[0m"
play stage

echo -e "\033[1mInvoking servers\033[0m"

$CMD -Dhttp.port=9001 -Dcamel.endpoint="file://tmp/camel/server1?autoCreate=true" -Dimage.dir="/Users/markusklink/Pictures/Export/BobWayne/" -Dpidfile.path="PID9001.pid" &
$CMD -Dhttp.port=9002 -Dcamel.endpoint="file://tmp/camel/server2?autoCreate=true" -Dimage.dir="/Users/markusklink/Pictures/Export/GetLostFest/" -Dpidfile.path="PID9002.pid" &
$CMD -Dhttp.port=9003 -Dcamel.endpoint="activemq:evaluations" -Dimage.dir="/Users/markusklink/Pictures/Export/Flowers/" -Dpidfile.path="PID9003.pid" &

echo -e "\033[1mServers running\033[0m"
