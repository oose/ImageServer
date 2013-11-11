
if hash figlet 2>/dev/null; then
    figlet ImageServer
else
    echo -e ImageServer
fi

echo -e "\033[1mStopping servers\033[0m"
kill  `cat PID9001.pid` > /dev/null
kill  `cat PID9002.pid` > /dev/null
kill  `cat PID9003.pid` > /dev/null

rm *.pid


