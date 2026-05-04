#!/bin/bash

export $(grep -v '^#' kafka.env | xargs)

docker-compose up -d