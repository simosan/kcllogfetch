package com.simosan.kclapi.kcllogfetch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.common.SimGetprop;
import com.simosan.kclapi.kcllogfetch.service.SimAssumeRoleCred;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.net.URI;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimAssumeRoleCredTest {
	
	private static final Logger log = LoggerFactory.getLogger(SimAssumeRoleCredTest.class);
	
	// 事前にKinesisStreamを作成する
	@BeforeClass
	public static void setUp() {
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
		
		String cmd = "/opt/homebrew/bin/aws kinesis create-stream --stream-name simkpltest --shard-count 1 --profile localstack --endpoint-url http://simubu:4566";
		try {
			log.info("Streamを作成します");
			Runtime runtime = Runtime.getRuntime();
			//@SuppressWarnings("deprecation")
			Process p = runtime.exec(cmd);
			p.waitFor();
			p.destroy();
			//ストリームの作成に時間がかかるのでスリープ（awsコマンドでステータスみてもいいけどめんどくさいのでそこまでやらん）
			Thread.sleep(10000);
		} catch (Exception e) {
			log.error("Streamの作成に失敗しました!!!!");
			e.printStackTrace();
		}
	}
	//テスト終わったらKinesisStream消す
	@AfterClass
	public static void tearDown() {
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
		
		String cmd = "/opt/homebrew/bin/aws kinesis delete-stream --enforce-consumer-deletion --stream-name simkpltest --profile localstack --endpoint-url http://simubu:4566";
		try {
			log.info("Streamを削除します");
			Runtime runtime = Runtime.getRuntime();
			//@SuppressWarnings("deprecation")
			Process p = runtime.exec(cmd);
			p.waitFor();
			p.destroy();
			Thread.sleep(10000);
		} catch (Exception e) {
			log.error("Streamの削除に失敗しました!!!!");
			e.printStackTrace();
		}
	}
	
	@Test
	public void SimAssumeRoleCred_EndpointURI指定_正常系() throws Exception{
		SimAssumeRoleCred sarc = new SimAssumeRoleCred(URI.create(SimGetprop.getProp("endpointuri")));
		log.info("=================================================");
		assertThat(sarc.loadCredentials(),is(instanceOf(AwsCredentialsProvider.class)));
	}
}

