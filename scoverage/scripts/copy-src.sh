#!/bin/bash

my_dir="$(dirname "$0")"

mkdir -p $my_dir/../target/src/

cp -r $my_dir/../../*/*/src/* $my_dir/../target/src/

exit 0

