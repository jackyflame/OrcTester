/*      						
 * Copyright 2010 Beijing Xinwei, Inc. All rights reserved.
 * 
 * History:
 * ------------------------------------------------------------------------------
 * Date    	|  Who  		|  What  
 * 2016-2-24	| duanbokan 	| 	create the file                       
 */

package vipo.haozi.orclib.utils;

import android.os.Environment;

import java.io.File;

/**
 * @author duanbokan
 */

public class TessConstantConfig {
	/**数据包的路径*/
	public static final String TESSBASE_BASE_PATH = "/Yunlaba/Orc/";

	/**数据包的路径 tessdata文件包 必须存在于这个目录下*/
	public static final String TESSBASE_TESSDATA_FOLDER = "tessdata/";

	/**数据包后缀名*/
	public static final String TESSBASE_TRANEDDATA_SUFFIX = ".traineddata";

	/**识别语言：英文（需要识别字库支持）*/
	public static final String DEFAULT_LANGUAGE_ENG = "eng";

	/**识别语言：数字字库（需要识别字库支持） 默认*/
	public static final String DEFAULT_LANGUAGE_NUM = "num";

	/**识别语言：简体中文（需要识别字库支持）*/
	public static final String DEFAULT_LANGUAGE_CHI = "chi_sim";

	/**识别字库文件名 数字字库 默认*/
	public static final String TESSBASE_NUM_FILENAME = DEFAULT_LANGUAGE_NUM + TESSBASE_TRANEDDATA_SUFFIX;

	public static String getTessDataFilePath() {
		return getTessDataFileDirectory() + TESSBASE_NUM_FILENAME;
	}

	public static String getTessDataFileDirectory() {
		return getSDPath() + TESSBASE_BASE_PATH + TESSBASE_TESSDATA_FOLDER;
	}

	public static String getTessDataDirectory() {
		return getSDPath() + TESSBASE_BASE_PATH;
	}

	public static String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals("mounted");
		if(sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();
		} else {
			sdDir = Environment.getRootDirectory();
		}

		return sdDir.toString();
	}
}
