package com.simosan.kclapi.kcllogfetch.inflastructure.export;

public class SimKinesisDataExportlogFactory implements SimKinesisDataExportFactory {
	
	@Override
	public SimKinesisDataExport createDataExport() {
		return new SimKinesisDataExportlog();
	}
}
