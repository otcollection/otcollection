package com.otcollection.ossupload;

import static com.aliyun.oss.common.utils.IOUtils.newRepeatableInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.aliyun.oss.model.UploadPartRequest;

public class FileOperation {

	private static final Logger log = Logger.getLogger(FileOperation.class);
	private OSSConfigure conf;
	
	// 获得文件夹下所有文件的名字
	public List<String> listFile(File file, List<String> resultFileName) {

		File[] files = file.listFiles();
		if (files == null)
			return resultFileName;// 判断目录下是不是空的
		for (File f : files) {
			if (!f.isHidden()) {// 不读取隐藏文件
				if (f.isDirectory()) {// 判断是否文件夹

					resultFileName.add(f.getPath());
					listFile(f, resultFileName);// 调用自身,查找子目录
				} else {
					resultFileName.add(f.getPath());
				}
			}
		}
		return resultFileName;
	}

	public void deleteAll(File file) {
		if (file.isFile() || file.list().length == 0) {
			file.delete();
		} else {
			File[] files = file.listFiles();
			for (File f : files) {
				deleteAll(f);// 递归删除每一个文件
				f.delete();// 删除该文件夹
			}
		}
	}

	public boolean isFileExist(List<String> fileNameList) {
		if (log.isDebugEnabled()) {
			log.debug("enter method isFileExist----------------fileNameList is "+fileNameList);
		}
		
		for (String fileName : fileNameList) {
			if (new File(fileName).isFile()) {
				return true;
			}
		}
		return false;
	}
	
	public List<String> fileNameFilter(List<String> fileNameList){
		if (log.isDebugEnabled()) {
			log.debug("enter method fileNameFilter----------------fileNameList is "+fileNameList);
		}
		List<String> newfileNameList = new ArrayList<String>();
		for(String fileName:fileNameList){
			if(!fileName.endsWith(".ucp")){
				newfileNameList.add(fileName);
			}
		}
		return newfileNameList;
	}
	
	@SuppressWarnings("rawtypes")
	public List<String> cdTimeIsEnough(List<String> fileNameList){
		if (log.isDebugEnabled()) {
			log.debug("enter method cdTimeIsEnough----------------fileNameList is "+fileNameList);
		}
		List<String> newfileNameList = new ArrayList<String>();
		String ss = conf.getTimeLength();
		String safeTimeLength = conf.getSafeTimeLength();
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(ss);
		long cdTime = 1;
		while (m.find()) {
			cdTime *= Long.parseLong(m.group());
		}
		Matcher m1 = p.matcher(safeTimeLength);
		int safeTime = 1;
		while (m1.find()) {
			safeTime *= Long.parseLong(m1.group());
		}
		for (String fileName : fileNameList) {
			if (new File(fileName).isFile()) {
				Map result = this.getFileDate(fileName);
				if (result.get("result").equals("true")) {
					Date date = (Date) result.get("date");
					if (new Date().getTime() - date.getTime() > cdTime && new Date().getTime() - date.getTime()< safeTime) {
						newfileNameList.add(fileName);
					}
				}
			}
		}
		return newfileNameList;
	}

	public String getFileMd5(File file, Long partSize) throws IOException {

		String md5 = new String();

		// int partSize = 1024 * 1024 * 1;

		// 计算分块数目
		int partCount = (int) (file.length() / partSize);
		if (file.length() % partSize != 0) {
			partCount++;
		}

		for (int i = 0; i < partCount; i++) {
			// 获取文件流
			FileInputStream fis;
			fis = new FileInputStream(file);
			// 跳到每个分块的开头
			long skipBytes = partSize * i;
			fis.skip(skipBytes);

			// 计算每个分块的大小
			long size = partSize < file.length() - skipBytes ? partSize : file.length() - skipBytes;

			// 调用oss的分块方法
			UploadPartRequest uploadPartRequest = new UploadPartRequest();
			uploadPartRequest.setInputStream(fis);
			uploadPartRequest.setPartSize(size);
			InputStream repeatableInputStream = newRepeatableInputStream(uploadPartRequest.buildPartialStream());
			// 计算所有分块的md5和再做md5加密，得到的值与oss返回的eTag比较
			md5 += DigestUtils.md5Hex(repeatableInputStream);
			repeatableInputStream.close();
			fis.close();
		}
           
		return this.md5ToETag(DigestUtils.md5Hex(md5.toUpperCase()), partCount);

	}
	
	public String md5ToETag(String md5, int partCount) {
		
		return (md5.toUpperCase() + '-' + partCount);
		
	}
	
	@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
	public Map getFileDate(String path) {

		// path = "D:/mpeg4/年(2016)/月(1)/日(1)/xxxx/xxxxx";
		
		String regex = conf.getRegex();
		Map result = new HashMap();
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(path);
		int year = 0;
		int month = 0;
		int day = 0;
		if (m.find()) {
			year = Integer.parseInt(m.group(1).replaceFirst("^0*", ""));
		}
		if (m.find()) {
			month = Integer.parseInt(m.group(1).replaceFirst("^0*", ""));
		}
		if (m.find()) {
			day = Integer.parseInt(m.group(1).replaceFirst("^0*", ""));
		}
		if (year != 0 && month != 0 && day != 0) {
			result.put("date", new Date(year - 1900, month - 1, day));
			result.put("result", "true");
			
		} else {
			log.error("解析路径时间失败，请检查文件路径是否正确或者正则表达式是否正确! "+"path:"+path+" regex:"+regex);
			result.put("result", "false");
		}
		return result;
	}

	public String myChange(Matcher m, String date) {
		if (m.find()) {
			String xx = m.group();
			Pattern p1 = Pattern.compile("\\d+");
			Matcher m1 = p1.matcher(xx);
			if (m1.find()) {
				date = m1.group();
			}

		}
		else{
			log.info("解析文件路径出错，请查看文件路径!");
		}
		return date;
	}

	public OSSConfigure getConf() {
		return conf;
	}

	public void setConf(OSSConfigure conf) {
		this.conf = conf;
	}
 
	
}
