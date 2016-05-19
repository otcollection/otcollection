package com.otcollection.ossupload;



import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ListMultipartUploadsRequest;
import com.aliyun.oss.model.ListPartsRequest;
import com.aliyun.oss.model.MultipartUpload;
import com.aliyun.oss.model.MultipartUploadListing;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PartListing;
import com.aliyun.oss.model.PartSummary;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.UploadFileRequest;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;

public class OSSOperation {

	private static final Logger log = Logger.getLogger(OSSOperation.class);
	private FileOperation fileSVC;
	private OSSConfigure conf;
	
	//小文件直接上传方法
	public void putObject(String bucketName, String fileName) {
		if (log.isInfoEnabled()) {
			log.info("enter method putObject,bucketName is" + bucketName + "fileNameList is" + fileName);
		}

		OSSClient client = this.createOSSClient();
		String key;
		// 文件名如果带路径，以"/"开头需要去掉，oss不支持以"/"开头的object名
		if (fileName.startsWith("/")) {
			key = fileName.substring(1, fileName.length());
		} else {
			key = fileName;
		}
		File file = new File(fileName);
		PutObjectRequest putObjectRequest;
		if (file.isDirectory()) {
			// 如果是文件夹直接建立
			putObjectRequest = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(new byte[0]));
		} else {
			// 如果是文件的话需获取文件的md5值，oss服务端会对文件meta信息中的md5值进行校验，相同才会上传成功
//			ObjectMetadata meta = new ObjectMetadata();
//			meta.setContentMD5(fileSVC.getFileMd5(file));
			putObjectRequest = new PutObjectRequest(bucketName, key, file);
		}
		try {
			client.putObject(putObjectRequest);
			// client.generatePresignedUrl(request)
		} catch (OSSException oe) {
			System.out.println("Caught an OSSException, which means your request made it to OSS, "
					+ "but was rejected with an error response for some reason.");
			System.out.println("Error Message: " + oe.getErrorCode());
			System.out.println("Error Code:       " + oe.getErrorCode());
			System.out.println("Request ID:      " + oe.getRequestId());
			System.out.println("Host ID:           " + oe.getHostId());
		} catch (ClientException ce) {
			System.out.println("Caught an ClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with OSS, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ce.getMessage());
		} finally {
			client.shutdown();
		}

	}

	// 大文件分块上传，支持断点续传
	@SuppressWarnings("unused")
	public List<PartETag> multipartUploadObject(String bucketName, String key, File partFile ,Long partSize) {
		String tag = null;
		String uploadid = null;
		int j = 0;
		if (key.startsWith("/")) {
			key = key.substring(1, key.length());
		} 
		OSSClient client = this.createOSSClient();
		if (partFile.isDirectory()) {
			// 如果是文件夹直接建立
			PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(new byte[0]));
			client.putObject(putObjectRequest);
			return null;
		} 				
		
		ListMultipartUploadsRequest lmur = new ListMultipartUploadsRequest(bucketName);

		// 获取Bucket内所有上传事件
		MultipartUploadListing listing = client.listMultipartUploads(lmur);
		// 新建一个List保存每个分块上传后的ETag和PartNumber
		List<PartETag> partETags = new ArrayList<PartETag>();

		// 遍历所有上传事件 设置UploadId
		for (MultipartUpload multipartUpload : listing.getMultipartUploads()) {
			if (multipartUpload.getKey().equals(key)) {
				uploadid = multipartUpload.getUploadId();
				break;
			}
		}

