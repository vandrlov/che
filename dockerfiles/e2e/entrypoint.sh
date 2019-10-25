#!/bin/bash

# Validate selenium base URL
if [ -z "$TS_SELENIUM_BASE_URL" ]; then
    echo "The \"TS_SELENIUM_BASE_URL\" is not set!";
    echo "Please, set the \"TS_SELENIUM_BASE_URL\" environment variable."
    exit 1
fi

# Set testing suite
if [ -z "$TEST_SUITE" ]; then
    TEST_SUITE=test-happy-path
fi

# Launch selenium server
/usr/bin/supervisord --configuration /etc/supervisord.conf & \
export TS_SELENIUM_REMOTE_DRIVER_URL=http://localhost:4444/wd/hub

# Check selenium server launching
expectedStatus=200
currentTry=1
maximumAttempts=5

while [ $(curl -s -o /dev/null -w "%{http_code}" --fail http://localhost:4444/wd/hub/status) != $expectedStatus ];
do
  if (( currentTry > maximumAttempts ));
  then
    status=$(curl -s -o /dev/null -w "%{http_code}" --fail http://localhost:4444/wd/hub/status)
    echo "Exceeded the maximum number of checking attempts,"
    echo "selenium server status is '$status' and it is different from '$expectedStatus'";
    exit 1;
  fi;

  echo "Wait selenium server availability ..."

  curentTry=$((curentTry + 1))
  sleep 1
done

# Print information about launching tests
if mount | grep 'e2e'; then
	echo "The local code is mounted. Executing local code."
	cd /tmp/e2e || exit
	npm install
else
	echo "Executing e2e tests from an image."
	cd /tmp/e2e || exit
fi


# Launch tests
if [ $TEST_SUITE == "load-test" ]; then

  echo "Running LOAD TESTS with user: $TS_SELENIUM_USERNAME"
  export TS_SELENIUM_REPORT_FOLDER="./$TS_SELENIUM_USERNAME/report"
  export TS_SELENIUM_LOAD_TEST_REPORT_FOLDER="./$TS_SELENIUM_USERNAME/load-test-folder"
  CONSOLE_LOGS="./$TS_SELENIUM_USERNAME/console-log.txt"
  mkdir $TS_SELENIUM_USERNAME
  touch $CONSOLE_LOGS

  npm run $TEST_SUITE 2>&1 | tee $CONSOLE_LOGS

  echo "Zipping a files..."
  zip -q -r $TS_SELENIUM_USERNAME.zip ./$TS_SELENIUM_USERNAME

  echo "Sending via FTP..."
  #cp -r ./$TS_SELENIUM_USERNAME /mnt/mountedVolume/$TIMESTAMP/$TS_SELENIUM_USERNAME
  ftp -n load-tests-ftp-service << End_script 
  user user pass1234
  binary
  put $TS_SELENIUM_USERNAME.zip
  quit
End_script
  
  echo "Files sent to load-tests-ftp-service."
else
  echo "Running TEST_SUITE: $TEST_SUITE with user: $TS_SELENIUM_USERNAME and pass: $TS_SELENIUM_PASSWORD"
  npm run $TEST_SUITE
fi
