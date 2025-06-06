#!/bin/bash
set -e

# Clear or create log file
: > out.log

# Run Maven, log to file, pipe JSON lines to jq
./mvnw -q clean spring-boot:run 2>&1 | tee out.log | jq .
