package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    private TeachplanMapper teachplanMapper;
    @Autowired
    private TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;
    }

    private int getTeachplanCount(Long courseId,Long parentId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId).eq(Teachplan::getParentid,parentId);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count+1;
    }

    @Override
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {
        // 通过课程计划id判断是新增还是修改
        Long dtoId = teachplanDto.getId();
        if (dtoId == null){
            // 新增
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(teachplanDto,teachplan);
            // 确定排序字段，找到同级节点个数
            Long parentId = teachplan.getParentid();
            Long courseId = teachplan.getCourseId();
            teachplan.setOrderby(getTeachplanCount(courseId,parentId));
            teachplanMapper.insert(teachplan);
        }else{
            // 修改
            Teachplan teachplan = teachplanMapper.selectById(dtoId);
            BeanUtils.copyProperties(teachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }

    @Transactional
    @Override
    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan==null){
            XueChengPlusException.cast("教学计划不存在");
        }
        Integer grade = teachplan.getGrade();
        if(grade!=2){
            XueChengPlusException.cast("只允许第二级教学计划绑定媒资文件");
        }
        //课程id
        Long courseId = teachplan.getCourseId();

        // 先删除原有记录,根据课程计划id删除它所绑定的媒资
        LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachplanMedia::getTeachplanId,bindTeachplanMediaDto.getTeachplanId());
        teachplanMediaMapper.delete(queryWrapper);

        // 再添加新记录
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachplanMediaDto,teachplanMedia);
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);

    }

    @Override
    public void deleteTeachplan(Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if (teachplan == null){
            XueChengPlusException.cast("课程计划信息无法操作");
            return;
        }
        Integer grade = teachplan.getGrade();
        // 删除第一级别的章节时要求其下无子小节
        if (grade == 1){
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getParentid,teachplanId);
            List<Teachplan> teachplanList = teachplanMapper.selectList(queryWrapper);
            if (teachplanList.size() != 0){
                XueChengPlusException.cast("课程计划信息还有子级信息，无法操作");
                return;
            }
            int rows = teachplanMapper.deleteById(teachplanId);
            if (rows!=1){
                XueChengPlusException.cast("课程计划信息删除时产生未知的异常");
            }
        }
        // 删除第二级别的章节时要需要将其关联的视频信息也删除
        if (grade == 2) {
            int rows = teachplanMapper.deleteById(teachplanId);
            if (rows!=1){
                XueChengPlusException.cast("课程计划信息删除时产生未知的异常");
                return;
            }
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TeachplanMedia::getTeachplanId,teachplanId);
            TeachplanMedia teachplanMedia = teachplanMediaMapper.selectOne(queryWrapper);
            if (teachplanMedia != null){
                rows = teachplanMediaMapper.deleteById(teachplanMedia.getId());
                if (rows!=1){
                    XueChengPlusException.cast("课程计划信息删除时产生未知的异常");
                    return;
                }
            }
        }

    }

    @Override
    public void teachplanMoveDown(Long teachplanId) {
        // 根据id找到需要进行移动的课程计划
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Integer orderby = teachplan.getOrderby();
        // 找到下一个课程计划
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,teachplan.getCourseId()).eq(Teachplan::getOrderby,orderby+1).eq(Teachplan::getParentid,teachplan.getParentid());
        Teachplan nextTeachplan = teachplanMapper.selectOne(queryWrapper);
        // 若找不到则证明已经是最后一个位置了
        if (nextTeachplan == null){
            XueChengPlusException.cast("已经是最后一个了哦~");
            return;
        }
        teachplan.setOrderby(nextTeachplan.getOrderby());
        teachplan.setCreateDate(LocalDateTime.now());
        nextTeachplan.setOrderby(orderby);
        nextTeachplan.setChangeDate(LocalDateTime.now());
        int rows = teachplanMapper.updateById(teachplan);
        if (rows!=1){
            XueChengPlusException.cast("课程计划移动时产生未知的异常");
            return;
        }
        rows = teachplanMapper.updateById(nextTeachplan);
        if (rows!=1){
            XueChengPlusException.cast("课程计划移动时产生未知的异常");
            return;
        }
    }

    @Override
    public void teachplanMoveoveUp(Long teachplanId) {
        // 根据id找到需要进行移动的课程计划
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Integer orderby = teachplan.getOrderby();
        // 找到上一个课程计划
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,teachplan.getCourseId()).eq(Teachplan::getOrderby,orderby-1).eq(Teachplan::getParentid,teachplan.getParentid());
        Teachplan nextTeachplan = teachplanMapper.selectOne(queryWrapper);
        // 若找不到则证明已经是第一个位置了
        if (nextTeachplan == null){
            XueChengPlusException.cast("已经是第一个了哦~");
            return;
        }
        teachplan.setOrderby(nextTeachplan.getOrderby());
        teachplan.setCreateDate(LocalDateTime.now());
        nextTeachplan.setOrderby(orderby);
        nextTeachplan.setChangeDate(LocalDateTime.now());
        int rows = teachplanMapper.updateById(teachplan);
        if (rows!=1){
            XueChengPlusException.cast("课程计划移动时产生未知的异常");
            return;
        }
        rows = teachplanMapper.updateById(nextTeachplan);
        if (rows!=1){
            XueChengPlusException.cast("课程计划移动时产生未知的异常");
            return;
        }
    }
}
