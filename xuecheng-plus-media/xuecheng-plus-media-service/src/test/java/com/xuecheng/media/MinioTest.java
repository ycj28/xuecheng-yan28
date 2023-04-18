package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;

public class MinioTest {

    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void test_upload() throws Exception {

        // 通过扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".docx");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
        if (extensionMatch != null ){
            mimeType = extensionMatch.getMimeType();
        }

        UploadObjectArgs testbucket = UploadObjectArgs.builder()
                .bucket("testbucket") // 桶名
                .object("第3章媒资管理模块v3.1.docx") // 对象名 在根目录存储该文件
                .object("test/01/第3章媒资管理模块v3.1.docx") // 对象名 放在子目录下
                .contentType(mimeType)
                .filename("E:\\BaiduNetdiskDownload\\学成在线项目—资料\\day05 媒资管理 Nacos Gateway MinIO\\资料\\第3章媒资管理模块v3.1.docx") // 本地文件路径
                .build();

        // 上传文件
        minioClient.uploadObject(testbucket);
        System.out.println("上传文件成功！");
    }

    @Test
    public void test_delete() throws Exception {
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket("testbucket")
                .object("第3章媒资管理模块v3.1.docx").build();

        // 上传文件
        minioClient.removeObject(removeObjectArgs);
        System.out.println("删除文件成功！");
    }

    @Test
    public void test_find() throws Exception {
        // 从minio中下载文件
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket("testbucket")
                .object("test/01/第3章媒资管理模块v3.1.docx").build();
        // 指定输出流
        FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
        FileOutputStream outputStream = new FileOutputStream(new File("D:\\develop\\1_2.docx"));
        IOUtils.copy(inputStream,outputStream);

        // 校验文件的完整性，对文件的内容进行MD5
        String sources = DigestUtils.md5Hex(inputStream); //minio中文件的md5
        String local = DigestUtils.md5Hex(new FileInputStream("D:\\develop\\1_2.docx")); //本地文件的md5
        if (sources.equals(local)) {
            System.out.println("下载成功");
        }

    }
}
