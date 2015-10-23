#!/bin/bash

DIR=`readlink -f \`dirname $0\``

rm -r $DIR/src/main $DIR/src/test

