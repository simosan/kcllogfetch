package com.simosan.kclapi.kcllogfetch.inflastructure.export;

import java.util.List;

public interface SimKinesisDataExport {
	public void dataExport(String pk, String seq, List<String> data);
}
