package com.simosan.kclapi.kcllogfetch.service.awsconnection;

import java.net.URI;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.common.SimGetprop;
import com.simosan.kclapi.kcllogfetch.processor.SimKinesisRecordProcessorFactory;
import com.simosan.kclapi.kcllogfetch.service.SimAssumeRoleCred;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;

public class EndpointUriConnection implements ConnectionType {
	
	private static final Logger log = LoggerFactory.getLogger(EndpointUriConnection.class);
	private final AwsCredentialsProvider credentialsProvider;
	private final Region region;
	private final DynamoDbAsyncClient dynamoClient;
	private final CloudWatchAsyncClient cloudWatchClient;
	private final KinesisAsyncClient kinesisClient;
	private final ConfigsBuilder configsBuilder;
	
	public EndpointUriConnection(final String endpointuri) {

		SimAssumeRoleCred sarc = null;
		try {
			sarc = new SimAssumeRoleCred(URI.create(endpointuri));
		} catch (Exception e) {
			log.error("ProxyConnection SimAssumeRoleCred Exception!");
		}
		this.credentialsProvider = sarc.loadCredentials();
		// リージョン名（注意）書き方は東京リージョンの場合、ap-northeast-1にすること
		// v1.xのマニュアルではAP_NORTHEAST_1となっているが、V2.xではエラーになる
		this.region = Region.of(SimGetprop.getProp("region"));
		// Dynamoクライアント初期化
		this.dynamoClient = DynamoDbAsyncClient.builder().credentialsProvider(credentialsProvider).region(region)
				.endpointOverride(URI.create(endpointuri)).build();
		// Cloudwatchクライアント（kinesisの状態をCloudwatchに出力）初期化
		this.cloudWatchClient = CloudWatchAsyncClient.builder().credentialsProvider(credentialsProvider).region(region)
				.endpointOverride(URI.create(endpointuri)).build();
		// Kinesisクライアント初期化
		this.kinesisClient = KinesisAsyncClient.builder().credentialsProvider(credentialsProvider).region(region)
				.endpointOverride(URI.create(endpointuri)).build();
		// KCLビルダーオブジェクト初期化
		this.configsBuilder = new ConfigsBuilder(SimGetprop.getProp("streamname"), SimGetprop.getProp("appname"),
				kinesisClient, dynamoClient, cloudWatchClient, UUID.randomUUID().toString(),
				new SimKinesisRecordProcessorFactory());
	}
	
	@Override	
	public DynamoDbAsyncClient retriveDynamoClient() {
				
		return this.dynamoClient;
	}
	
	@Override	
	public ConfigsBuilder retriveconfigsBuilder() {
		return this.configsBuilder;
	}

	@Override
	public AwsCredentialsProvider retriveCredentialProvider() {
		return this.credentialsProvider;
	}
	
	@Override
	public Region retriveRegion() {
		return this.region;
	}

	
}
