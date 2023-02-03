package com.simosan.kclapi.kcllogfetch.inflastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimKinesisDataExportlog implements SimKinesisDataExport {

	private static final Logger log = LoggerFactory.getLogger(SimKinesisDataExportlog.class);
	
	@Override
	public void dataExport(String pk, String seq, String data) {
		log.info("pk: {},Seq: {},Data: {}",pk, seq, data);	
	}

}
