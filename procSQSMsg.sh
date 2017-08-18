#!/bin/bash
export APP_ENV=Production
cd /data/Scripts
./doSQSMsgProc.sc &> /data/Scripts/Logs/runlog.log