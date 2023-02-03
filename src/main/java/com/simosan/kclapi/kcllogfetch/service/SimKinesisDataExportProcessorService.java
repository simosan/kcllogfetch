package com.simosan.kclapi.kcllogfetch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.common.SimGetprop;
import com.simosan.kclapi.kcllogfetch.domain.SimkinesisExtractData;
import com.simosan.kclapi.kcllogfetch.inflastructure.SimKinesisDataExport;

import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;

public class SimKinesisDataExportProcessorService {

	private static final Logger log = LoggerFactory.getLogger(SimKinesisDataExportProcessorService.class);
	private final SimkinesisExtractData ed;
	private final SimKinesisDataExport de;
	private final SimKinesisDateTimePositionFromDynamodb skdtfd;

	public SimKinesisDataExportProcessorService(SimkinesisExtractData sked, SimKinesisDataExport skde,
			SimKinesisDateTimePositionFromDynamodb db) {
		this.ed = sked;
		this.de = skde;
		this.skdtfd = db;
	}

	public void dataProcessor(ProcessRecordsInput processRecordsInput) {

		log.warn("SimKinesisDataExportProcessorService.dataProcessor - Processing {} record(s)",
				processRecordsInput.records().size());
		try {
			processRecordsInput.records().forEach(
					r -> de.dataExport(r.partitionKey(), r.sequenceNumber(), ed.extractDataFromJson(r.data())));
		} catch (Exception e) {
			log.error(
					"SimKinesisDataExportProcessorService.dataProcessor - Caught throwable while processing records. Aborting.",
					e);
			Runtime.getRuntime().halt(1);
		}

		// ログ取得→書き込み先への出力が完了したら最新時刻でDynamoDBのテーブルを更新する。
		skdtfd.updateTimestampItem(SimGetprop.getProp("partitionkey"), SimGetprop.getProp("partitionkey_value"),
				SimGetprop.getProp("dtpkey"));
	}
}
