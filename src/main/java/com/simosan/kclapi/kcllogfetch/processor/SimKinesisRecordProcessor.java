package com.simosan.kclapi.kcllogfetch.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.simosan.kclapi.kcllogfetch.domain.SimKinesisExtractDataUnprocessed;
import com.simosan.kclapi.kcllogfetch.domain.SimkinesisExtractData;
import com.simosan.kclapi.kcllogfetch.inflastructure.SimKinesisDataExport;
import com.simosan.kclapi.kcllogfetch.inflastructure.SimKinesisDataExportlog;
import com.simosan.kclapi.kcllogfetch.service.SimAwsClientManageService;
import com.simosan.kclapi.kcllogfetch.service.SimKinesisDataExportProcessorService;
import com.simosan.kclapi.kcllogfetch.service.SimKinesisDateTimePositionFromDynamodb;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.ShardRecordProcessor;

public class SimKinesisRecordProcessor implements ShardRecordProcessor {

	// slf4jでログに出力したりコンソコールに出力したりする。（logback.xmlがなければコンソール出力）
	private static final Logger log = LoggerFactory.getLogger(SimKinesisRecordProcessor.class);
	private String shardId;
	private AwsCredentialsProvider credentialsProvider;
	private Region region;
	private SimKinesisDataExportProcessorService svc;
	// private String endpointuri;

	/**
	 * ShardRecordProcessor（processRecords）からのデータ配信前にKCLによって初期化
	 * あわせて初期化にかかるログ（ShardId、所属するシャード内のパーティションキーごとの一意キー）を出力
	 * 
	 * @param initializationInput Processor初期化に関連する情報を引数にとるが意識しない
	 */
	@Override
	public void initialize(InitializationInput initializationInput) {
		this.shardId = initializationInput.shardId();
		log.info("SimKinesisRecordProcessor.initialize - DEBUG-initialize");
		log.info("SimKinesisRecordProcessor.initialize - Initializing record processor for shard: "
				+ initializationInput.shardId());
		log.info("SimKinesisRecordProcessor.initialize - Initializing @ Sequence: "
				+ initializationInput.extendedSequenceNumber());

		// インターネットダイレクト
		SimAwsClientManageService sacms = new SimAwsClientManageService();
		credentialsProvider = sacms.retriveCredentialProvider();
		region = sacms.retriveRegion();
		SimKinesisDateTimePositionFromDynamodb skdtfd = new SimKinesisDateTimePositionFromDynamodb(credentialsProvider,
				region);
		SimkinesisExtractData sked = new SimKinesisExtractDataUnprocessed();
		SimKinesisDataExport skde = new SimKinesisDataExportlog();
		svc = new SimKinesisDataExportProcessorService(sked, skde, skdtfd);

		// EndpointURI設定
		/*
		 * SimAwsClientManageService sacms = new
		 * SimAwsClientManageService(SimGetprop.getProp("endpointuri"));
		 * credentialsProvider = sacms.retriveCredentialProvider(); region =
		 * sacms.retriveRegion(); endpointuri = SimGetprop.getProp("endpointuri");
		 * skdtfd = new SimKinesisDateTimePositionFromDynamodb(credentialsProvider,
		 * region, endpointuri);
		 */

		// proxy設定
		/*
		 * this.proxy =
		 * ProxyConfiguration.builder().host(SimGetprop.getProp("proxyhost"))
		 * .port(Integer.parseInt(SimGetprop.getProp("proxyport"))).build();
		 * this.httpclient =
		 * NettyNioAsyncHttpClient.builder().proxyConfiguration(this.proxy).build();
		 * 
		 * // AssumeRoleをロード SimAssumeRoleCred sarc = new SimAssumeRoleCred(httpclient);
		 * credentialsProvider = sarc.loadCredentials();
		 * 
		 * region = Region.of(SimGetprop.getProp("region")); tbName =
		 * SimGetprop.getProp("postbname"); skcadt = new
		 * SimKinesisDateTimePositionFromDynamodb(credentialsProvider, region,
		 * httpclient);
		 */
	}

	/**
	 * データレコードを処理。KCLはデータレコードをログ（EC2とか）に出力。
	 * 
	 * @param processRecordsInput データレコードに対する関連するレコード情報（どこまで読み込んだかのチェックポイント等）を保持
	 */
	@Override
	public void processRecords(ProcessRecordsInput processRecordsInput) {
		svc.dataProcessor(processRecordsInput);
	}

	/**
	 * recordProcessorに関連づけられたリースが失われると実行される。
	 * リースが失われるとrecordProcessorはチェックポイントを実行できなくなる。
	 *
	 * @param leaseLostInput リース損失に関連する情報を保持（サンプルなので使ってない）
	 */
	@Override
	public void leaseLost(LeaseLostInput leaseLostInput) {

		log.warn("SimKinesisRecordProcessor.leaseLost - Lost lease, so terminating. shardId = " + shardId);

	}

	/**
	 * 本処理対象のシャードの全てのデータを読み込んだ時に実行される。処理完了後、チェックポイントをDynamoDBに保持。
	 *
	 * @param shardEndedInput チェックポイントへのアクセスを可能とする
	 */
	@Override
	public void shardEnded(ShardEndedInput shardEndedInput) {
		try {
			log.warn("Reached shard end checkpointing. shardId = " + shardId);
			shardEndedInput.checkpointer().checkpoint();
		} catch (ShutdownException | InvalidStateException e) {
			log.warn("SimKinesisRecordProcessor.shardEnded - Exception while checkpointing at shard end. Giving up.",
					e);
		}
	}

	/**
	 * KCLスケジューラがシャットダウンした時に呼び出される。終了時にチェックポイントとログを出力。
	 *
	 * @param shutdownRequestedInput チェックポイントへのアクセスを可能とし、シャットダウン完了前にチェックポイントを出力
	 */
	@Override
	public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
		try {
			log.warn("Scheduler is shutting down, checkpointing. shardId = " + shardId);
			shutdownRequestedInput.checkpointer().checkpoint();
		} catch (ShutdownException | InvalidStateException e) {
			log.warn(
					"SimKinesisRecordProcessor.shutdownRequested - Exception while checkpointing at requested shutdown. Giving up.",
					e);
		}
	}
}