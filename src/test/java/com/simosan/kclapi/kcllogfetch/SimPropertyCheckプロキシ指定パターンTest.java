package com.simosan.kclapi.kcllogfetch;

import static org.junit.Assert.assertEquals;
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

import com.simosan.kclapi.kcllogfetch.common.SimPropertyCheck;


public class SimPropertyCheckプロキシ指定パターンTest {
	private static final Logger log = LoggerFactory.getLogger(SimPropertyCheckプロキシ指定パターンTest.class);
	
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
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/SimPropertyCheck_PROXYパターン_正常系.properties");
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
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/SimPropertyCheck_PROXYパターン_正常系.properties");
		testmodosi.renameTo(netamodosi);
		
		//退避したプロパティファイルを戻す
		File seitaihi = new File(
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/kcllogfetch.properties.taihi");
		File seimodosi = new File(
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/kcllogfetch.properties");
		seitaihi.renameTo(seimodosi);
    }
	
	@Test
	public void SimPropertyCheck_プロキシ指定パターン_正常系() throws Exception{
		
        SimPropertyCheck spc = new SimPropertyCheck();
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
		assertEquals(spc.chkPropertyfile(),true);
		//assertThat(spc.getConnectType(),is("PROXY"));
				
	}
}
