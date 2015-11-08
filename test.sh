#!/bin/bash

#if [[ $# = 1 ]]
#then
#    name=$1
#else
#    name="developset"
#fi

perl score-answers.pl <(java -cp out/production/sherlock:lib/* cs.utah.sherlock.Driver developset-manifest) "developset-answers" | tail -n 11
