package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CourseTeacherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    private CourseMarketMapper courseMarketMapper;
    @Autowired
    private CourseCategoryMapper courseCategoryMapper;
    @Autowired
    private TeachplanMapper teachplanMapper;
    @Autowired
    private TeachplanMediaMapper teachplanMediaMapper;
    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto dto) {

        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(dto.getCourseName()),CourseBase::getName,dto.getCourseName());
        queryWrapper.eq(StringUtils.isNotEmpty(dto.getAuditStatus()),CourseBase::getAuditStatus,dto.getAuditStatus());
        queryWrapper.eq(StringUtils.isNotEmpty(dto.getPublishStatus()),CourseBase::getStatus,dto.getPublishStatus());

        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        List<CourseBase> records = pageResult.getRecords();
        long total = pageResult.getTotal();
        PageResult<CourseBase> courseBasePageResult = new PageResult<CourseBase>(records,total,pageParams.getPageNo(),pageParams.getPageSize());
        return courseBasePageResult;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBae(Long companyId,AddCourseDto dto) {
        // 向课程基本信息表course_base写入数据
        CourseBase courseBase = new CourseBase();
        // 将传入页面的参数传入CourseBase对象中
        BeanUtils.copyProperties(dto,courseBase);
        courseBase.setCompanyId(companyId);
        courseBase.setCreateDate(LocalDateTime.now());
        // 审核状态和发布状态默认值
        courseBase.setAuditStatus("202002");
        courseBase.setStatus("203001");
        // 插入数据
        int rows = courseBaseMapper.insert(courseBase);
        if (rows != 1){
            throw new RuntimeException("添加课程失败");
        }

        // 向课程查询营销表course_market写入数据
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto,courseMarket);
        // 主键课程的id
        Long courseId = courseBase.getId();
        courseMarket.setId(courseId);
        saveCourseMarket(courseMarket);
        //从数据库中查询课程的详细信息
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    // 查询课程信息
    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId){
        // 查课程信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase==null){
            return null;
        }
        // 查营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        // 组装
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        if (courseMarket != null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }
        // 查询课程分类名称
        CourseCategory courseCategory = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setStName(courseCategory.getName());
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());
        return courseBaseInfoDto;
    }

    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {

        // 拿到课程id
        Long courseId = editCourseDto.getId();
        // 查询课程信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null){
            XueChengPlusException.cast("课程不存在");
        }
        // 数据的合法性校验
        // 本机构只能修改本机构的课程
        if (!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("只能修改本机构的课程哦！");
        }

        // 封装数据
        BeanUtils.copyProperties(editCourseDto,courseBase);
        // 修改时间
        courseBase.setChangeDate(LocalDateTime.now());
        //更新数据库
        int rows = courseBaseMapper.updateById(courseBase);
        if (rows != 1){
            XueChengPlusException.cast("修改课程失败");
        }

        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if (courseMarket == null){
            XueChengPlusException.cast("课程不存在");
        }
        BeanUtils.copyProperties(editCourseDto,courseMarket);
        rows = saveCourseMarket(courseMarket);
        if (rows != 1){
            XueChengPlusException.cast("修改课程失败");
        }
        // 查询课程信息
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    @Override
    @Transactional
    public void deleteCourse(Long courseId) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (!courseBase.getAuditStatus().equals("202002")) {
            XueChengPlusException.cast("课程已经提交审核，不能删除！");
            return;
        }
        // 删除课程基本信息
        int rows = courseBaseMapper.deleteById(courseId);
        if (rows != 1){
            XueChengPlusException.cast("删除课程基本信息失败");
        }
        // 删除课程营销信息
        rows = courseMarketMapper.deleteById(courseId);
        if (rows != 1){
            XueChengPlusException.cast("删除课程营销信息失败");
        }
        // 删除课程计划信息
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId);
        rows = teachplanMapper.delete(queryWrapper);
        if (rows < 0){
            XueChengPlusException.cast("删除课程计划失败");
        }

        LambdaQueryWrapper<TeachplanMedia> teachplanMediaWrapper = new LambdaQueryWrapper<>();
        teachplanMediaWrapper.eq(TeachplanMedia::getCourseId,courseId);
        rows = teachplanMediaMapper.delete(teachplanMediaWrapper);
        if (rows < 0){
            XueChengPlusException.cast("删除课程计划相关媒资失败");
        }

        // 删除课程教师信息表
        LambdaQueryWrapper<CourseTeacher> courseTeacherQueryWrapper = new LambdaQueryWrapper<>();
        courseTeacherQueryWrapper.eq(CourseTeacher::getCourseId,courseId);
        rows = courseTeacherMapper.delete(courseTeacherQueryWrapper);
        if (rows < 0){
            XueChengPlusException.cast("删除课程教师信息失败");
        }
    }

    // 保存营销信息
    private int saveCourseMarket(CourseMarket courseMarket){
        // 参数的合法性校验
        String charge = courseMarket.getCharge();
        if (StringUtils.isEmpty(charge)){
            throw new RuntimeException("收费规则为空");
        }
        // 如果课程收费，价格没有填写叶需要抛异常
        if ("201001".equals(charge)){
            if (courseMarket.getPrice() == null || courseMarket.getPrice() <= 0){
                XueChengPlusException.cast("课程价格不能为空且必须大于0");
            }
        }
        CourseMarket courseMarket2Save = courseMarketMapper.selectById(courseMarket.getId());
        if (courseMarket2Save == null){
            // 插入数据库
            int rows = courseMarketMapper.insert(courseMarket);
            return rows;
        }else{
            BeanUtils.copyProperties(courseMarket,courseMarket2Save);
            courseMarket2Save.setId(courseMarket.getId());
            // 更新
            int rows = courseMarketMapper.updateById(courseMarket2Save);
            return rows;
        }
    }
}
