package com.simosan.kclapi.kcllogfetch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
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
    private DynamoDbClient client;


    /**
     * AWS認証のセットアップ
     * そのほかテーブル名など初期化
     * @param creprofile（awsプロファイルネーム）
     * @param tbn（テーブル名）
     */
    public SimKinesisConsumeAppDtPos(AwsCredentialsProvider cred, Region region, String tbn)
    {
    	//DynamoDBクライアントの初期化（assumeroleロード、リージョンセット）
    	this.client = DynamoDbClient.builder()
    			.region(region)
    			.credentialsProvider(cred)
    			.build();

        this.tbName = tbn;
    }

    /**
     * DynamoDBへの項目（タイムスタンプ）を取得
     * @param pk（パーティショーンキー名)
     * @param pk_v（pkキーの値：ロググループ名）
     * @param k（属性のキー：タイムスタンプキー名)
     * @return timestline（最新のタイムポジションを返却）
     */
    public String getTimestampItem(String pk, String pk_v, String k) throws ResourceNotFoundException,SdkClientException {
    	String timestline = null;

    	HashMap<String,AttributeValue> key_to_get =
                new HashMap<String,AttributeValue>();
        key_to_get.put(pk, AttributeValue.builder()
        		.s(pk_v).build());
        GetItemRequest request = null;
        request = GetItemRequest.builder()
                .key(key_to_get)
                .tableName(this.tbName)
                .build();

        Map<String,AttributeValue> returned_item =
        		this.client.getItem(request).item();
        Set<String> keys = returned_item.keySet();
        for (String key : keys) {
        	if(key.equals(k)) {
        		timestline = returned_item.get(k).s();
        	}
        }

        return timestline;
    }

    /**
     * DynamoDBへの項目（タイムスタンプ）を更新。対象項目（キーも）がなければ新規作成
     * @param pk（パーティショーンキー）
     * @param pk_v（pkキーの値：ロググループ名）
     * @param k（属性のキー：タイムスタンプキー名)
     */
    public void updateTimestampItem(String pk, String pk_v, String k) throws ResourceNotFoundException {

    	//最新のタイムスタンプ（UTC)を取得
    	String dt = null;
    	try {
    	   dt = getUtcDt();
    	}catch(Exception e) {
    		log.error("updateTimestampItem：getUtcDt_Error! " + e.getMessage());
    	}

    	HashMap<String,AttributeValue> item_key =
                new HashMap<String,AttributeValue>();
       	item_key.put(pk, AttributeValue.builder()
       			.s(pk_v).build());
       	HashMap<String,AttributeValueUpdate> updated_values =
                new HashMap<String,AttributeValueUpdate>();
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

        client.updateItem(request);
    }
    /**
     * 時刻を取得する。フォーマットは"yyyy-MM-dd'T'HH:mm:ss"
     * Kinesisの時刻はUTC(-9)なのでそれで返す。
     * @return 時刻を返却。String型で返す。
     */
	protected String getUtcDt() throws Exception {

    	String dtstr = null;

    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    	sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

    	Date dt = new Date();
    	dtstr = sdf.format(dt).toString();

    	return dtstr;
    }

}
