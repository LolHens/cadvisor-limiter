#!/bin/sh

/cadvisor-limiter.sh.bat

exec /usr/bin/cadvisor -logtostderr -port 8081 "$@"
