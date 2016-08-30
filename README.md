#项目介绍
otcollection 这个程序用来定时上传数据到阿里云的oss ,使用 spring 的 quartz 做定时任务<br>
1.定时任务DealSynJob 负责将冷却期内的数据上传到 oss，冷却期通过配置和目录结构计算获得<br>
2.上传过程通过md5校验保证上传数据的正确性 <br>
3.上传失败的数据会生成日志文件，等待自动重试 <br>
4.上传成功会调用 http 服务做回调 ，告知上传成功, 使用者也可以自己实现UpdateUrlSVC完成上传数据更新<br>
5.上传最终的结果会记录在配置的日志文件中<br>
#编译和使用
项目通过maven 管理 <br>
1. 使用eclipse 按照 maven project 方式 import<br>
2. 运行 run as -> maven install 后 <br>
3. 在target 目录下拷贝出 生成的 dependency 目录和  ot_ossupload.jar <br>
4. 使用 java -jar ot_ossupload.jar 启动程序<br>


#配置说明
<strong>conf.properties</strong><br>
<strong>必填项，连接使用的 endpoint   eg: oss-cn-shanghai.aliyuncs.com</strong><br>
endpoint=oss-cn-shanghai.aliyuncs.com<br>
<br>
<strong>必填项，用户的ak，用于连接阿里客户端</strong><br>
accessKeyId=xxxxxxx<br>
accessKeySecret=xxxxxxxx<br>
<br>
<strong>必填项，bucket名，用于指定文件上传的bucket</strong><br>
bucketName=xxxxxx<br>
<br>
<strong>所有路径 windows 下面  路径分隔符 为  \\  ,unix 为 / 使用的都是绝对路径</strong><br>
<strong>必填项，本地文件的路径，windows下的文件路径请用\\作为分隔符，例如D:\\test\\mytest</strong><br>
<strong>linux下使用原始路径即可，例如/Users/my_document/mpeg4</strong><br>
<strong>由文件时间信息根据路径获得的，请保证填写的路径下有相应时间信息，并且可以用之后填写的正则表达式提取时间信息</strong><br>
path=D:\\testoss<br>
<br>
<strong>必填项，从路径获取时间的正则表达式，例如D:\\testoss\\年(2016)\\月(1)\\日(1)\\xxxx\\xxxxx  是可以被识别的路径</strong><br>
regex=[年|月|日]\\((\\d+)\\)<br>

<strong>必填项，文件冷却时间，超过这个时间的文件将会被上传，单位为毫秒ms，默认值为7天</strong><br>
timeLength=xxxxxL<br>
<br>
<strong>安全时间，在安全时间内的文件将被上传，例如安全时间8天，超出安全时间的文件不会上传</strong><br>
<code>safeTimeLength=xxxxx</code><br>
<br>
<strong>必填项，用于锁定进程的文件，确保不能同时开启多个相同进程发生抢夺资源的情况</strong><br>
<strong>给出路径和文件名即可，若不存在会自动生成，推荐使用任意txt小文件</strong><br>
lockFile=D:\\lockFile.txt<br>
<br>


<strong>必填项，记录oss上的文件的url的输出路径用于核对，例如D:\\testoss\\url.txt，给出路径和文件名即可，若不存在会自动生成</strong><br>
ossUrlFileName=D:\\testoss_url\\url.txt<br>
<br>
<strong>选填项，如不填写远端服务地址，则无法调用远端服</strong>务<br>
<strong>调用的远端服务地址，必须给出请求的两个参数名，分别代表文件名和新生成的url；如oldurl，代表文件路径；newurl，代表新生成的url</strong><br>
<strong>eg : http://127.0.0.1/util/oss/updateUrl?oldurl={fileName}&newurl={ossUrl}</strong><br>
service=http://xxxxx/updateUrl?oldurl={fileName}&newurl={ossUrl}<br>
<br>
<strong>选填项，如果额外实现了接口UpdateUrlSVC，请填写实现函数名，将会优先调用该实现方法，未实现则无需填写</strong><br>
updateUrlSVCImpl=<br>
<br>

<strong>必填项，mode=pub 表示公有云 ，mode=private 表示私有云</strong><br>
mode=pub<br>
