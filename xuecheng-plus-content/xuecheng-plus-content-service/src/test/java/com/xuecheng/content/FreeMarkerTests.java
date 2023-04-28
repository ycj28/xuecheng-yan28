package com.xuecheng.content;


import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;


@SpringBootTest
public class FreeMarkerTests {

    @Autowired
    private CoursePublishService coursePublishService;

    @Test
    public void testGenerateHtmlByTemplate() throws IOException, TemplateException {
        Configuration configuration = new Configuration(Configuration.getVersion());

        // 拿到classpath路径
        String path = this.getClass().getResource("/").getPath();
        // 指定模板的目录
        configuration.setDirectoryForTemplateLoading(new File(path+"/templates/"));
        // 指定编码
        configuration.setDefaultEncoding("utf-8");
        // 得到模板
        Template template = configuration.getTemplate("course_template.ftl");
        // 准备数据
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(1L);
        HashMap<String, Object> map = new HashMap<>();
        map.put("model",coursePreviewInfo);
        String s = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);

        // 输入流
        InputStream inputStream = IOUtils.toInputStream(s, "utf-8");
        // 输出文件
        FileOutputStream outputStream = new FileOutputStream(new File("E:\\BaiduNetdiskDownload\\学成在线项目—资料\\day08 课程发布 分布式事务\\资料\\1.html"));
        // 使用流将html写入文件
        IOUtils.copy(inputStream,outputStream);
    }

}
