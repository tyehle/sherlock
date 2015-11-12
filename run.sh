#!/bin/bash

java -cp out/production/sherlock:lib/* cs.utah.sherlock.Driver "$@" 2> /dev/null

