package com.simosan.kclapi.kcllogfetch;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

public class SimAssumeRoleCred {
	
	private Region Rg;
	private AssumeRoleResponse response;

    private static final Logger log = LoggerFactory.getLogger(SimAssumeRoleCred.class);
	
	/**
     * IAMロールで実行できるようにするため、AssumeRoleをロード
     */
    public AwsCredentialsProvider loadCredentials(SdkAsyncHttpClient cl)  {
    	final AwsCredentialsProvider credentialsProvider;
    	
    	ProfileCredentialsProvider devProfile = ProfileCredentialsProvider.builder()
                .profileName(SimGetprop.getProp("prof"))
                .build();
    	
        Rg = Region.of(SimGetprop.getProp("region"));
        
    	StsAsyncClient stsAsyncClient = StsAsyncClient.builder()
                .credentialsProvider(devProfile)
                .region(Rg)
                .httpClient(cl)
                .build();
    	
    	AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .durationSeconds(3600)
                .roleArn(SimGetprop.getProp("rolearn"))
                .roleSessionName(SimGetprop.getProp("rolesesname"))
                .build();
    	
    	Future<AssumeRoleResponse> responseFuture = stsAsyncClient.assumeRole(assumeRoleRequest);
		try {
			response = responseFuture.get();
		} catch (InterruptedException e) {
			log.error("AssumeRole InterruptedException!", e);
			System.exit(255);
		} catch (ExecutionException e) {
			log.error("AssumeRole ExecutionException!", e);
			System.exit(255);
		}
		
        Credentials credentials = response.credentials();
        AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
        		credentials.accessKeyId(),
        		credentials.secretAccessKey(),
        		credentials.sessionToken());
        
        credentialsProvider =  AwsCredentialsProviderChain.builder()
                .credentialsProviders(StaticCredentialsProvider.create(sessionCredentials))
                .build();
        
        return credentialsProvider;
    	
    }
}
