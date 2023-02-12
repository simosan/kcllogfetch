package com.simosan.kclapi.kcllogfetch.domain.extract;

public class SimKinesisExtractSubscriptionFactory implements SimkinesisExtractDataFactory {

	@Override
	public SimkinesisExtractData createExtractdata() {
		return new SimKinesisExtractSubscription();
	}
}
