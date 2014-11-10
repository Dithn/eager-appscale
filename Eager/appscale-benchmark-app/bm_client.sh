#!/bin/sh
curl -v -X POST -d @req.json -H "Content-type: application/json" http://localhost:8080/predict
