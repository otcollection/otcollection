package com.otcollection.ossupload;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;  
  
public class OSSConfigure {  
  
    private String endpoint;  
    private String accessKeyId;  
    private String accessKeySecret;  
    private String bucketName;  
    private String path;
    private String timeLength;
    private String ossUrlFileName;
    private String service;
    private String updateUrlSVCImpl;
    private String regex;
    private String lockFile;
    private String safeTimeLength;
    private String mode;
  
    public OSSConfigure() {  
  
    }  
  
    /** 
     * 通过配置文件.properties文件获取，这几项内容。 
     *  
     * @param storageConfName 
     * @throws IOException 
     */  
    public OSSConfigure(String storageConfName) {  
  
        Properties prop = new Properties();  
        try {
			prop.load(new InputStreamReader(this.getClass().getClassLoader()  
			        .getResourceAsStream("conf/" + storageConfName),"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}  
  
        endpoint = prop.getProperty("endpoint").trim();  
        accessKeyId = prop.getProperty("accessKeyId").trim();  
        accessKeySecret = prop.getProperty("accessKeySecret").trim();  
        bucketName = prop.getProperty("bucketName").trim();  
        path = prop.getProperty("path").trim();  
        timeLength = prop.getProperty("timeLength").trim();
        ossUrlFileName = prop.getProperty("ossUrlFileName").trim();
        service = prop.getProperty("service").trim();
        updateUrlSVCImpl = prop.getProperty("updateUrlSVCImpl").trim();
        regex = prop.getProperty("regex").trim();
        lockFile = prop.getProperty("lockFile").trim();
        safeTimeLength = prop.getProperty("safeTimeLength").trim();
        mode = prop.getProperty("mode").trim();

    }  
  
    public OSSConfigure(String endpoint, String accessKeyId,  
            String accessKeySecret, String bucketName, String path, String timeLength, 
            String ossUrlFileName,String service,String updateUrlSVCImpl,String regex,String lockFile,String mode) {  
  
        this.endpoint = endpoint;  
        this.accessKeyId = accessKeyId;  
        this.accessKeySecret = accessKeySecret;  
        this.bucketName = bucketName;  
        this.path = path;  
        this.timeLength = timeLength;
        this.ossUrlFileName = ossUrlFileName;
        this.service = service;
        this.updateUrlSVCImpl = updateUrlSVCImpl;
        this.regex = regex;
        this.lockFile = lockFile;
        this.mode = mode;
    }  

  
    public String getEndpoint() {  
        return endpoint;  
    }  
  
    public void setEndpoint(String endpoint) {  
        this.endpoint = endpoint;  
    }  
  
    public String getAccessKeyId() {  
        return accessKeyId;  
    }  
  
    public void setAccessKeyId(String accessKeyId) {  
        this.accessKeyId = accessKeyId;  
    }  
  
    public String getAccessKeySecret() {  
        return accessKeySecret;  
    }  
  
    public void setAccessKeySecret(String accessKeySecret) {  
        this.accessKeySecret = accessKeySecret;  
    }  
  
    public String getBucketName() {  
        return bucketName;  
    }  
  
    public void setBucketName(String bucketName) {  
        this.bucketName = bucketName;  
    }

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getTimeLength() {
		return timeLength;
	}

	public void setTimeLength(String timeLength) {
		this.timeLength = timeLength;
	}

	

	public String getOssUrlFileName() {
		return ossUrlFileName;
	}

	public void setOssUrlFileName(String ossUrlFileName) {
		this.ossUrlFileName = ossUrlFileName;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getUpdateUrlSVCImpl() {
		return updateUrlSVCImpl;
	}

	public void setUpdateUrlSVCImpl(String updateUrlSVCImpl) {
		this.updateUrlSVCImpl = updateUrlSVCImpl;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public String getLockFile() {
		return lockFile;
	}

	public void setLockFile(String lockFile) {
		this.lockFile = lockFile;
	}

	public String getSafeTimeLength() {
		return safeTimeLength;
	}

	public void setSafeTimeLength(String safeTimeLength) {
		this.safeTimeLength = safeTimeLength;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}  
  
	
    
  
}  
