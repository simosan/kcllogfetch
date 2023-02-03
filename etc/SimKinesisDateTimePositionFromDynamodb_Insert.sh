#!/bin/bash

/opt/homebrew/bin/aws dynamodb put-item --table-name SimKinesisConsumeAppDateTimePos --item '{"LogGroupKey" :{"S": "simstream"},"dtp":{"S": "2022-01-01T11:00:00"}}' --profile localstack --endpoint-url http://simubu:4566
