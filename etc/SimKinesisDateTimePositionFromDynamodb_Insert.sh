#!/bin/bash

/opt/homebrew/bin/aws dynamodb put-item --table-name SimKinesisConsumeAppDatePosTbl --item '{"LogGroupKey" :{"S": "simkpltest"},"dtp":{"S": "2022-01-01T11:00:00"}}' --profile localstack --endpoint-url http://simubu:4566
