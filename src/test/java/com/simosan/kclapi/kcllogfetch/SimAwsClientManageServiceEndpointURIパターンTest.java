package com.simosan.kclapi.kcllogfetch;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.common.SimGetprop;
import com.simosan.kclapi.kcllogfetch.service.SimAwsClientManageService;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.kinesis.common.ConfigsBuilder;


public class SimAwsClientManageServiceEndpointURIパターンTest {
	private static final Logger log = LoggerFactory.getLogger(SimAwsClientManageServiceEndpointURIパターンTest.class);
	
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
    }
	
	@After
    public void tearDown() throws Exception {
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
	public void SimAwsClientManageService_EndpointURIパターン_正常系() throws Exception{
		
        Properties properties = new Properties();
        String path = System.getProperty("proppath");
		try {
			InputStream istream = new FileInputStream(path);
			properties.load(istream);
		} catch (IOException e) {
			log.error("プロパティファイルが読み込めません！", e);
			System.exit(255);
		}
		log.info("=================================================");
		SimAwsClientManageService sacms = new SimAwsClientManageService(SimGetprop.getProp("endpointuri"));
		sacms.retriveconfigsBuilder();
		sacms.retriveCredentialProvider();
		sacms.retriveRegion();
		assertThat(sacms.retriveconfigsBuilder(),is(instanceOf(ConfigsBuilder.class)));
		assertThat(sacms.retriveCredentialProvider(),is(instanceOf(AwsCredentialsProvider.class)));
		assertThat(sacms.retriveRegion(),is(instanceOf(Region.class)));
				
	}
}
