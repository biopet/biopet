#!/bin/bash

DIR=`readlink -f \`dirname $0\``

cp -r $DIR/../*/*/src/* $DIR/src

