#!/bin/bash

verbose=false
name="developset"

while getopts ":n:v" opt; do
  case $opt in
    n)
      name=$OPTARG
      ;;
    v)
      verbose=true
      ;;
    \?)
      echo "Invalid option: -$OPTARG was ignored" >&2
      ;;
  esac
done


java -cp out/production/sherlock:lib/* cs.utah.sherlock.Driver "$name-manifest" > answers

if [[ $verbose = true ]]; then
    perl score-answers.pl answers "$name-answers"
else
    perl score-answers.pl answers "$name-answers" | tail -n 11
fi

#rm answers

