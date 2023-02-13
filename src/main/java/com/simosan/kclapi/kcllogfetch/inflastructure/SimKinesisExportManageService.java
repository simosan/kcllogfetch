package com.simosan.kclapi.kcllogfetch.inflastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.inflastructure.export.SimKinesisDataExport;
import com.simosan.kclapi.kcllogfetch.inflastructure.export.SimKinesisDataExportlogFactory;

public class SimKinesisExportManageService {
	private static final Logger log = LoggerFactory.getLogger(SimKinesisExportManageService.class);
	private String exp;
	private  SimKinesisDataExportlogFactory ef;
	private  SimKinesisDataExport de;
	
	public  SimKinesisDataExport retriveExport() {
		
		SimkinesisExportSwitch ses = new SimkinesisExportSwitch();
		try {
			exp = ses.getExportType();
		} catch (Exception e) {
			log.error("SimKinesisExportManageService.retriveExport - プロパティファイルエラーをキャッチしました");
			System.exit(255);
		}
		
		switch(exp) {
		case "LOG":
			ef = new SimKinesisDataExportlogFactory();
			de = ef.createDataExport();
			break;
		}
		return de;		
	}
}
