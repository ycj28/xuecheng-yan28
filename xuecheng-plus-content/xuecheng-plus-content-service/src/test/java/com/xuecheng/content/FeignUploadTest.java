package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@SpringBootTest
public class FeignUploadTest {

    @Autowired
    MediaServiceClient mediaServiceClient;

    @Test
    public void test() throws IOException {
        // 将file类型转成MultipartFile
        File file = new File("E:\\BaiduNetdiskDownload\\学成在线项目—资料\\day08 课程发布 分布式事务\\资料\\1.html");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        // 远程调用
        mediaServiceClient.upload(multipartFile,"course/1.html");

    }
}
