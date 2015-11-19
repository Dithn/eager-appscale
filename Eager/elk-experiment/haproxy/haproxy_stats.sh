#!/bin/sh
echo "show info;show stat" | socat stdio unix-connect:/etc/haproxy/stats | grep watchtower
