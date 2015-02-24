#!/bin/bash

# This script copy all src dirs from all projects to the target dir before. This way we get a agregated test coverage report.

my_dir="$(dirname "$0")"

mkdir -p $my_dir/../target/src/

cp -r $my_dir/../../*/*/src/* $my_dir/../target/src/

exit 0

