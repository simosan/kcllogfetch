package com.simosan.kclapi.kcllogfetch.service.awsconnection;

public class InternetDirectConnectionFactory implements ConnectionFactory {

	@Override
	public ConnectionType createConnection() {
		return new InternetDirectConnection();
	}

}
