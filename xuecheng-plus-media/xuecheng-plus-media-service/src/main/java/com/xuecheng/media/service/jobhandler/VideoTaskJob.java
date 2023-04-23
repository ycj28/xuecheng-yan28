package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 视频任务处理类
 */
@Slf4j
@Component
public class VideoTaskJob {

    @Autowired
    private MediaFileProcessService mediaFileProcessService;

    @Autowired
    private MediaFileService mediaFileService;

    //ffmpeg的路径
    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;

    /**
     * 视频处理任务
     */
    @XxlJob("videoJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();  // 执行器的序号，从0开始
        int shardTotal = XxlJobHelper.getShardTotal();  // 执行器总数

        // 确定cpu的核心数
        int processors = Runtime.getRuntime().availableProcessors();

        // 查询待处理的任务
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        // 任务的数量
        int size = mediaProcessList.size();
        log.debug("取到的视频处理任务数："+ size);
        if (size <= 0) {
            return;
        }
        // 创建一个线程池
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        // 使用计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            // 将任务加入线程池
            executorService.execute(()->{
                try {
                    // 任务id
                    Long taskId = mediaProcess.getId();
                    // 文件的id就是md5值
                    String md5 = mediaProcess.getFileId();
                    // 开启任务
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        log.debug("抢占任务失败，任务id:{}", taskId);
                        return;
                    }
                    // 执行视频转码
                    // 桶
                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();
                    // 下载minio视频到本地
                    File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if (file == null) {
                        log.debug("下载视频出错，任务id:{},bucket:{},objectName:{}", taskId, bucket, objectName);
                        // 保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", md5, null, "下载视频到本地失败");
                        return;
                    }

                    //源avi视频的路径
                    String video_path = file.getAbsolutePath();
                    //转换后mp4文件的名称
                    String mp4_name = md5 + ".mp4";
                    //转换后mp4文件的路径
                    // 先创建一个临时文件，作为转换后的文件
                    File mp4file = null;
                    try {
                        mp4file = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.debug("创建临时文件异常,{}", e.getMessage());
                        // 保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", md5, null, "下载视频到本地失败");
                        return;
                    }
                    String mp4_path = mp4file.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, video_path, mp4_name, mp4_path);
                    //开始视频转换，成功将返回success
                    String s = videoUtil.generateMp4();
                    if (!s.equals("success")) {
                        log.debug("视频转码失败，bucket:{},objectName:{},原因:{}", bucket, objectName, s);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", md5, null, "视频转码失败");
                        return;
                    }
                    // mp4文件的url
                    String url = getFilePathByMd5(md5, ".mp4");
                    // 上传minio
                    boolean toMinIO = mediaFileService.addMediaFilesToMinIO(mp4_path, "video/mp4", bucket, url);
                    if (!toMinIO) {
                        log.debug("上传MP4到minio失败，taskId:{},bucket:{},objectName:{}", taskId, bucket, objectName);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", md5, null, "上传MP4到minio失败");
                        return;
                    }

                    // 保存任务处理结果
                    mediaFileProcessService.saveProcessFinishStatus(taskId, "2", md5, url, "");
                }finally {
                    // 计算器减去1
                    countDownLatch.countDown();
                }
            });
        });
        // 阻塞,指定最大限度的等待时间，阻塞最多等待一定的时间后就解除阻塞
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    /**
     * 得到合并后的文件的地址
     * @param fileMd5 文件id即md5值
     * @param fileExt 文件扩展名
     * @return
     */
    private String getFilePathByMd5(String fileMd5,String fileExt){
        return   fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
    }

}
