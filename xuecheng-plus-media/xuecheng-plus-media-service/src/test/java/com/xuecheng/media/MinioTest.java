package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;


import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MinioTest {

    @Value("${spring.datasource.url}")
    private String bucket_mediafiles;

    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void test(){
        System.err.println(bucket_mediafiles);
    }

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

    // 将分块文件上传到minio
    @Test
    public void uploadChunk() throws Exception {
        for (int i = 0; i < 5; i++) {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("testbucket") // 桶名
                    .object("第3章媒资管理模块v3.1.docx") // 对象名 在根目录存储该文件
                    .object("test/chunk/"+i) // 对象名 放在子目录下
                    .filename("D:\\1. Spring Boot入门\\chunk\\"+i) // 本地文件路径
                    .build();

            // 上传文件
            minioClient.uploadObject(testbucket);
            System.out.println("上传分块"+i+"成功");
        }
    }

    // 调用minio接口合并分块
    @Test
    public void testMerge() throws Exception {
//        List<ComposeSource> sources = new ArrayList<>();
//        for (int i = 0; i < 21; i++) {
//            // 指定分块文件信息
//            ComposeSource build = ComposeSource.builder().bucket("testbucket").object("test/chunk/" + i).build();
//            sources.add(build);
//        }

        List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(5)
                .map(i -> ComposeSource.builder().bucket("testbucket")
                        .object("test/chunk/" + i).build()).collect(Collectors.toList());

        // 指定合并后的objectName等信息
        ComposeObjectArgs testbucket = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("test/merge/merge01.mp4")
                .sources(sources) // 置顶源文件
                .build();
        // 合并文件
        // 报错size 1048576 must be greater than 5242880，minio默认分块大小是5M
        minioClient.composeObject(testbucket);
    }

    // 批量清理分块文件
}
