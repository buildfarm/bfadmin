#!/bin/bash
# This script runs a basic buildfarm admin test using local Bazel

echo "Running buildfarm admin tests with local Bazel"
./bazel test //... --test_output=errors

echo "Building buildfarm admin with local Bazel"
./bazel build //...
