#!/bin/bash

v="$*"

while [ -z "$v" ]; do
  echo -n "Version e.g., 2.4-M3-0.9 "
  read v
done


ROOT=/Volumes/release/net/liftmodules

for d in $ROOT/imap-idle*
do
 mkdir $d/$v
done

