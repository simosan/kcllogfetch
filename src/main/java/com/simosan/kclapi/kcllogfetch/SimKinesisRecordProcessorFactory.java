package com.simosan.kclapi.kcllogfetch;

import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

public class SimKinesisRecordProcessorFactory implements ShardRecordProcessorFactory{

	@Override
	public ShardRecordProcessor shardRecordProcessor() {
		return new SimKinesisRecordProcessor();
	}

}