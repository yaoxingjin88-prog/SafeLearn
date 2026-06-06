package com.safelearn.service;

import com.safelearn.dto.AiAskRequest;
import com.safelearn.dto.FeedbackRequest;
import com.safelearn.entity.QaRecord;
import com.safelearn.entity.User;
import com.safelearn.repository.QaRecordRepository;
import com.safelearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiService {

    private final QaRecordRepository qaRepo;
    private final UserRepository userRepo;

    private static final Map<String, String> KNOWLEDGE = new LinkedHashMap<>();
    static {
        KNOWLEDGE.put("冒烟", "储能柜冒烟处理步骤：1. 立即切断电源；2. 启动消防系统；3. 疏散周围人员；4. 通知消防部门；5. 在安全距离外观察。切勿直接用水扑灭锂电池火灾。");
        KNOWLEDGE.put("热失控", "热失控前兆包括：1. 电池温度异常升高；2. 电压异常波动；3. 电池膨胀变形；4. 产生异味气体；5. BMS频繁告警。一旦发现应立即启动应急响应。");
        KNOWLEDGE.put("消防", "锂电池储能消防系统类型：1. 气体灭火系统（七氟丙烷、全氟己酮）；2. 水喷雾系统；3. 细水雾系统；4. 干粉灭火系统。推荐使用全氟己酮。");
        KNOWLEDGE.put("BMS", "BMS系统主要功能：1. 电池状态监控（电压、电流、温度）；2. SOC/SOH估算；3. 均衡管理；4. 故障诊断与预警；5. 热管理控制。");
        KNOWLEDGE.put("安全检查", "储能电站安全检查重点：1. 电池外观检查；2. BMS告警记录；3. 消防系统状态；4. 通风系统运行；5. 电气连接检查；6. 接地系统检测。");
    }

    public Map<String, Object> ask(String userId, AiAskRequest req) {
        String question = req.getQuestion();
        String answer = KNOWLEDGE.entrySet().stream()
                .filter(e -> question.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(() -> "关于\"" + question + "\"的解答：储能电站安全是新能源行业的重要课题。建议参考《储能电站安全管理规定》和《锂离子电池储能系统技术规范》。如需更详细的解答，请提供更具体的问题描述。");

        String sources = "[\"储能电站安全管理规定\",\"锂离子电池储能系统技术规范\"]";

        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        QaRecord record = new QaRecord();
        record.setUser(user);
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setSources(sources);
        qaRepo.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("sources", List.of(
                Map.of("id", "1", "title", "储能电站安全管理规定", "relevance", 0.95),
                Map.of("id", "2", "title", "锂离子电池储能系统技术规范", "relevance", 0.88)
        ));
        result.put("relatedQuestions", List.of("热失控前兆有哪些？", "消防系统如何选择？"));
        return result;
    }

    public Map<String, Object> getHistory(String userId, int page, int pageSize) {
        Page<QaRecord> records = qaRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page - 1, pageSize));
        Map<String, Object> result = new HashMap<>();
        result.put("items", records.getContent().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("question", r.getQuestion());
            m.put("answer", r.getAnswer());
            m.put("rating", r.getRating());
            m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            return m;
        }).toList());
        result.put("total", records.getTotalElements());
        return result;
    }

    public Map<String, Object> feedback(FeedbackRequest req) {
        QaRecord record = qaRepo.findById(req.getRecordId())
                .orElseThrow(() -> new RuntimeException("记录不存在"));
        record.setRating(req.getRating());
        qaRepo.save(record);
        return Map.of("success", true);
    }
}
