#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
java tools/GenerateLocalSetup.java .
