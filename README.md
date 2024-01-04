# Overview
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fsimosan%2Fkcllogfetch.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fsimosan%2Fkcllogfetch?ref=badge_shield)


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

|  Item              |  Description                                                    | required or optional   |
| ------------------ | --------------------------------------------------------------- | ---------------------- |
| appname            | Name of the app that KCL registers in DynamoDB                  | required               |
| prof               | AWS profile name                                                | required               |
| region             | AWS region name                                                 | required               |
| streamname         | Kinesis stream name                                             | required               |
| rolesesname        | Name of the assesume role                                       | required               |
| rolearn            | Resource name for the assesume role                             | required               |
| postbname          | Name of the DynamoDB table for positioning                      | required               |
| partitionkey       | Name of the DynamoDB table partition key for positioning        | required               |
| partitionkey_value | Value of the partition key of the DynamoDB table for positioning| required               |
| dtpkey             | Date key of the DynamoDB table for positioning                  | required               |
| timezoneid         | Time zone for KinesisStream acquisition messages                | required               |
| extracttype        | Type of KinesisStream message (NORM or SUBSCRIPTION)<br>-> Uncompressed message or compressed message like Cloudwatchlogs        | required               |
| exporttype         | Specify data output destination (currently only LOG)            | required               |
| proxyhost          | Server host name when specifying proxy                          | optional               |
| proxyport          | Server port name when specifying proxy                          | optional               |
| endpointuri        | URI when using endpoint                                         | optional               |



## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fsimosan%2Fkcllogfetch.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fsimosan%2Fkcllogfetch?ref=badge_large)