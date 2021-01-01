#!/bin/sh

/cadvisor-limiter

exec /usr/bin/cadvisor -logtostderr "$@"
