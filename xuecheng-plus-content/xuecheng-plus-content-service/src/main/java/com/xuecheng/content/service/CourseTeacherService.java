package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

public interface CourseTeacherService {

    /**
     * 获取教师列表
     * @param courseId
     * @return
     */
    List<CourseTeacher> getTeacherList(Long courseId);

    /**
     * 添加老师
     * @param courseTeacher
     * @return
     */
    CourseTeacher insertOrUpdateTeacher(CourseTeacher courseTeacher);

    /**
     * 删除教师
     * @param courseId
     * @param teacherId
     */
    void deleteTeacher(Long courseId, Long teacherId);
}
