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
<br>
　Name of the app that KCL registers in DynamoDB (lease table)
<br>
prof (required)
<br>
　AWS profile name
<br>
region (required)
<br>
  AWS region name
<br>
streamname (required)
<br>
　Kinesis stream name
<br>
rolesesname (required)
<br>
  Name of the assesume role (required)
<br>
rolearn (required)
<br>
  Resource name for the assesume role
<br>
proxyhost (optional)
<br>
  Server host name when specifying proxy
<br>
proxyport (optional)
<br>
　Server port name when specifying proxy
<br>
endpointuri (optional)
<br>
　URI when using endpoint
<br>
postbname (required)
<br>
  Name of the DynamoDB table for positioning
<br>
partitionkey (required)
<br>
　Name of the DynamoDB table partition key for positioning
<br>
partitionkey_value (required)
<br>
  Value of the partition key of the DynamoDB table for positioning
<br>
dtpkey (required)
<br>
  Date key of the DynamoDB table for positioning
<br>
timezoneid (required)
<br>
  Time zone for KinesisStream acquisition messages
<br>
extracttype (required)
<br>
  Type of KinesisStream message (NORM or SUBSCRIPTION)
<br>
　 -> Uncompressed message or compressed message like Cloudwatchlogs

