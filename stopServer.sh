echo -e "\033[1mStopping servers\033[0m"
kill  `cat PID9001.pid`
kill  `cat PID9002.pid`
kill  `cat PID9003.pid`
