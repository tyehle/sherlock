#!/bin/bash

#verbose=false
#
#if [[ $1 = "-v" ]]; then
#    verbose=true
#fi
#
#else
#    name="developset"
#fi

java -cp out/production/sherlock:lib/* cs.utah.sherlock.Driver developset-manifest > answers

perl score-answers.pl answers "developset-answers" | tail -n 11

rm answers
