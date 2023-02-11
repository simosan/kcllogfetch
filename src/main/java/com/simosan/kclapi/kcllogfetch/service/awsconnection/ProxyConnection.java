package com.simosan.kclapi.kcllogfetch.service.awsconnection;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.common.SimGetprop;
import com.simosan.kclapi.kcllogfetch.processor.SimKinesisRecordProcessorFactory;
import com.simosan.kclapi.kcllogfetch.service.SimAssumeRoleCred;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;

public class ProxyConnection implements ConnectionType {
	
	private static final Logger log = LoggerFactory.getLogger(ProxyConnection.class);
	private final ProxyConfiguration proxy;
	private final SdkAsyncHttpClient httpclient;
	private final AwsCredentialsProvider credentialsProvider;
	private final Region region;
	private final DynamoDbAsyncClient dynamoClient;
	private final CloudWatchAsyncClient cloudWatchClient;
	private final KinesisAsyncClient kinesisClient;
	private final ConfigsBuilder configsBuilder;
	
	public ProxyConnection(final String proxyhost, final String proxyport) {

		// proxy設定
		this.proxy = ProxyConfiguration.builder().host(proxyhost)
				.port(Integer.parseInt(proxyport)).build();
		this.httpclient = NettyNioAsyncHttpClient.builder().proxyConfiguration(this.proxy).build();
		SimAssumeRoleCred sarc = null;
		try {
			sarc = new SimAssumeRoleCred(this.httpclient);
		} catch (Exception e) {
			log.error("ProxyConnection SimAssumeRoleCred Exception!");
		}
		this.credentialsProvider = sarc.loadCredentials();
		// リージョン名（注意）書き方は東京リージョンの場合、ap-northeast-1にすること
		// v1.xのマニュアルではAP_NORTHEAST_1となっているが、V2.xではエラーになる
		this.region = Region.of(SimGetprop.getProp("region"));
		// Dynamoクライアント初期化
		this.dynamoClient = DynamoDbAsyncClient.builder().credentialsProvider(credentialsProvider)
				.region(region).httpClient(httpclient).build();
		// Cloudwatchクライアント（kinesisの状態をCloudwatchに出力）初期化
		this.cloudWatchClient = CloudWatchAsyncClient.builder().credentialsProvider(credentialsProvider)
				.region(region).httpClient(httpclient).build();
		// Kinesisクライアント初期化
		this.kinesisClient = KinesisAsyncClient.builder().credentialsProvider(credentialsProvider)
				.region(region).httpClient(httpclient).build();
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
	
	public SdkAsyncHttpClient retriveHttpclient() {
		return this.httpclient;
	}
	
}
