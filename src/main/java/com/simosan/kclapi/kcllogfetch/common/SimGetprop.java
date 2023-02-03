package com.simosan.kclapi.kcllogfetch.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 指定されたキーに対して、プロパティファイルを都度読み込み、値を取得する
 * 都度ファイルの読み込みを行うため、ループ処理内で本staticメソッドは呼び出さないこと 初期化処理など、ワンショットの処理だけで利用すること
 */
/*
 * @author sim
 * 
 */
public class SimGetprop {

	private static final Logger log = LoggerFactory.getLogger(SimGetprop.class);

	/**
	 * @param key プロパティファイルのキー
	 * @return キーに紐づく値
	 */
	public static String getProp(String key) {

		String path = null;
		String val = null;
		Properties properties = new Properties();

		// 実行時引数に指定したプロパティファイル(-Dproppath=絶対パス)を読み込む
		path = System.getProperty("proppath");
		try {
			InputStream istream = new FileInputStream(path);
			properties.load(istream);
		} catch (IOException e) {
			log.error("SimGetprop.getProp - プロパティファイルが読み込めません！", e);
			System.exit(255);
		}
		// プロパティファイルから指定したキーに対する値を取得
		val = properties.getProperty(key);
		if (val == null) {
			log.error("SimGetprop.getProp - 指定したキーに対する値が設定されていません。キー：" + key);
			System.exit(255);
		}
		return val;
	}
}
