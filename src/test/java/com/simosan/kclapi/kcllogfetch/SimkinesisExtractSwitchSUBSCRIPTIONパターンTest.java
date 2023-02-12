package com.simosan.kclapi.kcllogfetch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.domain.SimkinesisExtractSwitch;

public class SimkinesisExtractSwitchSUBSCRIPTIONパターンTest {
	
	private static final Logger log = LoggerFactory.getLogger(SimkinesisExtractSwitchSUBSCRIPTIONパターンTest.class);
	
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
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/bk/kcllogfetch.properties.aws");
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
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/bk/kcllogfetch.properties.aws");
		testmodosi.renameTo(netamodosi);
		
		//退避したプロパティファイルを戻す
		File seitaihi = new File(
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/kcllogfetch.properties.taihi");
		File seimodosi = new File(
				"/Users/sim/Documents/eclipse_workspace/kcllogfetch/etc/kcllogfetch.properties");
		seitaihi.renameTo(seimodosi);
    }
	
	@Test
	public void SimkinesisExtractSwitchSUBSCRIPTIONパターン_正常系() throws Exception{
		SimkinesisExtractSwitch skes = new SimkinesisExtractSwitch();
		log.info("=================================================");
		System.out.println(skes.getExtractType());
		assertThat(skes.getExtractType(),is("SUBSCRIPTION"));
	}

}
