package com.simosan.kclapi.kcllogfetch.domain;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.common.SimGetprop;

public class SimkinesisExtractSwitch {

	private static final Logger log = LoggerFactory.getLogger(SimkinesisExtractSwitch.class);
	public final EXTRACTTYPE ext;
	
	private enum EXTRACTTYPE {
	    NORM,
	    SUBSCRIPTION;
	}
	
	public SimkinesisExtractSwitch(){
		/// 非圧縮の通常データ
		if (SimGetprop.getProp("extracttype").equals(EXTRACTTYPE.NORM.toString())) {
			ext = EXTRACTTYPE.NORM;
        /// Cloudwatchlogsから抽出するサブスクリプションフィルタログ
		} else if (SimGetprop.getProp("extracttype").equals(EXTRACTTYPE.SUBSCRIPTION.toString())) {
			ext = EXTRACTTYPE.SUBSCRIPTION;
		} else {
			ext = null;
		}
	}
	
	public String getExtractType() throws Exception {
		
		if(Objects.isNull(ext)) {
			log.error("SimkinesisExtractSwitch - Kineis抽出指定に誤りがあります");
			throw new Exception("SimkinesisExtractSwitchエラー");
		}
		return ext.toString();
	}
}
