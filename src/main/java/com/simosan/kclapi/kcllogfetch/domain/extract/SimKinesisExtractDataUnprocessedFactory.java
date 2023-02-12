package com.simosan.kclapi.kcllogfetch.domain.extract;

public class SimKinesisExtractDataUnprocessedFactory implements SimkinesisExtractDataFactory {

	@Override
	public SimkinesisExtractData createExtractdata() {
		return new SimKinesisExtractDataUnprocessed();
	}
}
