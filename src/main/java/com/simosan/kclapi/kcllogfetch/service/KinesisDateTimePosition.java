package com.simosan.kclapi.kcllogfetch.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.common.SimGetprop;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class KinesisDateTimePosition {

	private static final Logger log = LoggerFactory.getLogger(KinesisDateTimePosition.class);
	private final String tbName;
	private DynamoDbAsyncClient dynamoClient;

	public KinesisDateTimePosition(final DynamoDbAsyncClient dynamoClient, final String tbName) {

		this.dynamoClient = dynamoClient;
		this.tbName = tbName;

	}

	// 起動時はDynamoDBのSimKinesisConsumeAppDateTimePosテーブルから存在する最新のタイムスタンプを取得
	// タイムスタンプを遡りたいのであれば、DynamoDBのタイムスタンプを直接いじること。
	// スケジューラが起動したらそれ以降のポジショニングはKCLがいい感じにしてくれる
	public Date retrieveTimeposition() {

		String tmstr = this.getTimestampItem(SimGetprop.getProp("partitionkey"),
				SimGetprop.getProp("partitionkey_value"), SimGetprop.getProp("dtpkey"));
		// DynamoDBのdtpが空だったら最新のタイムスタンプに更新
		if (tmstr == null || tmstr.isEmpty()) {
			this.updateTimestampItem(SimGetprop.getProp("partitionkey"), SimGetprop.getProp("partitionkey_value"),
					SimGetprop.getProp("dtpkey"));
			tmstr = this.getTimestampItem(SimGetprop.getProp("partitionkey"), SimGetprop.getProp("partitionkey_value"),
					SimGetprop.getProp("dtpkey"));
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

		Date date = null;
		try {
			date = sdf.parse(tmstr);
		} catch (ParseException e) {
			log.error("SimKinesisDateTimePositionFromDynamodb.retrieveTimeposition - 時刻のフォーマットがおかしいです。", e);
			System.exit(255);
		}
		return date;
	}

	/**
	 * DynamoDBへの項目（タイムスタンプ）を取得
	 * 
	 * @param pk   パーティショーンキー名
	 * @param pk_v pkキーの値：ロググループ名
	 * @param k    属性のキー：タイムスタンプキー名
	 * @return timestline 最新のタイムポジションを返却
	 */
	private String getTimestampItem(String pk, String pk_v, String k) {
		String timestline = null;

		HashMap<String, AttributeValue> key_to_get = new HashMap<String, AttributeValue>();

		key_to_get.put(pk, AttributeValue.builder().s(pk_v).build());

		try {
			GetItemRequest request = GetItemRequest.builder().key(key_to_get).tableName(this.tbName).build();

			Map<String, AttributeValue> returned_item = dynamoClient.getItem(request).get().item();

			Set<String> keys = returned_item.keySet();
			for (String key : keys) {
				if (key.equals(k)) {
					timestline = returned_item.get(k).s();
				}
			}

		} catch (ResourceNotFoundException e) {
			// テーブルがない場合は更新（新規作成）するのでここで停止しない
			log.warn("SimKinesisConsumeAppDtPos.getTimestampItem - DynamoDBテーブルから情報を取得できませんでした", e.getCause());
		} catch (InterruptedException e) {
			log.warn("SimKinesisConsumeAppDtPos.getTimestampItem -  DynamoDBテーブル情報取得時に割り込み例外が発生しました", e.getCause());
		} catch (ExecutionException e) {
			log.warn("SimKinesisConsumeAppDtPos.getTimestampItem -  DynamoDBテーブル情報取得中に実行時例外発生", e.getCause());
		}

		return timestline;
	}

	/**
	 * DynamoDBへの項目（タイムスタンプ）を更新。対象項目（キーも）がなければ新規作成
	 * 
	 * @param pk（パーティショーンキー）
	 * @param pk_v（pkキーの値：ロググループ名）
	 * @param k（属性のキー：タイムスタンプキー名)
	 */
	public void updateTimestampItem(String pk, String pk_v, String k) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		String timezone = SimGetprop.getProp("timezoneid");
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
		String dt = formatter.format(now);

		HashMap<String, AttributeValue> item_key = new HashMap<String, AttributeValue>();

		item_key.put(pk, AttributeValue.builder().s(pk_v).build());

		HashMap<String, AttributeValueUpdate> updated_values = new HashMap<String, AttributeValueUpdate>();

		try {
			updated_values.put(k, AttributeValueUpdate.builder().value(AttributeValue.builder().s(dt).build())
					.action(AttributeAction.PUT).build());

			UpdateItemRequest request = null;
			request = UpdateItemRequest.builder().tableName(tbName).key(item_key).attributeUpdates(updated_values)
					.build();

			dynamoClient.updateItem(request);
		} catch (ResourceNotFoundException e) {
			log.error("SimKinesisConsumeAppDtPos.updateTimestampItem - 更新対象のテーブルが存在しません", e);
			System.exit(255);
		} catch (DynamoDbException e) {
			log.warn("SimKinesisConsumeAppDtPos.updateTimestampItem - DynamoDBにてなんらかのエラーが発生しました。処理は継続します。",
					e.getMessage());
		}

	}
}
