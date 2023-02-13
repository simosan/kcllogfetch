package com.simosan.kclapi.kcllogfetch.inflastructure;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.common.SimGetprop;

public class SimkinesisExportSwitch {

	private static final Logger log = LoggerFactory.getLogger(SimkinesisExportSwitch.class);
	public final EXTRACTTYPE ext;
	
	private enum EXTRACTTYPE {
	    LOG;
	}
	
	public SimkinesisExportSwitch(){
		/// ログファイル（Logback）出力
		if (SimGetprop.getProp("exporttype").equals(EXTRACTTYPE.LOG.toString())) {
			ext = EXTRACTTYPE.LOG;
		} else {
			ext = null;
		}
	}
	
	public String getExportType() throws Exception {
		
		if(Objects.isNull(ext)) {
			log.error("SimkinesisExportSwitch - Kineis出力指定に誤りがあります");
			throw new Exception("SimkinesisExportSwitchエラー");
		}
		return ext.toString();
	}
}
