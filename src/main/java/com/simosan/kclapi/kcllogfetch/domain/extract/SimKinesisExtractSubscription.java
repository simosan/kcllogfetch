package com.simosan.kclapi.kcllogfetch.domain.extract;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SimKinesisExtractSubscription implements SimkinesisExtractData {

	private static final Logger log = LoggerFactory.getLogger(SimKinesisExtractSubscription.class);

	@Override
	public List<String> extractDataFromJson(final ByteBuffer dt) {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root;
		List<String> strmessage = new ArrayList<String>();

		String jsondt = getBufferzipData(dt);
		//System.out.println(jsondt);
		try {
			root = mapper.readTree(jsondt);
			// ロググループ名取得
			JsonNode loggrp = root.get("logGroup");
			for (JsonNode n : root.get("logEvents")) {
				strmessage.add(loggrp + " - " + n.get("message").asText());
			}
		} catch (IOException e) {
			log.error("SimKinesisExtractSubscription: Json Parse Error!", e);
		}
		List<String> unmodstrmessage = Collections.unmodifiableList(strmessage);
		return unmodstrmessage;
	}

	/**
	 * Kinesisに溜め込んだデータバッファを取得し、JSONデータの"Message"のみ取得する。 データバッファは圧縮されているため解凍する必要あり。
	 * 
	 * @param KinesisClientRecordのdata
	 */
	private String getBufferzipData(ByteBuffer d) {
		String strline = null;
		String message = new String();
		byte[] arr = new byte[d.remaining()];
		d.get(arr);
		GZIPInputStream gis;
		try {
			gis = new GZIPInputStream(new ByteArrayInputStream(arr));
			BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "UTF-8"));

			while ((strline = bf.readLine()) != null) {
				message += strline;
			}

		} catch (IOException e) {
			log.error("SimKinesisExtractSubscription BufferError!", e);
		}
		return message;
	}

}
