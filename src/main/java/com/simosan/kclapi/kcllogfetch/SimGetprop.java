package com.simosan.kclapi.kcllogfetch;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SimGetprop {

	public Map<String, String> setProp(String path) {
		Properties properties = new Properties();

		try {
            InputStream istream = new FileInputStream(path);
            properties.load(istream);
        } catch (IOException e) {
            e.printStackTrace();
        }
		//プロパティファイルをMap（連想配列）に格納
		Map<String, String> propMap = new HashMap<>();
		for(Map.Entry<Object, Object> e : properties.entrySet()) {
			propMap.put(e.getKey().toString(), e.getValue().toString());
	    }
		return propMap;
	}

}
