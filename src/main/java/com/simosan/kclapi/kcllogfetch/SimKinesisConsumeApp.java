package com.simosan.kclapi.kcllogfetch;

/**
 * Kinesisコンシューマーのワーカー
 *
 */
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.coordinator.Scheduler;

//これ用に作った独自ライブラリ（DynamoDBからタイムスタンプ取得する）
import com.simosan.dynamodb.dynamoope.SimKinesisConsumeAppDtPos;

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
    //以下３つの初期化情報とクレデンシャル（id、key）はプロパティファイルから取得
    private String streamName;
    // appNameはDynamoDBのテーブル名になる
    private String appName;
    private Region region;
    private String prof;
    private String tbName;
    private String pk;
    private String pkv;
    private String k;


    private KinesisAsyncClient kinesisClient;
    private DynamoDbAsyncClient dynamoClient;
    private CloudWatchAsyncClient cloudWatchClient;

    /**
     * クレデンシャルやKinesisストリーム等,KCL実行に必要な情報を初期化
     */
    private SimKinesisConsumeApp(Map<String,String> simmap) {
    	// クレデンシャル（ID,シークレットキー）
    	System.setProperty("aws.accessKeyId", simmap.get("awsaccesskeyid"));
    	System.setProperty("aws.secretAccessKey", simmap.get("awssecretaccesskey"));
    	// ストリーム名
        this.streamName = simmap.get("streamname");
        // 本体名
        this.appName = simmap.get("appname");
        // awsアカウントプロファイル、SimKinesisRecordProcessor用にシステムプロパティにも格納
        this.prof = simmap.get("prof");
        System.setProperty("prof", this.prof);
        // DynamoDBのポジショニング（どこまで読んだか）テーブル名
        this.tbName = simmap.get("postbname");
        System.setProperty("postbname", this.tbName);
        // DynamoDBのポジショニング用テーブルのパーティションキー
        this.pk = simmap.get("partitionkey");
        System.setProperty("partitionkey", this.pk);
        // DynamoDBのポジショニング用テーブルのパーティションキーの値
        this.pkv = simmap.get("partitionkey_value");
        System.setProperty("partitionkey_value", this.pkv);
        // DynamoDBのポジショニング用テーブルのタイムスタンプのキー
        this.k = simmap.get("dtpkey");
        System.setProperty("dtpkey", this.k);

        // リージョン名（注意）書き方は東京リージョンの場合、ap-northeast-1にすること
        // v1.xのマニュアルではAP_NORTHEAST_1となっているが、V2.xではエラーになる
        this.region = Region.of(simmap.get("region"));
        // kinesisクライアント
        this.kinesisClient = KinesisAsyncClient.builder().region(region).build();
        // Dynamoクライアント（kinesis設定をDynamoに格納）
        this.dynamoClient = DynamoDbAsyncClient.builder().region(region).build();
        // Cloudwatchクライアント（kinesisの状態をCloudwatchに出力）
        this.cloudWatchClient = CloudWatchAsyncClient.builder().region(region).build();
    }

    /**
     * メインメソッド
     * 第１引数にストリーム名を指定、第２引数にAWSリージョンを指定
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            log.error("引数が足りません。第1引数にプロパティファイル名（絶対パス）を指定");
            System.exit(1);
        }

        String proppath = args[0];
        log.info("MainThread Start!");
        //プロパティファイルを読み込んでMapに格納
        SimGetprop sp = new SimGetprop();
        Map<String,String> mp = sp.setProp(proppath);
        new SimKinesisConsumeApp(mp).run();
    }

    /**
     * KCLのセットアップ。DynamoDB（チェックポイント格納）、Cloudwatch（KCL監視）セットアップも含む。
     * SimKinesisRecordProcessorFactoryはSimKinesisRecordProcessorFactoryを実装したレコードプロセッサー生成クラス
     */
    private void run() {

    	Date date = null;

        ConfigsBuilder configsBuilder = new ConfigsBuilder(
        					streamName,
        					appName,
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
        SimKinesisConsumeAppDtPos skcadt = new SimKinesisConsumeAppDtPos(prof, tbName);
        String tmstr = null;
        try {
            tmstr = skcadt.getTimestampItem(pk, pkv, k);
            // DynamoDBのdtpが空だったら最新のタイムスタンプに更新
            if (tmstr == null)
            {
                skcadt.updateTimestampItem(
            		    System.getProperty("partitionkey"),
            		    System.getProperty("partitionkey_value"),
            		    System.getProperty("dtpkey"));
                tmstr = skcadt.getTimestampItem(pk, pkv, k);
            }
        } catch(DynamoDbException e) {
            // テーブルがないケース。あとでCreateTableも作っとく
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