package com.simosan.kclapi.kcllogfetch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simosan.kclapi.kcllogfetch.common.SimGetprop;
import com.simosan.kclapi.kcllogfetch.service.awsconnection.ConnectionFactory;
import com.simosan.kclapi.kcllogfetch.service.awsconnection.ConnectionType;
import com.simosan.kclapi.kcllogfetch.service.awsconnection.EndpointUriConnectionFactory;
import com.simosan.kclapi.kcllogfetch.service.awsconnection.InternetDirectConnectionFactory;
import com.simosan.kclapi.kcllogfetch.service.awsconnection.ProxyConnectionFactory;

public class SimAwsConnectionManageService {
	private static final Logger log = LoggerFactory.getLogger(SimAwsConnectionManageService.class);
	private String con;
	private ConnectionFactory cf;
	private ConnectionType ct;
	
	public  ConnectionType retriveConnection() {
		
		SimConnectionSwitch scs = new SimConnectionSwitch();
		try {
			con = scs.getConnectionType();
		} catch (Exception e) {
			log.error("SimAwsConnectionManageService.retriveConnection - プロパティファイルエラーをキャッチしました");
			System.exit(255);
		}
		
		switch(con) {
		case "DIRECT":
			cf = new InternetDirectConnectionFactory();
			ct = cf.createConnection();
			break;
		case "ENDPOINTURI":
			cf = new EndpointUriConnectionFactory(SimGetprop.getProp("endpointuri"));
			ct = cf.createConnection();
			break;
		case "PROXY":
			cf = new ProxyConnectionFactory(SimGetprop.getProp("proxyhost"),SimGetprop.getProp("proxyport"));
			ct = cf.createConnection();
		}

		return ct;
	}
}
