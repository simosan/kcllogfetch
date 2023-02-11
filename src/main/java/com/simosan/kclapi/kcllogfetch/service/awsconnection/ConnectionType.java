package com.simosan.kclapi.kcllogfetch.service.awsconnection;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;

public interface ConnectionType {

	public DynamoDbAsyncClient retriveDynamoClient();
	public ConfigsBuilder retriveconfigsBuilder();
	public AwsCredentialsProvider retriveCredentialProvider();
	public Region retriveRegion();

}
