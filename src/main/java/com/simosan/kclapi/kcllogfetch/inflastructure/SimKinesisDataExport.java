package com.simosan.kclapi.kcllogfetch.inflastructure;

public interface SimKinesisDataExport {
	public void dataExport(String pk, String seq, String data);
}
