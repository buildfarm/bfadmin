#!/bin/bash
# This script runs a basic buildfarm admin test
cd main;

echo "Running buildfarm admin tests"
./mvnw clean test package
