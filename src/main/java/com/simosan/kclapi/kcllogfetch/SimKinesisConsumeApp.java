package com.simosan.kclapi.kcllogfetch;


/**
 * Kinesisコンシューマーのワーカー（スケジューラ）
 *
 */
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException; 
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
//import software.amazon.awssdk.http.SdkHttpClient;
//import software.amazon.awssdk.http.apache.ApacheHttpClient;
//import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.coordinator.Scheduler;

/** Kinesis Client Library（以降、KCL）を利用して、KinesisStreamのキューデータを読み込む。
 *  本プログラム実行前にKinesisStreamのセットアップを完了させておく必要がある。
 *  本プログラムで利用しているKCLはバージョン2.xを利用している。1.xとライブラリの互換性は一切ないため注意。
 *  本プログラム（KCL）を動作させるためには、Kinesis、DynamoDB、CloudwatchのIAMを
 *  事前セットアップしておく必要がある。詳しくは以下URL参照。
 *  https://docs.aws.amazon.com/ja_jp/streams/latest/dev/tutorial-stock-data-kplkcl-iam.html
 */
/* @author sim
 *
 */
public class SimKinesisConsumeApp {

	private static final Logger log = LoggerFactory.getLogger(SimKinesisConsumeApp.class);
	private Region region;
	private String tbName;
	private KinesisAsyncClient kinesisClient;
	private DynamoDbAsyncClient dynamoClient;
	private CloudWatchAsyncClient cloudWatchClient;
	private AwsCredentialsProvider credentialsProvider;
	private ProxyConfiguration proxy;
	private SdkAsyncHttpClient httpclient;


	private SimKinesisConsumeApp() {
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
		this.credentialsProvider = sarc.loadCredentials(httpclient);
		// リージョン名（注意）書き方は東京リージョンの場合、ap-northeast-1にすること
		// v1.xのマニュアルではAP_NORTHEAST_1となっているが、V2.xではエラーになる
		this.region = Region.of(SimGetprop.getProp("region"));
		// Kinesisクライアント初期化
		// Dynamoクライアント（kinesis設定をDynamoに格納）初期化
		this.dynamoClient = DynamoDbAsyncClient.builder()
				.credentialsProvider(credentialsProvider)
				.region(region)
				.httpClient(httpclient)
				.build();
		// Cloudwatchクライアント（kinesisの状態をCloudwatchに出力）初期化
		this.cloudWatchClient = CloudWatchAsyncClient.builder()
				.credentialsProvider(credentialsProvider)
				.region(region)
				.httpClient(httpclient)
				.build();
		// Kinesisクライアント初期化
		this.kinesisClient = KinesisAsyncClient.builder()
				.credentialsProvider(credentialsProvider)
				.region(region)
				.httpClient(httpclient)
				.build();

	}

	/**
	 * メインメソッド
	 */
	public static void main(String[] args) {

		log.warn("MainThread Start!");
		new SimKinesisConsumeApp().run();
	}