		if (uploadid == null) {
			// 开始Multipart Upload,InitiateMultipartUploadRequest
			// 来指定上传Object的名字和所属Bucket
			InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(
					bucketName, key);
			InitiateMultipartUploadResult initiateMultipartUploadResult = client
					.initiateMultipartUpload(initiateMultipartUploadRequest);
			uploadid = initiateMultipartUploadResult.getUploadId();
		} else {
			ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, key, uploadid);
			// listParts 方法获取某个上传事件所有已上传的块
			PartListing partListing = client.listParts(listPartsRequest);
			// 遍历所有Part
			for (PartSummary part : partListing.getParts()) {
				partETags.add(new PartETag(part.getPartNumber(), part.getETag()));
				j++;
			}
		}
		// 设置每块为 1M
		//partSize = 1024 * 1024 * 1L;

		// 计算分块数目
		int partCount = (int) (partFile.length() / partSize);
		if (partFile.length() % partSize != 0) {
			partCount++;
		}
		try {
			for (int i = j; i < partCount; i++) {
				// 获取文件流
				FileInputStream fis;
				fis = new FileInputStream(partFile);

				// 跳到每个分块的开头
				long skipBytes = partSize * i;
				fis.skip(skipBytes);

				// 计算每个分块的大小
				long size = partSize < partFile.length() - skipBytes ? partSize : partFile.length() - skipBytes;

				// 创建UploadPartRequest，上传分块
				UploadPartRequest uploadPartRequest = new UploadPartRequest();
				uploadPartRequest.setBucketName(bucketName);
				uploadPartRequest.setKey(key);
				uploadPartRequest.setUploadId(uploadid);
				uploadPartRequest.setInputStream(fis);
				uploadPartRequest.setPartSize(size);
				uploadPartRequest.setPartNumber(i + 1);
//				InputStream repeatableInputStream = newRepeatableInputStream(uploadPartRequest.buildPartialStream());
//				uploadPartRequest.setMd5Digest(DigestUtils.md5Hex(repeatableInputStream));
				UploadPartResult uploadPartResult = client.uploadPart(uploadPartRequest);

				// 将返回的PartETag保存到List中
				partETags.add(uploadPartResult.getPartETag());
                
				System.err.println(uploadPartResult.getPartETag());
				// 关闭文件
				fis.close();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(bucketName,
				key, uploadid, partETags);
		// 完成分块上传
		CompleteMultipartUploadResult completeMultipartUploadResult = client
				.completeMultipartUpload(completeMultipartUploadRequest);
		// 打印Object的ETag（返回的ETag不是md5）
		tag = completeMultipartUploadResult.getETag();
		return partETags;
	}
	
	public OSSClient createOSSClient(){
		String endpoint = conf.getEndpoint();
		String accessKeyId = conf.getAccessKeyId();
		String accessKeySecret = conf.getAccessKeySecret();
		OSSClient client = new OSSClient(endpoint, accessKeyId, accessKeySecret);
		return client;
	}
	
	@SuppressWarnings("rawtypes")
	public Map upLoadFile(String bucketName, String key, String path, Long partSize) {
		
		if (log.isInfoEnabled()) {
			log.info("开始上传文件，文件名为：" + key);
		}
		OSSClient client = this.createOSSClient();
		File file = new File(path);
		Map<String,String> resultMap = new HashMap<String,String>();
//		if (key.startsWith("/")) {
//			key = key.substring(1, key.length());
//		} 
		key = file.toURI().toString();
		if (file.isDirectory()) {
			// 如果是文件夹直接建立
		    //	client.getObject(bucketName, key);
			if (!client.doesObjectExist(bucketName, key)) {
				PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key,
						new ByteArrayInputStream(new byte[0]));
				client.putObject(putObjectRequest);
				resultMap.put("isExist", "0");
			}else{
				resultMap.put("isExist", "1");
			}
			resultMap.put("url", null);
			return resultMap;
		} 
		
	//	ObjectMetadata objectMetadata = new ObjectMetadata();
		//objectMetadata.setContentMD5("123");
		UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
	//	uploadFileRequest.setObjectMetadata(objectMetadata);
		uploadFileRequest.setUploadFile(path);
		uploadFileRequest.setTaskNum(10);
		uploadFileRequest.setPartSize(partSize);
		uploadFileRequest.setEnableCheckpoint(true);

		
		try {
			client.uploadFile(uploadFileRequest);
		} catch (Throwable e) {
			
			e.printStackTrace();
		}
		String url = new String();
		try {
			client.setObjectAcl(bucketName, key, CannedAccessControlList.parse("public-read"));
			url = "http://"+bucketName+"."+conf.getEndpoint()+"/"+URLEncoder.encode(key, "UTF-8");
			url = url.replaceAll("%2F", "/");
			url = url.replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	
		resultMap.put("url", url);
		return resultMap;
	}
	
	@SuppressWarnings({ "resource", "deprecation" })
	public boolean updateUrl(String fileName,String newUrl) throws ParseException, UnsupportedEncodingException, IOException {

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(20000)
                .setConnectTimeout(20000)
                .setConnectionRequestTimeout(20000)
                .build();
        String url = conf.getService();
		if (url != null && !url.isEmpty()) {
			String serviceUrl = url.substring(0, url.indexOf("?"));
			String param1 = url.substring(url.indexOf("?") + 1, url.indexOf("="));
			String param2 = url.substring(url.indexOf("&") + 1, url.lastIndexOf("="));
			HttpClient client = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(serviceUrl);
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair(param1, fileName));
			params.add(new BasicNameValuePair(param2, newUrl));
			httpget.setConfig(requestConfig);

			String str = EntityUtils.toString(new UrlEncodedFormEntity(params));
			try {
				httpget.setURI(new URI(httpget.getURI().toString() + "?" + str));
			} catch (URISyntaxException e) {

				e.printStackTrace();
			}
			// 发送请求
			HttpResponse response;
			String result = "ok";
			try {
				response = client.execute(httpget);
				if (response.getStatusLine().getStatusCode() == 200) {
					HttpEntity resEntity = response.getEntity();
					String message = EntityUtils.toString(resEntity, "utf-8");
					// System.err.println(URLDecoder.decode(message, "UTF-8"));
					if (log.isInfoEnabled()) {
						log.info("调用远端服务返回信息为：" + message);
					}

				} else {
					result = "falied";
				}
			} catch (ClientProtocolException e) {

				e.printStackTrace();
			} catch (IOException e) {

				e.printStackTrace();
			}
			if (result.equals("ok")) {
				return true;
			} else {
				return false;
			}
		}else{
			log.warn("未读取到配置文件中的远端服务，请检查配置!如果自己实现了接口，请在配置文件中填写实现名");
			return false;
		}
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
