#!/bin/bash

for file in `find lib/src -name '*.java'`; do
  grep 'http://www.apache.org/licenses/LICENSE-2.0' $file > /dev/null && continue
  cat script/header.txt $file > ${file}.new
  mv ${file}.new $file
done
