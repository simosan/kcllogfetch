package com.simosan.kclapi.kcllogfetch.service;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.common.SimGetprop;

public class SimConnectionSwitch {

	private static final Logger log = LoggerFactory.getLogger(SimConnectionSwitch.class);
	public final CONNECTTYPE con;
	
	private enum CONNECTTYPE {
	    DIRECT,
	    ENDPOINTURI,
	    PROXY;
	}
	
	public SimConnectionSwitch(){
		/// インターネットダイレクトアクセスパターン
		if (SimGetprop.getProp("proxyhost").isEmpty() && SimGetprop.getProp("proxyport").isEmpty()
				&& SimGetprop.getProp("endpointuri").isEmpty()) {
			con = CONNECTTYPE.DIRECT;
        /// EndpointURI指定ありパターン
		} else if (SimGetprop.getProp("proxyhost").isEmpty() && SimGetprop.getProp("proxyport").isEmpty()
				&& !SimGetprop.getProp("endpointuri").isEmpty()) {
			con = CONNECTTYPE.ENDPOINTURI;
		/// プロキシ指定ありパターン
		} else if (!SimGetprop.getProp("proxyhost").isEmpty() && !SimGetprop.getProp("proxyport").isEmpty()
				&& SimGetprop.getProp("endpointuri").isEmpty()) {
			con = CONNECTTYPE.PROXY;
		} else {
			con = null;
		}
	}
	
	public String getConnectionType() throws Exception {
		
		if(Objects.isNull(con)) {
			log.error("SimConnectionSwitch - 接続方式のプロパティ指定（パターン）に誤りがあります");
			throw new Exception("SimConnectionSwitchエラー");
		}
		return con.toString();
	}
}
