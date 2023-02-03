package com.simosan.kclapi.kcllogfetch.service;

import java.net.URI;
import java.util.UUID;

import com.simosan.kclapi.kcllogfetch.common.SimGetprop;
import com.simosan.kclapi.kcllogfetch.processor.SimKinesisRecordProcessorFactory;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;

public class SimAwsClientManageService {
	private final Region region;
	private final KinesisAsyncClient kinesisClient;
	private final DynamoDbAsyncClient dynamoClient;
	private final CloudWatchAsyncClient cloudWatchClient;
	private final AwsCredentialsProvider credentialsProvider;
	private final ProxyConfiguration proxy;
	private final SdkAsyncHttpClient httpclient;
	private final ConfigsBuilder configsBuilder;

	//インターネットダイレクト
	public SimAwsClientManageService() {
		this.proxy = null;
		this.httpclient = null;

		SimAssumeRoleCred sarc = new SimAssumeRoleCred();
		this.credentialsProvider = sarc.loadCredentials();
		// リージョン名（注意）書き方は東京リージョンの場合、ap-northeast-1にすること
		// v1.xのマニュアルではAP_NORTHEAST_1となっているが、V2.xではエラーになる
		this.region = Region.of(SimGetprop.getProp("region"));
		// Kinesisクライアント初期化（に必要なDynamoDBとCloudwatchも）
		/// Dynamoクライアント（kinesis設定をDynamoDBに格納）初期化
		this.dynamoClient = DynamoDbAsyncClient.builder().credentialsProvider(credentialsProvider)
				.region(region).build();
		/// Cloudwatchクライアント（kinesisの状態をCloudwatchに出力）初期化
		this.cloudWatchClient = CloudWatchAsyncClient.builder().credentialsProvider(credentialsProvider)
				.region(region).build();
		/// Kinesisクライアント初期化
		this.kinesisClient = KinesisAsyncClient.builder().credentialsProvider(credentialsProvider)
				.region(region).build();

		this.configsBuilder = new ConfigsBuilder(SimGetprop.getProp("streamname"), SimGetprop.getProp("appname"),
				kinesisClient, dynamoClient, cloudWatchClient, UUID.randomUUID().toString(),
				new SimKinesisRecordProcessorFactory());
	}
	
	//EndpointURI経由
	public SimAwsClientManageService(final String endpointuri) {
		this.proxy = null;
		this.httpclient = null;
		
		SimAssumeRoleCred sarc = new SimAssumeRoleCred(URI.create(endpointuri));
		this.credentialsProvider = sarc.loadCredentials();
		// リージョン名（注意）書き方は東京リージョンの場合、ap-northeast-1にすること
		// v1.xのマニュアルではAP_NORTHEAST_1となっているが、V2.xではエラーになる
		this.region = Region.of(SimGetprop.getProp("region"));
		// Kinesisクライアント初期化（に必要なDynamoDBとCloudwatchも）
		/// Dynamoクライアント（kinesis設定をDynamoDBに格納）初期化
		this.dynamoClient = DynamoDbAsyncClient.builder().credentialsProvider(credentialsProvider).region(region)
				.endpointOverride(URI.create(endpointuri)).build();
		/// Cloudwatchクライアント（kinesisの状態をCloudwatchに出力）初期化
		this.cloudWatchClient = CloudWatchAsyncClient.builder().credentialsProvider(credentialsProvider).region(region)
				.endpointOverride(URI.create(endpointuri)).build();
		/// Kinesisクライアント初期化
		this.kinesisClient = KinesisAsyncClient.builder().credentialsProvider(credentialsProvider).region(region)
				.endpointOverride(URI.create(endpointuri)).build();

		this.configsBuilder = new ConfigsBuilder(SimGetprop.getProp("streamname"), SimGetprop.getProp("appname"),
				kinesisClient, dynamoClient, cloudWatchClient, UUID.randomUUID().toString(),
				new SimKinesisRecordProcessorFactory());
	}
	
	//proxy経由
	public SimAwsClientManageService(final String proxyhost, final String proxyport) {
		// proxy設定
		this.proxy = ProxyConfiguration.builder().host(proxyhost)
				.port(Integer.parseInt(proxyport)).build();
		this.httpclient = NettyNioAsyncHttpClient.builder().proxyConfiguration(this.proxy).build();
		// AssumeRoleをロード
		SimAssumeRoleCred sarc = new SimAssumeRoleCred(httpclient);
		this.credentialsProvider = sarc.loadCredentials();
		// リージョン名（注意）書き方は東京リージョンの場合、ap-northeast-1にすること
		// v1.xのマニュアルではAP_NORTHEAST_1となっているが、V2.xではエラーになる
		this.region = Region.of(SimGetprop.getProp("region"));
		// Kinesisクライアント初期化
		// Dynamoクライアント（kinesis設定をDynamoに格納）初期化
		this.dynamoClient = DynamoDbAsyncClient.builder().credentialsProvider(credentialsProvider).region(region)
				.httpClient(httpclient).build();
		// Cloudwatchクライアント（kinesisの状態をCloudwatchに出力）初期化
		this.cloudWatchClient = CloudWatchAsyncClient.builder().credentialsProvider(credentialsProvider).region(region)
				.httpClient(httpclient).build();
		// Kinesisクライアント初期化
		this.kinesisClient = KinesisAsyncClient.builder().credentialsProvider(credentialsProvider).region(region)
				.httpClient(httpclient).build();

		this.configsBuilder = new ConfigsBuilder(SimGetprop.getProp("streamname"), SimGetprop.getProp("appname"),
				kinesisClient, dynamoClient, cloudWatchClient, UUID.randomUUID().toString(),
				new SimKinesisRecordProcessorFactory());
	}

	public ConfigsBuilder retriveconfigsBuilder() {
		return this.configsBuilder;
	}

	public AwsCredentialsProvider retriveCredentialProvider() {
		return this.credentialsProvider;
	}

	public Region retriveRegion() {
		return this.region;
	}

	public SdkAsyncHttpClient retriveHttpClient() {
		return this.httpclient;
	}

}
