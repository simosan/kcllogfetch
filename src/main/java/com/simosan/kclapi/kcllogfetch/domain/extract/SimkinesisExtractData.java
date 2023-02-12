package com.simosan.kclapi.kcllogfetch.domain.extract;

import java.nio.ByteBuffer;
import java.util.List;

public interface SimkinesisExtractData {
	public List<String> extractDataFromJson(final ByteBuffer dt);
}
