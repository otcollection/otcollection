package com.otcollection;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.otcollection.ossupload.OSSConfigure;

public class ServiceLauncher {
	private static final Logger log = Logger.getLogger(ServiceLauncher.class);
    
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		Calendar cal = Calendar.getInstance();
		TimeZone timeZone = cal.getTimeZone();
		log.info("TimeZoneId:"+timeZone.getID());
		log.info("TimeZoneDisp:"+timeZone.getDisplayName());
		log.info("OSS 上传工具，载入的配置文件为[" + Application.SERVICE_PROVIDER_XML + "]");
		
		OSSConfigure conf = (OSSConfigure)Application.getInstance().getContext().getBean("configure");
		String lckPath = conf.getLockFile();
		if (lckPath != null && !lckPath.isEmpty()) {
			Application.getInstance().getContext().start();
			try {
				FileLock lck;
				lck = new FileOutputStream(lckPath).getChannel().tryLock();
				if (lck == null) {
					log.info("实例已经在运行....");
					log.info("退出此次启动 ...");
					System.exit(1);
				}

			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			log.info("启动完毕，请查看日志");
			while (true) {
				try {
					Thread.currentThread();
					Thread.sleep(30000L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}else{
			throw new NullPointerException("请填写配置文件中的锁文件路径来保证线程安全！");
		}
	}
}
