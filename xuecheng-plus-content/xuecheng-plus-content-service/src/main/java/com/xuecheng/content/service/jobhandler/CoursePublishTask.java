package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    private CoursePublishService coursePublishService;

    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex="+shardIndex+",shardTotal="+shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex,shardTotal,"course_publish",30,60);
    }

    // 执行课程发布任务的方法
    @Override
    public boolean execute(MqMessage mqMessage) {

        // 从mqMessage拿到课程id
        Long courseId = Long.valueOf(mqMessage.getBusinessKey1());

        // 课程静态化上传到minio
        generateCourseHtml(mqMessage, courseId);

        // 向elasticsearch写索引
        saveCourseIndex(mqMessage,courseId);
        // 向redis写缓存

        // 返回true表示任务完成
        return true;
    }

    //生成课程静态化页面并上传至文件系统
    private void generateCourseHtml(MqMessage mqMessage, long courseId) {
        // 消息id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        // 任务幂等性处理

        // 取出该阶段的执行状态
        int stageOne = mqMessageService.getStageOne(taskId);
        if (stageOne > 0) {
            log.debug("课程静态化任务完成，无需处理");
            return;
        }
        // 开始进行课程静态化,生成html页面，将html上传至minio
        File file = coursePublishService.generateCourseHtml(courseId);
        if (file == null){
            XueChengPlusException.cast("生成的静态页面为空");
        }
        coursePublishService.uploadCourseHtml(courseId,file);


        // ..任务处理完成写任务状态为完成
        mqMessageService.completedStageOne(taskId);
    }

    //保存课程索引信息
    private void saveCourseIndex(MqMessage mqMessage,long courseId){
        // 任务id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        int stageTwo = mqMessageService.getStageTwo(taskId);
        // 任务幂等性处理
        if (stageTwo>0){
            log.debug("课程索引信息已写入，无需执行");
            return;
        }
        // 查询课程信息你，调用搜索服务添加索引

        // 完成本阶段的任务
        mqMessageService.completedStageTwo(courseId);

    }
}
