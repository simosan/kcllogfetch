package com.simosan.kclapi.kcllogfetch;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.ShardRecordProcessor;


public class SimKinesisRecordProcessor implements ShardRecordProcessor{

	//slf4jでログに出力したりコンソコールに出力したりする。（logback.xmlがなければコンソール出力）
	private static final Logger log = LoggerFactory.getLogger(SimKinesisRecordProcessor.class);
	private String shardId;
	private AwsCredentialsProvider credentialsProvider;
	private SimKinesisConsumeAppDtPos skcadt;
	private Region region;
	private String tbName;
	private ProxyConfiguration proxy;
	private SdkAsyncHttpClient httpclient;

	/**
	 * ShardRecordProcessor（processRecords）からのデータ配信前にKCLによって初期化
	 * あわせて初期化にかかるログ（ShardId、所属するシャード内のパーティションキーごとの一意キー）を出力
	 * @param initializationInput　Processor初期化に関連する情報を引数にとるが意識しない
	 */
	public void initialize(InitializationInput initializationInput) {
		this.shardId = initializationInput.shardId();
		log.warn("Initializing record processor for shard: " + initializationInput.shardId());
		log.warn("- Initializing @ Sequence: " + initializationInput.extendedSequenceNumber());

		// proxy設定
		this.proxy = ProxyConfiguration.builder()
				.host(SimGetprop.getProp("proxyhost"))
				.port(Integer.parseInt(SimGetprop.getProp("proxyport")))
				.build();
		this.httpclient = NettyNioAsyncHttpClient.builder()
				.proxyConfiguration(this.proxy)
				.build();

		//AssumeRoleをロード
		SimAssumeRoleCred sarc = new SimAssumeRoleCred();
		credentialsProvider = sarc.loadCredentials(httpclient);

		region = Region.of(SimGetprop.getProp("region"));
		tbName = SimGetprop.getProp("postbname");
		skcadt = new SimKinesisConsumeAppDtPos(credentialsProvider, region, tbName, httpclient);

	}


	/**
	 * データレコードを処理。KCLはデータレコードをログ（EC2とか）に出力。
	 * @param processRecordsInput　データレコードに対する関連するレコード情報（どこまで読み込んだかのチェックポイント等）を保持
	 */
	@Override
	public void processRecords(ProcessRecordsInput processRecordsInput) {

		try {
			log.warn("Processing {} record(s)", processRecordsInput.records().size());
			processRecordsInput.records().forEach(r -> processSingleRecord(r.partitionKey(), r.sequenceNumber(), r.data()));
			//processRecordsInput.records().forEach(r -> log.info("Processing record pk: {} -- Seq: {} -- SimKinesisRecordProcessor-Data: {}",
			//		r.partitionKey(), r.sequenceNumber(), extractJsonMessage(getBufferzipData(r.data())))
			//);
		} catch (Throwable t) {
			log.error("Caught throwable while processing records. Aborting.");
			Runtime.getRuntime().halt(1);
		}
	}

	/**
	 * recordProcessorに関連づけられたリースが失われると実行される。
	 * リースが失われるとrecordProcessorはチェックポイントを実行できなくなる。
	 *
	 * @param leaseLostInput　リース損失に関連する情報を保持（サンプルなので使ってない）
	 */
	@Override
	public void leaseLost(LeaseLostInput leaseLostInput) {

		log.warn("Lost lease, so terminating. shardId = " + shardId);

	}

	/**
	 * 本処理対象のシャードの全てのデータを読み込んだ時に実行される。処理完了後、チェックポイントをDynamoDBに保持。
	 *
	 * @param shardEndedInput　チェックポイントへのアクセスを可能とする
	 */
	@Override
	public void shardEnded(ShardEndedInput shardEndedInput) {

		try {
			log.warn("Reached shard end checkpointing. shardId = " + shardId);
			shardEndedInput.checkpointer().checkpoint();
		} catch (ShutdownException | InvalidStateException e) {
			log.warn("Exception while checkpointing at shard end. Giving up.", e);
		}
	}

	/**
	 * KCLスケジューラがシャットダウンした時に呼び出される。終了時にチェックポイントとログを出力。
	 *
	 * @param shutdownRequestedInput　チェックポイントへのアクセスを可能とし、シャットダウン完了前にチェックポイントを出力
	 */
	@Override
	public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {

		try {
			log.warn("Scheduler is shutting down, checkpointing. shardId = " + shardId);
			shutdownRequestedInput.checkpointer().checkpoint();
		} catch (ShutdownException | InvalidStateException e) {
			log.warn("Exception while checkpointing at requested shutdown. Giving up.", e);
		}
	}

	/**
	 * Kinesisに溜め込んだデータバッファを取得し、JSONデータの"Message"のみ取得する。
	 * データバッファは圧縮されているため解凍する必要あり。
	 * データを取得したらDynamoDBのポジショニング用タイムスタンプを更新
	 * @param ByteBuffer　KinesisClientRecordのdata
	 * @return 解凍したデータ（JSONデータ）
	 */
	private String getBufferzipData(ByteBuffer d)
	{
		String strline = null;
		String message = new String();
		byte[] arr = new byte[d.remaining()];
		d.get(arr);
		GZIPInputStream gis;
		try {
			gis = new GZIPInputStream(new ByteArrayInputStream(arr));
			BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "UTF-8"));

			while ((strline = bf.readLine()) != null) {
				message += strline;
			}

		} catch (IOException e) {
			log.error("KinesisClientRecord Buffer Error!", e);
		}

		//ログ取得したら最新時刻でDynamoDBのテーブルを更新する。
		skcadt.updateTimestampItem(
				SimGetprop.getProp("partitionkey"),
				SimGetprop.getProp("partitionkey_value"),
				SimGetprop.getProp("dtpkey"));

		return message;
	}


	/**
	 * データレコード単位での処理（ログ出力）。kinesisにログが出力されたタイミングで発呼
	 * getBufferzipDataメソッドが返したJSONデータから"message"を抽出
	 * @param k kinesisログキー
	 * @param seqnum kinesisログシーケンスナンバー
	 * @param dt kinesisデータ（圧縮されている）,ByteBuffer型
	 */
	private void processSingleRecord(String k, String seqnum, ByteBuffer dt) {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root;

		String jsondt = getBufferzipData(dt);

		try {
			root = mapper.readTree(jsondt);
			//ロググループ名取得
			JsonNode loggrp = root.get("logGroup");
			//CloudwatchLogsのログがJson Or フラットであっても以下実装で出力可能
			for (JsonNode n : root.get("logEvents")) {
				log.info("ProcessingRecordPk:{},Seq:{},LogGroup:{},Data:{}",
						k,seqnum,loggrp,n.get("message").asText());
			}
		} catch (IOException e) {
			log.error("extractJsonMessage: Json Parse Error!", e);
		}

	}

}