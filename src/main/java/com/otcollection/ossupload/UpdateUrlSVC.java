package com.otcollection.ossupload;

public interface UpdateUrlSVC {
	  //扩展接口，fileName:文件名；newUrl:上传完成后阿里给出的url
      public boolean updateUrl(String fileName,String newUrl);
}
