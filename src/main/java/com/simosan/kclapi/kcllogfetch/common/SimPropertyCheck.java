package com.simosan.kclapi.kcllogfetch.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimPropertyCheck {
	
	private static final Logger log = LoggerFactory.getLogger(SimPropertyCheck.class);
	private enum CONNECTTYPE {
	    DIRECT,
	    ENDPOINTURI,
	    PROXY;
	}
	private CONNECTTYPE con;
	
	public boolean chkPropertyfile() {		
		//プロパティファイルのチェック - インターネットダイレクトアクセスパターン/EndpointURI指定ありパターン/プロキシ指定ありパターン
		
		///共通チェック
		if (SimGetprop.getProp("appname").isEmpty()) {
			log.error("SimPropertyCheck.chkPropertyfile - プロパティファイルのappnameが空です");
			return false;
		}
		if (SimGetprop.getProp("prof").isEmpty()) {
			log.error("SimPropertyCheck.chkPropertyfile - プロパティファイルのprofが空です");
			return false;
		}
		if (SimGetprop.getProp("region").isEmpty()) {
			log.error("SimPropertyCheck.chkPropertyfile - プロパティファイルのregionが空です");
			return false;
		}
		if (SimGetprop.getProp("streamname").isEmpty()) {
			log.error("SimPropertyCheck.chkPropertyfile - プロパティファイルのstreamnameが空です");
			return false;
		}
		if (SimGetprop.getProp("rolesesname").isEmpty()) {
			log.error("SimPropertyCheck.chkPropertyfile - プロパティファイルのrolesesnameが空です");
			return false;
		}
		if (SimGetprop.getProp("rolearn").isEmpty()) {
			log.error("SimPropertyCheck.chkPropertyfile - プロパティファイルのrolearnが空です");
			return false;
		}
		if (SimGetprop.getProp("postbname").isEmpty()) {
			log.error("SimPropertyCheck.chkPropertyfile - プロパティファイルのpostbnameが空です");
			return false;
		}
		if (SimGetprop.getProp("partitionkey").isEmpty()) {
			log.error("SimPropertyCheck.chkPropertyfile - プロパティファイルのpartitionkeyが空です");
			return false;
		}
		if (SimGetprop.getProp("dtpkey").isEmpty()) {
			log.error("SimPropertyCheck.chkPropertyfile - プロパティファイルのdtpkeyが空です");
			return false;
		}
		if (SimGetprop.getProp("partitionkey_value").isEmpty()) {
			log.error("SimPropertyCheck.chkPropertyfile - プロパティファイルのpartitionkey_valueが空です");
			return false;
		}
		if (SimGetprop.getProp("timezoneid").isEmpty()) {
			log.error("SimPropertyCheck.chkPropertyfile - プロパティファイルのtimezoneidが空です");
			return false;
		}
		
		/// インターネットダイレクトアクセスパターン
		if (SimGetprop.getProp("proxyhost").isEmpty() && SimGetprop.getProp("proxyport").isEmpty()
				&& SimGetprop.getProp("endpointuri").isEmpty()) {
			con = CONNECTTYPE.DIRECT;
		}
		
		/// EndpointURI指定ありパターン
		if (SimGetprop.getProp("proxyhost").isEmpty() && SimGetprop.getProp("proxyport").isEmpty()
				&& !SimGetprop.getProp("endpointuri").isEmpty()) {
			con = CONNECTTYPE.ENDPOINTURI;
		}
		
		/// プロキシ指定ありパターン
		if (!SimGetprop.getProp("proxyhost").isEmpty() && !SimGetprop.getProp("proxyport").isEmpty()
				&& SimGetprop.getProp("endpointuri").isEmpty()) {
			con = CONNECTTYPE.PROXY;
		}
		return true;
	}
	
	public String getConnectType() {
		return con.toString();
	}

}
