package com.simosan.kclapi.kcllogfetch.service.awsconnection;

public class EndpointUriConnectionFactory implements ConnectionFactory {

	private final String endpointuri;

	public EndpointUriConnectionFactory(final String endpointuri) {
		this.endpointuri = endpointuri;
	}
	
	@Override
	public ConnectionType createConnection() {
		return new EndpointUriConnection(endpointuri);
	}

}
