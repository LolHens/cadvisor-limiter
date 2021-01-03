#!/bin/sh

/cadvisor-limiter

exec /usr/bin/cadvisor -logtostderr -port 8081 "$@"
