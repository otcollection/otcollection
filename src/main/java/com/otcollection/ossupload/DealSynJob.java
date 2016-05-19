package com.otcollection.ossupload;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.aliyun.oss.OSSClient;
import com.otcollection.*;


public class DealSynJob {
	private static final Logger log = Logger.getLogger(DealSynJob.class);
    private OSSOperation ossSVC;
    private FileOperation fileSVC;
    private OSSConfigure conf;
    
	
	
	public void doJob(){
		if (log.isInfoEnabled()) {
			log.info("Enter method doJob-------------------");
		}
		      
	   
		String path = conf.getPath();
		List<String> fileNameList = new ArrayList<String>();
		fileNameList = fileSVC.listFile(new File(path), fileNameList);
		fileNameList = fileSVC.fileNameFilter(fileNameList);//过滤文件
		if (fileNameList.size() > 0) {
			//检查是否有满足冷却时间的文件
			fileNameList = fileSVC.cdTimeIsEnough(fileNameList);
			if (fileNameList.size() > 0) {
				if (fileSVC.isFileExist(fileNameList)) {
					try {
						this.uploadFile(fileNameList);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("上传完成!");
				} else {
					System.out.println("符合冷却时间条件的文件已经全部上传完成!");
				}
			} else {
				System.out.println("未找到满足冷却时间的文件!");
			}
		} else {
			System.out.println("指定路径没有检测到文件,请检查路径!");
		}

	}
	
	@SuppressWarnings("unchecked")
	public void uploadFile(List<String> fileNameList) throws IOException{
		
		if (log.isDebugEnabled()) {
			log.debug("Enter method uploadFile-------------fileNameList is "+fileNameList);
		}
		OSSClient client = ossSVC.createOSSClient();
		
		String bucketName = conf.getBucketName();
		
		String key = new String();
		Long partSize = 1024 * 1024 * 1L;// 阿里规定partSize 不能小于100kb
		if (partSize < 1024 * 100L) {
			partSize = 1024 * 100L;
		}
		Map<String, String> urlResult = new HashMap<String, String>();		
		for (String fileName : fileNameList) {
			FileWriter fw = new FileWriter(conf.getOssUrlFileName(), true);
			File file = new File(fileName);

			//System.err.println(file.toURI().toString());
			key = file.toURI().toString();
			// unix 下文件路径开头为"/",阿里不允许objectnName开头为"/"
			if (key.startsWith(File.separator)) {
				key = key.substring(1, fileName.length());
			} 			
			// ossSVC.multipartUploadObject(bucketName, key, newFile(fileName),partSize);
			urlResult = ossSVC.upLoadFile(bucketName, key, fileName, partSize);
			String eTag = client.getObject(bucketName, key).getObjectMetadata().getETag();
			if (urlResult.get("url") == null) {
				if (urlResult.get("isExist").equals("0")) {
					fw.write(fileName + "------This is a directory.\r\n");
				}

			} else {
				String localMd5 = (fileSVC.getFileMd5(file, partSize));
				if (localMd5.equals(eTag)) {
					// 暂定不删除目录，对上传成功的文件做删除
					//System.err.println("DEL file:"+file.getAbsolutePath());
					if(log.isInfoEnabled()){
						log.info("已将文件上传到阿里云，计算本地Md5值为:"+localMd5+" 经过对比与阿里云一致，上传成功开始删除本地文件!");
					}
					file.delete();
					Boolean ret;
					if(!conf.getUpdateUrlSVCImpl().isEmpty()&&conf.getUpdateUrlSVCImpl()!=null){
						//如果实现扩展接口，调用接口
						 UpdateUrlSVC updateUrlSVC = (UpdateUrlSVC)Application.getInstance().getContext().getBean("updateUrlSVC");
						 ret = updateUrlSVC.updateUrl(key, urlResult.get("url"));
					}else{
						//如果未实现扩展接口，调用配置文件中给出的service
						 ret = ossSVC.updateUrl(key, urlResult.get("url"));
					}
					
					if(ret){
						if (log.isInfoEnabled())
							log.info("调用接口更新url成功! 文件名："+key+",新的url为"+urlResult.get("url"));
						fw.write("上传成功！调用远端服务成功！文件名："+fileName + "------" + urlResult.get("url") + "\r\n\r\n");
					}
					else{
						log.warn("上传成功！调用远端服务失败，请检查远端服务是否可用！文件名："+key+",新的url为"+urlResult.get("url"));
						fw.write("上传成功！调用远端服务失败，请检查远端服务是否可用！文件名：" + fileName+"------" + urlResult.get("url") + "\r\n\r\n");
					}
					//System.err.println("END DEL file:"+file.getAbsolutePath());
					
				} else {
					client.deleteObject(bucketName, key);
					log.warn("上传的文件与本地文件不一致，重新上传!");
				}
			}
			fw.close();
		}
	}

	public OSSOperation getOssSVC() {
		return ossSVC;
	}

	public void setOssSVC(OSSOperation ossSVC) {
		this.ossSVC = ossSVC;
	}

	public FileOperation getFileSVC() {
		return fileSVC;
	}

	public void setFileSVC(FileOperation fileSVC) {
		this.fileSVC = fileSVC;
	}

	public OSSConfigure getConf() {
		return conf;
	}

	public void setConf(OSSConfigure conf) {
		this.conf = conf;
	}
	
	
	
}
