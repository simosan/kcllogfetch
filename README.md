# Overview

* This tool uses the Kinesis Client Library (v2) to retrieve data from Kinesis Streams.

## About

* This Java project consists of a maven project.
* We have checked the operation with JavaSE-17.
* In addition to getting messages from a regular Kinesis Stream, CloudwatchLogs subscriptions (compressed logs) can also be retrieved.

## Usage

* Specify the property file as an argument to the built executable.
```
   e.g.) java -Dproppath=~/kcllogfetch.properties -jar kcllogfetch.jar

```

* The specifications of the property file are as follows

appname (required)
　Name of the app that KCL registers in DynamoDB (lease table)
prof (required)
　AWS profile name
region (required)
  AWS region name
streamname (required)
　Kinesis stream name
rolesesname (required)
  Name of the assesume role (required)
rolearn (required)
  Resource name for the assesume role
proxyhost (optional)
  Server host name when specifying proxy
proxyport (optional)
　Server port name when specifying proxy
endpointuri (optional)
　URI when using endpoint
postbname (required)
  Name of the DynamoDB table for positioning
partitionkey (required)
　Name of the DynamoDB table partition key for positioning
partitionkey_value (required)
  Value of the partition key of the DynamoDB table for positioning
dtpkey (required)
  Date key of the DynamoDB table for positioning
timezoneid (required)
  Time zone for KinesisStream acquisition messages
extracttype (required)
  Type of KinesisStream message (NORM or SUBSCRIPTION)
　 -> Uncompressed message or compressed message like Cloudwatchlogs