	/**
	 * KCLのセットアップ。DynamoDB（チェックポイント格納）、Cloudwatch（KCL監視）セットアップも含む。
	 * SimKinesisRecordProcessorFactoryはSimKinesisRecordProcessorFactoryを実装したレコードプロセッサー生成クラス
	 */
	private void run() {
		Date date = null;


		ConfigsBuilder configsBuilder = new ConfigsBuilder(
				SimGetprop.getProp("streamname"),
				SimGetprop.getProp("appname"),
				kinesisClient,
				dynamoClient,
				cloudWatchClient,
				UUID.randomUUID().toString(),
				new SimKinesisRecordProcessorFactory()
				);

		/**
		 * KCLのエントリポイントを生成（2.xからスケジューラという。1.xはワーカー）。
		 * InitialPositionInStreamのパラメータを変更したい場合、既存のDynamoDBテーブルを削除しないと有効にならない。
		 * パラメータはTRIM_HORIZON(取得していない最も古いレコード、LATEST(最も新しいレコード）、AT_TIMESTAMP（タイムスタンプ指定）
		 * ちなみにTRIM_HORIZONはテーブル新規作成後（初回起動時）はKinesisStreamに存在するデータをすべて拾ってくる（Default24時間保持）。
		 * それが嫌な場合は、AT_TIMESTAMPをつかう。（本APはこれを採用）
		 * AT_TIMESTAMPはDate型を指定し、Kinesis側が保持する標準時刻を意識する。
		 * タイムスタンプの更新はDynamoDB（個別実装）でやる。
		 * （ここ重要）個別実装のDynamoDBテーブルのタイムスタンプを変更（過去に遡るとか）した場合、KCLが生成したDynamoDBテーブル（プログラム名のもの）
		 * もあわせて削除すること（整合性があわなくなり動作しなくなる）。
		 */

		//起動時はDynamoDBのSimKinesisConsumeAppDateTimePosテーブルから存在する最新のタイムスタンプを取得
		//タイムスタンプを遡りたいのであれば、DynamoDBのタイムスタンプを直接いじること。
		//スケジューラが起動したらそれ以降のポジショニングはKCLがいい感じにしてくれる
		tbName = SimGetprop.getProp("postbname");
		SimKinesisConsumeAppDtPos skcadt = new SimKinesisConsumeAppDtPos(
				credentialsProvider,
				region,
				tbName,
				httpclient);

		String tmstr = skcadt.getTimestampItem(
				SimGetprop.getProp("partitionkey"),
				SimGetprop.getProp("partitionkey_value"),
				SimGetprop.getProp("dtpkey"));

		// DynamoDBのdtpが空だったら最新のタイムスタンプに更新
		if (tmstr == null)
		{
			skcadt.updateTimestampItem(
					SimGetprop.getProp("partitionkey"),
					SimGetprop.getProp("partitionkey_value"),
					SimGetprop.getProp("dtpkey"));
			tmstr = skcadt.getTimestampItem(
					SimGetprop.getProp("partitionkey"), 
					SimGetprop.getProp("partitionkey_value"),
					SimGetprop.getProp("dtpkey"));
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		try {
			date = sdf.parse(tmstr);
		} catch(ParseException e) {
			log.error("時刻のフォーマットがおかしいです。", e);
			System.exit(255);
		}

		Scheduler scheduler = new Scheduler(
				configsBuilder.checkpointConfig(),
				configsBuilder.coordinatorConfig(),
				configsBuilder.leaseManagementConfig(),
				configsBuilder.lifecycleConfig(),
				configsBuilder.metricsConfig(),
				configsBuilder.processorConfig(),
				configsBuilder.retrievalConfig().initialPositionInStreamExtended(
						InitialPositionInStreamExtended.newInitialPositionAtTimestamp(date))
				);

		//kill SIGTERM [pid]等でシグナルをキャッチしてKCLを安全に停止するため、JVMに停止用メソッドを登録。
		//JVM停止前にstartGracefulShutdownを呼び出し、完全性を確保。
		Runtime.getRuntime().addShutdownHook(new Thread(() -> stopMainThread(scheduler)));

		/**
		 * スケジューラ開始
		 */
		Thread schedulerThread = new Thread(scheduler);
		schedulerThread.setDaemon(true);
		schedulerThread.start();

		try {
			//子スレッドが終了したらKCLメインスレッドを正常に停止
			schedulerThread.join();
		} catch (InterruptedException e1) {
			//例外を検知してもKCLメインスレッドを正常に停止
			log.error("Main Thread InterruptedException Occur!");
		} finally {
			System.exit(0);
		}
	}

	/**
	 * KCLクライアントを安全に停止
	 * @param  s 起動中のスケジューラ
	 */
	private void stopMainThread(Scheduler s) {

		Future<Boolean> gracefulShutdownFuture = s.startGracefulShutdown();
		log.warn("Waiting up to 20 seconds for shutdown to complete.");
		try {
			gracefulShutdownFuture.get(20, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.error("Interrupted while waiting for graceful shutdown. Continuing.");
		} catch (ExecutionException e) {
			log.error("Exception while executing graceful shutdown.", e);
		} catch (TimeoutException e) {
			log.error("Timeout while waiting for shutdown.  Scheduler may not have exited.");
		}
		log.warn("Completed, shutting down now.");
	}

}
