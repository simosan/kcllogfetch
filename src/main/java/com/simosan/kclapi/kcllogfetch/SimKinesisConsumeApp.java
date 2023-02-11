package com.simosan.kclapi.kcllogfetch;

/**
 * Kinesisコンシューマーのワーカー（スケジューラ）
 *
 */
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.common.SimGetprop;
import com.simosan.kclapi.kcllogfetch.common.SimPropertyCheck;
import com.simosan.kclapi.kcllogfetch.service.SimAwsConnectionManageService;
import com.simosan.kclapi.kcllogfetch.service.awsconnection.ConnectionType;
import com.simosan.kclapi.kcllogfetch.service.KinesisDateTimePosition;

import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.coordinator.Scheduler;

/**
 * Kinesis Client Library（以降、KCL）を利用して、KinesisStreamのキューデータを読み込む。
 * 本プログラム実行前にKinesisStreamのセットアップを完了させておく必要がある。
 * 本プログラムで利用しているKCLはバージョン2.xを利用している。1.xとライブラリの互換性は一切ないため注意。
 * 本プログラム（KCL）を動作させるためには、Kinesis、DynamoDB、CloudwatchのIAMを
 * 事前セットアップしておく必要がある。詳しくは以下URL参照。
 * https://docs.aws.amazon.com/ja_jp/streams/latest/dev/tutorial-stock-data-kplkcl-iam.html
 */

public class SimKinesisConsumeApp {

	private static final Logger log = LoggerFactory.getLogger(SimKinesisConsumeApp.class);
	private ConfigsBuilder configsBuilder;

	/**
	 * メインメソッド
	 */
	public static void main(String[] args) {
		// プロパティファイルのチェック
		SimPropertyCheck spc = new SimPropertyCheck();
		if(!spc.chkPropertyfile()) {
			log.error("引数に誤りがあります");
			System.exit(255);
		}
		
		log.warn("MainThread Start!");
		new SimKinesisConsumeApp().run();
	}

	/**
	 * KCLのセットアップ。DynamoDB（チェックポイント格納）、Cloudwatch（KCL監視）セットアップも含む。
	 * SimKinesisRecordProcessorFactoryはSimKinesisRecordProcessorFactoryを実装したレコードプロセッサー生成クラス
	 */
	private void run() {

		// 各種AWSサービスのアクセス方法（InternetDirect or EndpointURI or Proxy）を選択
		SimAwsConnectionManageService sacms = new SimAwsConnectionManageService();
		ConnectionType con = sacms.retriveConnection();
		configsBuilder = con.retriveconfigsBuilder();
		// Streamメッセージ取得後のタイムポジション（DynamoDB）更新用インスタンス取得
		KinesisDateTimePosition skdtfd = new KinesisDateTimePosition(con.retriveDynamoClient(), SimGetprop.getProp("postbname"));
		
		/**
		 * KCLのエントリポイントを生成（2.xからスケジューラという。1.xはワーカー）。
		 * InitialPositionInStreamのパラメータを変更したい場合、既存のDynamoDBテーブルを削除しないと有効にならない。
		 * パラメータはTRIM_HORIZON(取得していない最も古いレコード、LATEST(最も新しいレコード）、AT_TIMESTAMP（タイムスタンプ指定）
		 * ちなみにTRIM_HORIZONはテーブル新規作成後（初回起動時）はKinesisStreamに存在するデータをすべて拾ってくる（Default24時間保持）。
		 * それが嫌な場合は、AT_TIMESTAMPをつかう。（本APはこれを採用）
		 * AT_TIMESTAMPはDate型を指定し、Kinesis側が保持する標準時刻を意識する。 タイムスタンプの更新はDynamoDB（個別実装）でやる。
		 * （ここ重要）個別実装のDynamoDBテーブルのタイムスタンプを変更（過去に遡るとか）した場合、KCLが生成したDynamoDBテーブル（プログラム名のもの）
		 * もあわせて削除すること（整合性があわなくなり動作しなくなる）。
		 */
				
		Scheduler scheduler = new Scheduler(configsBuilder.checkpointConfig(), configsBuilder.coordinatorConfig(),
				configsBuilder.leaseManagementConfig(), configsBuilder.lifecycleConfig(),
				configsBuilder.metricsConfig(), configsBuilder.processorConfig(),
				configsBuilder.retrievalConfig().initialPositionInStreamExtended(
						InitialPositionInStreamExtended.newInitialPositionAtTimestamp(skdtfd.retrieveTimeposition())));
		
		// kill SIGTERM [pid]等でシグナルをキャッチしてKCLを安全に停止するため、JVMに停止用メソッドを登録。
		// JVM停止前にstartGracefulShutdownを呼び出し、完全性を確保。
		Runtime.getRuntime().addShutdownHook(new Thread(() -> stopMainThread(scheduler)));

		/**
		 * スケジューラ開始
		 */
		Thread schedulerThread = new Thread(scheduler);
		schedulerThread.setDaemon(true);
		schedulerThread.start();

		try {
			// 子スレッドが終了したらKCLメインスレッドを正常に停止
			schedulerThread.join();
		} catch (InterruptedException e1) {
			// 例外を検知してもKCLメインスレッドを正常に停止
			log.error("SimKinesisConsumeApp.run - Main Thread InterruptedException Occur!");
		} finally {
			System.exit(0);
		}
	}

	/**
	 * KCLクライアントを安全に停止
	 * 
	 * @param s 起動中のスケジューラ
	 */
	private void stopMainThread(Scheduler s) {

		Future<Boolean> gracefulShutdownFuture = s.startGracefulShutdown();
		log.warn("SimKinesisConsumeApp.stopMainThread - Waiting up to 20 seconds for shutdown to complete.");
		try {
			gracefulShutdownFuture.get(20, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.error("SimKinesisConsumeApp.stopMainThread - Interrupted while waiting for graceful shutdown. Continuing.");
		} catch (ExecutionException e) {
			log.error("SimKinesisConsumeApp.stopMainThread - Exception while executing graceful shutdown.", e);
		} catch (TimeoutException e) {
			log.error("SimKinesisConsumeApp.stopMainThread - Timeout while waiting for shutdown.  Scheduler may not have exited.");
		}
		log.warn("SimKinesisConsumeApp.stopMainThread - Completed, shutting down now.");
	}

}
