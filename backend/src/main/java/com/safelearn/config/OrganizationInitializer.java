package com.safelearn.config;

import com.safelearn.entity.Department;
import com.safelearn.entity.DepartmentPosition;
import com.safelearn.entity.User;
import com.safelearn.repository.DepartmentPositionRepository;
import com.safelearn.repository.DepartmentRepository;
import com.safelearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationInitializer implements ApplicationRunner {

    private static final String ROOT_ID = "dept0000-0000-0000-0000-000000000001";

    private final DepartmentRepository departmentRepo;
    private final DepartmentPositionRepository positionRepo;
    private final UserRepository userRepo;

    @Override
    public void run(ApplicationArguments args) {
        if (departmentRepo.count() > 0) {
            syncUserDepartments();
            return;
        }

        Department root = saveDept(ROOT_ID, "储能科技集团有限公司", null, null, null, 0);

        String runId = saveDept(null, "运行部", root.getId(), "张三", "运行部主任", 1).getId();
        String opsId = saveDept(null, "运维部", root.getId(), "李四", "运维部经理", 2).getId();
        String safeId = saveDept(null, "安全部", root.getId(), "王五", "安全部门经理", 3).getId();
        String techId = saveDept(null, "技术部", root.getId(), "赵六", "技术总监", 4).getId();
        String generalId = saveDept(null, "综合部", root.getId(), "钱七", "综合办主任", 5).getId();

        seedPositions(safeId, List.of(
                pos("安全专员", false, 1),
                pos("安全工程师", true, 2),
                pos("安全管理员", false, 3),
                pos("消防管理员", true, 4)
        ));
        seedPositions(runId, List.of(
                pos("运行值班员", true, 1),
                pos("运行班长", false, 2)
        ));
        seedPositions(opsId, List.of(
                pos("设备工程师", false, 1),
                pos("运维工程师", false, 2)
        ));
        seedPositions(techId, List.of(
                pos("系统工程师", false, 1),
                pos("研发工程师", false, 2)
        ));
        seedPositions(generalId, List.of(
                pos("行政专员", false, 1)
        ));

        assignDemoUsers();
        log.info("组织架构初始数据已就绪");
    }

    private void syncUserDepartments() {
        Map<String, String> deptByName = departmentRepo.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Department::getName, Department::getId, (a, b) -> a));
        for (User user : userRepo.findAll()) {
            if (user.getDepartment() != null && deptByName.containsKey(user.getDepartment())) {
                continue;
            }
        }
    }

    private void assignDemoUsers() {
        updateUserDept("lisi", "安全部", "安全工程师", "10007");
        updateUserDept("wangwu", "安全部", "安全专员", "10008");
        updateUserDept("zhanggong", "运维部", "设备工程师", "10001");
        User admin = userRepo.findByUsername("admin").orElse(null);
        if (admin != null) {
            admin.setDepartment("安全部");
            admin.setPosition("安全管理员");
            admin.setEmployeeNo("10003");
            userRepo.save(admin);
        }
    }

    private void updateUserDept(String username, String dept, String position, String employeeNo) {
        userRepo.findByUsername(username).ifPresent(user -> {
            user.setDepartment(dept);
            user.setPosition(position);
            user.setEmployeeNo(employeeNo);
            userRepo.save(user);
        });
    }

    private Department saveDept(String id, String name, String parentId, String leader, String leaderTitle, int sort) {
        Department dept = new Department();
        if (id != null) dept.setId(id);
        dept.setName(name);
        dept.setParentId(parentId);
        dept.setLeaderName(leader);
        dept.setLeaderTitle(leaderTitle);
        dept.setSortOrder(sort);
        return departmentRepo.save(dept);
    }

    private void seedPositions(String deptId, List<DepartmentPosition> positions) {
        for (DepartmentPosition p : positions) {
            p.setDepartmentId(deptId);
            positionRepo.save(p);
        }
    }

    private DepartmentPosition pos(String name, boolean highRisk, int sort) {
        DepartmentPosition p = new DepartmentPosition();
        p.setName(name);
        p.setHighRisk(highRisk);
        p.setSortOrder(sort);
        return p;
    }
}
