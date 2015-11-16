#!/bin/sh
curl -v -X PUT -d @add_dynamic_template.json http://host:port/_template/testwatchtower
