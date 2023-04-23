package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    @Override
    public List<CourseTeacher> getTeacherList(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(queryWrapper);
        return courseTeachers;
    }

    @Override
    public CourseTeacher insertOrUpdateTeacher(CourseTeacher courseTeacher) {
        // 如果不携带id，为增加教师
        courseTeacher.setCreateDate(LocalDateTime.now());
        if (courseTeacher.getId() == null){
            int rows = courseTeacherMapper.insert(courseTeacher);
            if (rows != 1){
                XueChengPlusException.cast("添加教师失败！");
                return null;
            }
            return courseTeacher;
        }
        // 如果携带id则是修改教师
        int rows = courseTeacherMapper.updateById(courseTeacher);
        if (rows != 1){
            XueChengPlusException.cast("修改教师失败！");
            return null;
        }
        return courseTeacher;
    }

    @Override
    public void deleteTeacher(Long courseId, Long teacherId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId).eq(CourseTeacher::getId,teacherId);
        courseTeacherMapper.delete(queryWrapper);
    }
}
