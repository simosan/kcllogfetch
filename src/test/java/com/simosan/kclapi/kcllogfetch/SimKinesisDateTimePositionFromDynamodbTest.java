package com.simosan.kclapi.kcllogfetch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.service.SimAwsClientManageService;
import com.simosan.kclapi.kcllogfetch.service.SimKinesisDateTimePositionFromDynamodb;

public class SimKinesisDateTimePositionFromDynamodbTest {

	private static final Logger log = LoggerFactory.getLogger(SimKinesisDateTimePositionFromDynamodbTest.class);
	private final String endpointuri = "http://simubu:4566";

	@Before
	public void setUp() throws Exception {
		String cmd1 = "/opt/homebrew/bin/aws dynamodb create-table "
	            + "--table-name SimKinesisConsumeAppDateTimePos "
				+ "--attribute-definitions AttributeName=LogGroupKey,AttributeType=S "
				+ "--key-schema AttributeName=LogGroupKey,KeyType=HASH "
				+ "--provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 "
				+ "--profile localstack "
				+ "--endpoint-url http://simubu:4566";
		String cmd2 = "/bin/bash -c "
				+ "/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/SimKinesisDateTimePositionFromDynamodb_Insert.sh";
		
		try {
			log.info("DynamoDBテーブルを作成します");
			Runtime runtime = Runtime.getRuntime();
			Process p = runtime.exec(cmd1);
			p.waitFor();
			p.destroy();
		} catch (Exception e) {
			log.error("DynamoDBテーブルの作成 or データ挿入に失敗しました!!!!");
			e.printStackTrace();
		}

		try {
			log.info("SimKinesisConsumeAppDatePosTblテーブルにデータを挿入します");
			Runtime runtime2 = Runtime.getRuntime();
			Process p2 = runtime2.exec(cmd2);
			p2.waitFor();
			p2.destroy();
		} catch (Exception e) {
			log.error("DynamoDBテーブルの作成 or データ挿入に失敗しました!!!!");
			e.printStackTrace();
		}

	}

	@After
	public void tearDown() throws Exception {
		String cmd = "/opt/homebrew/bin/aws dynamodb delete-table " + "--table-name SimKinesisConsumeAppDatePosTbl "
				+ "--profile localstack " + "--endpoint-url http://simubu:4566";
		try {
			log.info("DynamoDBテーブルを削除します");
			Runtime runtime = Runtime.getRuntime();
			Process p = runtime.exec(cmd);
			p.waitFor();
			p.destroy();
		} catch (Exception e) {
			log.error("DynamoDBテーブルの削除に失敗しました!!!!");
			e.printStackTrace();
		}
	}

	@Test
	public void retrieveTimeposition_EndpointURIパターン_正常系() throws Exception {
		SimAwsClientManageService sacms = new SimAwsClientManageService(endpointuri);
		SimKinesisDateTimePositionFromDynamodb skdtpfd = new SimKinesisDateTimePositionFromDynamodb(
				sacms.retriveCredentialProvider(), sacms.retriveRegion(), endpointuri);
		log.info("=================================================");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date hikakudt = sdf.parse("2022-01-01T11:00:00");
		assertThat(skdtpfd.retrieveTimeposition(), is(hikakudt));
		log.info("====retrieveTimeposition_EndpointURIパターン_正常系====");
		System.out.println(skdtpfd.retrieveTimeposition());
	}
	
	@Test
	public void updateTimestampItem_EndpointURIパターン_正常系() throws Exception {
		SimAwsClientManageService sacms = new SimAwsClientManageService(endpointuri);
		SimKinesisDateTimePositionFromDynamodb skdtpfd = new SimKinesisDateTimePositionFromDynamodb(
				sacms.retriveCredentialProvider(), sacms.retriveRegion(), endpointuri);
		log.info("=================================================");
		skdtpfd.updateTimestampItem("DataGroupKey", "kimoemoji", "timeposition");
		assertThat(skdtpfd.retrieveTimeposition(), notNullValue());
		log.info("====updateTimestampItem_EndpointURIパターン_正常系====");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		String strDate = sdf.format(skdtpfd.retrieveTimeposition());
		log.info(strDate);
	}

}
