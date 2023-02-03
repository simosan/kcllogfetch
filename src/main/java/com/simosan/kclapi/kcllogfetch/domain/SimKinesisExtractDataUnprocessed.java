package com.simosan.kclapi.kcllogfetch.domain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimKinesisExtractDataUnprocessed implements SimkinesisExtractData {

	private static final Logger log = LoggerFactory.getLogger(SimKinesisExtractDataUnprocessed.class);

	@Override
	public String extractDataFromJson(final ByteBuffer dt) {

		String strmessage = "";

		try {
			strmessage = StandardCharsets.UTF_8.decode(dt).toString();
		} catch (Exception e) {
			log.error(
					"SimKinesisExtractDataUnprocessed.extractDataFromJson - KinesisClientRecord-SimKinesisExtractDataUnprocessed Excetpion!",
					e);
		}
		return strmessage;
	}

}
