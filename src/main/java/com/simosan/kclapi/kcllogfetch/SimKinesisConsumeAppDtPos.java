package com.simosan.kclapi.kcllogfetch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/** Kinesis Streamsで読み込んだポジション（位置情報）を保存・読み取る
 *  これ本当はSingletonクラスにしたほうがいいかあらためてシーケンスみたほうがいい。
 *  本プログラムで利用しているKCLはバージョン2.xを利用している。1.xとライブラリの互換性は一切ないため注意。
 *  本プログラム（KCL）を動作させるためには、Kinesis、DynamoDB、CloudwatchのIAMを
 *  事前セットアップしておく必要がある。詳しくは以下URL参照。
 *  https://docs.aws.amazon.com/ja_jp/streams/latest/dev/tutorial-stock-data-kplkcl-iam.html
 */
/* @author sim
 */

public class SimKinesisConsumeAppDtPos
{

	private static final Logger log = LoggerFactory.getLogger(SimKinesisConsumeAppDtPos.class);
	//以下３つの初期化情報とクレデンシャル（id、key）はプロパティファイルから取得
	// tbNameはDynamoDBのテーブル名になる
	private String tbName;
	private DynamoDbAsyncClient dynamoClient;


	/**
	 * AWS認証のセットアップ
	 * そのほかテーブル名など初期化
	 * @param creprofile　awsクレデンシャル 
	 * @param region　リージョン情報
	 * @param tbn　テーブル名
	 * @param cl　プロキシクライアント
	 */
	public SimKinesisConsumeAppDtPos(AwsCredentialsProvider cred, Region region, String tbn, SdkAsyncHttpClient cl)
	{
		//DynamoDBクライアントの初期化（assumeroleロード、リージョンセット）
		this.dynamoClient = DynamoDbAsyncClient.builder()
				.region(region)
				.credentialsProvider(cred)
				.httpClient(cl)
				.build();

		this.tbName = tbn;
	}

	/**
	 * DynamoDBへの項目（タイムスタンプ）を取得
	 * @param pk　パーティショーンキー名
	 * @param pk_v　pkキーの値：ロググループ名
	 * @param k　属性のキー：タイムスタンプキー名
	 * @return timestline　最新のタイムポジションを返却
	 */
	public String getTimestampItem(String pk, String pk_v, String k) {
		String timestline = null;

		HashMap<String,AttributeValue> key_to_get =
				new HashMap<String,AttributeValue>();

		key_to_get.put(pk, AttributeValue.builder()
				.s(pk_v).build());

		try {
			GetItemRequest request = GetItemRequest.builder()
					.key(key_to_get)
					.tableName(this.tbName)
					.build();

			Map<String,AttributeValue> returned_item = dynamoClient.getItem(request)
					.get()
					.item();

			Set<String> keys = returned_item.keySet();
			for (String key : keys) {
				if(key.equals(k)) {
					timestline = returned_item.get(k).s();
				}
			}        	

		} catch (ResourceNotFoundException e) {
			// テーブルがない場合は更新（新規作成）するのでここで停止しない
			log.warn("SimKinesisConsumeAppDtPos: DynamoDBテーブルから情報を取得できませんでした", e.getCause());
		} catch (InterruptedException e) {
			log.warn("SimKinesisConsumeAppDtPos: DynamoDBテーブル情報取得時に割り込み例外が発生しました", e.getCause());
		} catch (ExecutionException e) {
			log.warn("SimKinesisConsumeAppDtPos: DynamoDBテーブル情報取得中に実行時例外発生", e.getCause());
		}

		return timestline;
	}

	/**
	 * DynamoDBへの項目（タイムスタンプ）を更新。対象項目（キーも）がなければ新規作成
	 * @param pk（パーティショーンキー）
	 * @param pk_v（pkキーの値：ロググループ名）
	 * @param k（属性のキー：タイムスタンプキー名)
	 */
	public void updateTimestampItem(String pk, String pk_v, String k) {

		//最新のタイムスタンプ（UTC)を取得
		String dt = getUtcDt();
		if ( dt == null) {
			log.error("SimKinesisConsumeAppDtPos: 時刻が取得できませんでした。");
			System.exit(255);
		}

		HashMap<String,AttributeValue> item_key =
				new HashMap<String,AttributeValue>();

		item_key.put(pk, AttributeValue.builder()
				.s(pk_v).build());

		HashMap<String,AttributeValueUpdate> updated_values =
				new HashMap<String,AttributeValueUpdate>();

		try {
			updated_values.put(k, AttributeValueUpdate.builder()
					.value(AttributeValue.builder().s(dt).build())
					.action(AttributeAction.PUT)
					.build());

			UpdateItemRequest request = null;
			request = UpdateItemRequest.builder()
					.tableName(tbName)
					.key(item_key)
					.attributeUpdates(updated_values)
					.build();

			dynamoClient.updateItem(request);
		} catch (ResourceNotFoundException e) {
			log.error("SimKinesisConsumeAppDtPos: 更新対象のテーブルが存在しません",e);
			System.exit(255);
		} catch (DynamoDbException e) {
			log.warn("SimKinesisConsumeAppDtPos: DynamoDBにてなんらかのエラーが発生しました。処理は継続します。",e.getMessage());
		}

	}

	/**
	 * 時刻を取得する。フォーマットは"yyyy-MM-dd'T'HH:mm:ss"
	 * Kinesisの時刻はUTC(-9)なのでそれで返す。
	 * @return 時刻を返却。String型で返す。
	 */
	protected String getUtcDt() {

		String dtstr = null;

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		Date dt = new Date();
		dtstr = sdf.format(dt).toString();

		return dtstr;
	}

}
