figlet ImageServer
echo -e "\033[1mStopping servers\033[0m"
kill  `cat PID9001.pid` > /dev/null
kill  `cat PID9002.pid` > /dev/null
kill  `cat PID9003.pid` > /dev/null

rm *.pid


