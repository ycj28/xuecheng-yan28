package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseTeacher;
import lombok.Data;

import java.util.List;

@Data
public class CoursePreviewDto {

    // 课程基本信息，课程营销信息
    private CourseBaseInfoDto courseBase;

    // 课程计划信息
    private List<TeachplanDto> teachplans;

    // 课程师资信息...
    private List<CourseTeacher> courseTeachers;
}
