package com.simosan.kclapi.kcllogfetch.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.domain.extract.SimKinesisExtractDataUnprocessedFactory;
import com.simosan.kclapi.kcllogfetch.domain.extract.SimKinesisExtractSubscriptionFactory;
import com.simosan.kclapi.kcllogfetch.domain.extract.SimkinesisExtractData;
import com.simosan.kclapi.kcllogfetch.domain.extract.SimkinesisExtractDataFactory;

public class SimKinesisExtractManageService {
	private static final Logger log = LoggerFactory.getLogger(SimKinesisExtractManageService.class);
	private String ext;
	private  SimkinesisExtractDataFactory ef;
	private  SimkinesisExtractData ed;
	
	public  SimkinesisExtractData retriveExtract() {
		
		SimkinesisExtractSwitch ses = new SimkinesisExtractSwitch();
		try {
			ext = ses.getExtractType();
		} catch (Exception e) {
			log.error("SimKinesisExtractManageService.retriveExtract - プロパティファイルエラーをキャッチしました");
			System.exit(255);
		}
		
		switch(ext) {
		case "NORM":
			ef = new SimKinesisExtractDataUnprocessedFactory();
			ed = ef.createExtractdata();
			break;
		case "SUBSCRIPTION":
			ef = new SimKinesisExtractSubscriptionFactory();
			ed = ef.createExtractdata();
			break;
		}

		return ed;
	}
}
