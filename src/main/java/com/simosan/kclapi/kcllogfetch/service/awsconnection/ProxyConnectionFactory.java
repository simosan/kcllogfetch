package com.simosan.kclapi.kcllogfetch.service.awsconnection;

public class ProxyConnectionFactory implements ConnectionFactory {

	private final String proxyhost;
	private final String proxyport;

	public ProxyConnectionFactory(String proxyhost, String proxyport) {
		this.proxyhost = proxyhost;
		this.proxyport = proxyport;
	}
	
	@Override
	public ConnectionType createConnection() {
		return new ProxyConnection(proxyhost, proxyport);
	}

}
