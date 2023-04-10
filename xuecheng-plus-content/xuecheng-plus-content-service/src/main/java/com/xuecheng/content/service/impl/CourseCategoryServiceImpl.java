package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    private CourseCategoryMapper courseCategoryMapper;;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        List<CourseCategoryTreeDto> treeDtos = courseCategoryMapper.selectTreeNode(id);
        // 将list转map,以备使用,排除根节点
        Map<String, CourseCategoryTreeDto> mapTemp = treeDtos.stream()
                .filter(item->!id.equals(item.getId()))
                .collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));
        // 最终返回的list
        List<CourseCategoryTreeDto> courseList = new ArrayList<>();
        // 依次遍历每个元素,排除根节点
        treeDtos.stream().filter(item->!id.equals(item.getId())).forEach(item ->{
            if (item.getParentid().equals(id)){
                courseList.add(item);
            }
            // 找到节点的父节点
            CourseCategoryTreeDto courseCategoryParent = mapTemp.get(item.getParentid());
            if (courseCategoryParent != null){
                if (courseCategoryParent.getChildrenTreeNodes()==null){
                    courseCategoryParent.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                courseCategoryParent.getChildrenTreeNodes().add(item);
            }
        });
        return courseList;
    }
}
