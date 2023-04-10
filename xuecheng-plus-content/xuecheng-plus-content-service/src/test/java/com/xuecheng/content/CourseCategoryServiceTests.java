package com.xuecheng.content;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class CourseCategoryServiceTests {

    @Autowired
    private CourseCategoryService courseCategoryService;

    @Test
    public void testCourseCategoryService(){
        List<CourseCategoryTreeDto> courseCategoryTreeDtoList = courseCategoryService.queryTreeNodes("1");
        System.out.println(courseCategoryTreeDtoList);
    }

}
