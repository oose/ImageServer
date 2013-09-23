#!/bin/bash

play stage
target/start -Dhttp.port=9001 -Dimage.dir="/Users/markusklink/Pictures/Export/BobWayne/" -Dpidfile.path="PID9001.pid" &
target/start -Dhttp.port=9002 -Dimage.dir="/Users/markusklink/Pictures/Export/GetLostFest/" -Dpidfile.path="PID9002.pid" &
target/start -Dhttp.port=9003 -Dimage.dir="/Users/markusklink/Pictures/Export/Flowers/" -Dpidfile.path="PID9003.pid" &
