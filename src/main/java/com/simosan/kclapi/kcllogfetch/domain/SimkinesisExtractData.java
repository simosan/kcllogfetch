package com.simosan.kclapi.kcllogfetch.domain;

import java.nio.ByteBuffer;

public interface SimkinesisExtractData {
	public String extractDataFromJson(final ByteBuffer dt);
}
