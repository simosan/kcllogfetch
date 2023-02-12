package com.simosan.kclapi.kcllogfetch.domain.extract;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimKinesisExtractDataUnprocessed implements SimkinesisExtractData {

	private static final Logger log = LoggerFactory.getLogger(SimKinesisExtractDataUnprocessed.class);

	@Override
	public List<String> extractDataFromJson(final ByteBuffer dt) {

		List<String> strmessage = new ArrayList<String>();
		try {
			strmessage.add(StandardCharsets.UTF_8.decode(dt).toString());
		} catch (Exception e) {
			log.error(
					"SimKinesisExtractDataUnprocessed.extractDataFromJson - KinesisClientRecord-SimKinesisExtractDataUnprocessed Excetpion!",
					e);
		}
		return strmessage;
	}
}