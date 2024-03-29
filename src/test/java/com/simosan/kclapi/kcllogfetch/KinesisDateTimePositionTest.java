package com.simosan.kclapi.kcllogfetch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.service.awsconnection.EndpointUriConnection;
import com.simosan.kclapi.kcllogfetch.common.SimGetprop;
import com.simosan.kclapi.kcllogfetch.service.KinesisDateTimePosition;

public class KinesisDateTimePositionTest {

	private static final Logger log = LoggerFactory.getLogger(KinesisDateTimePositionTest.class);
	private final String endpointuri = "http://simubu:4566";

	@Before
	public void setUp() throws Exception {
		//正のプロパティファイルを退避
		File moto = new File(
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/kcllogfetch.properties");
		File taihi = new File(
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/kcllogfetch.properties.taihi");
		moto.renameTo(taihi);
		
		// 本テストのプロパティファイルを正にする
		File neta = new File(
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/SimPropertyCheck_EndpointURIパターン_正常系.properties");
		File sei = new File(
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/kcllogfetch.properties");
		neta.renameTo(sei);
		
		String cmd1 = "/opt/homebrew/bin/aws dynamodb create-table "
	            + "--table-name SimKinesisConsumeAppDatePosTbl "
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
		
		// 本テストのプロパティファイルを戻す
		File testmodosi = new File(
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/kcllogfetch.properties");
		File netamodosi = new File(
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/SimPropertyCheck_EndpointURIパターン_正常系.properties");
		testmodosi.renameTo(netamodosi);
		
		//退避したプロパティファイルを戻す
		File seitaihi = new File(
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/kcllogfetch.properties.taihi");
		File seimodosi = new File(
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/kcllogfetch.properties");
		seitaihi.renameTo(seimodosi);
	}

	@Test
	public void retrieveTimeposition_EndpointURIパターン_正常系() throws Exception {
		EndpointUriConnection euc = new EndpointUriConnection(endpointuri);
		KinesisDateTimePosition skdtpfd = new KinesisDateTimePosition(euc.retriveDynamoClient(),SimGetprop.getProp("postbname"));
		log.info("=================================================");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date hikakudt = sdf.parse("2022-01-01T11:00:00");
		assertThat(skdtpfd.retrieveTimeposition(), is(hikakudt));
		log.info("====retrieveTimeposition_EndpointURIパターン_正常系====");
		System.out.println(skdtpfd.retrieveTimeposition());
	}
	
	@Test
	public void updateTimestampItem_EndpointURIパターン_正常系() throws Exception {
		EndpointUriConnection euc = new EndpointUriConnection(endpointuri);
		KinesisDateTimePosition skdtpfd = new KinesisDateTimePosition(euc.retriveDynamoClient(),SimGetprop.getProp("postbname"));
		log.info("=================================================");
		skdtpfd.updateTimestampItem("DataGroupKey", "kimoemoji", "timeposition");
		assertThat(skdtpfd.retrieveTimeposition(), notNullValue());
		log.info("====updateTimestampItem_EndpointURIパターン_正常系====");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		String strDate = sdf.format(skdtpfd.retrieveTimeposition());
		log.info(strDate);
	}

}
