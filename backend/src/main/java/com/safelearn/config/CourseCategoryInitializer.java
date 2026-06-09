package com.safelearn.config;

import com.safelearn.entity.CourseCategory;
import com.safelearn.repository.CourseCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseCategoryInitializer implements ApplicationRunner {

    private final CourseCategoryRepository categoryRepo;

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepo.count() > 0) return;

        log.info("初始化默认课程分类...");
        seed("cc000000-0000-0000-0000-000000000001", "basic", "基础知识", "", 1);
        seed("cc000000-0000-0000-0000-000000000002", "battery", "电池安全", "success", 2);
        seed("cc000000-0000-0000-0000-000000000003", "thermal", "热失控", "danger", 3);
        seed("cc000000-0000-0000-0000-000000000004", "fire", "消防安全", "warning", 4);
        seed("cc000000-0000-0000-0000-000000000005", "bms", "BMS系统", "info", 5);
        seed("cc000000-0000-0000-0000-000000000006", "case", "事故案例", "", 6);
    }

    private void seed(String id, String code, String name, String tagType, int sortOrder) {
        CourseCategory c = new CourseCategory();
        c.setId(id);
        c.setCode(code);
        c.setName(name);
        c.setTagType(tagType);
        c.setSortOrder(sortOrder);
        c.setEnabled(true);
        categoryRepo.save(c);
    }
}
