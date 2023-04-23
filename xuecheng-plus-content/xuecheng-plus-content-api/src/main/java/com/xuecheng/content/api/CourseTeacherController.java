package com.xuecheng.content.api;

import com.baomidou.mybatisplus.extension.api.R;
import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CourseTeacherController {

    @Autowired
    private CourseTeacherService courseTeacherService;

    @ApiOperation("查询教师")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> getTeacherList(@PathVariable Long courseId){
        return courseTeacherService.getTeacherList(courseId);
    }

    @ApiOperation("添加/修改教师")
    @PostMapping("/courseTeacher")
    public CourseTeacher insertOrUpdateTeacher(@RequestBody CourseTeacher courseTeacher){
        return courseTeacherService.insertOrUpdateTeacher(courseTeacher);
    }

    @ApiOperation("删除教师")
    @DeleteMapping("/courseTeacher/course/{courseId}/{teacherId}")
    public void deleteTeacher(@PathVariable Long courseId, @PathVariable Long teacherId){
        courseTeacherService.deleteTeacher(courseId,teacherId);
    }
}
