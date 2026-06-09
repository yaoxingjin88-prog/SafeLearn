package com.safelearn.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safelearn.entity.ChapterQuiz;
import com.safelearn.repository.ChapterQuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 启动时同步章节测验（resources/quiz-content/*.json）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizContentInitializer implements ApplicationRunner {

    private final ChapterQuizRepository quizRepo;
    private final ObjectMapper objectMapper;

    private record QuizFileDef(String chapterId, String jsonFile) {}

    private static final List<QuizFileDef> DEFINITIONS = List.of(
            new QuizFileDef("20000000-0000-0000-0000-000000000001", "quiz-ch-001.json"),
            new QuizFileDef("20000000-0000-0000-0000-000000000002", "quiz-ch-002.json"),
            new QuizFileDef("20000000-0000-0000-0000-000000000003", "quiz-ch-003.json"),
            new QuizFileDef("20000000-0000-0000-0000-000000000004", "quiz-ch-004.json"),
            new QuizFileDef("20000000-0000-0000-0000-000000000005", "quiz-ch-005.json"),
            new QuizFileDef("20000000-0000-0000-0000-000000000006", "quiz-ch-006.json"),
            new QuizFileDef("20000000-0000-0000-0000-000000000007", "quiz-ch-007.json"),
            new QuizFileDef("20000000-0000-0000-0000-000000000008", "quiz-ch-008.json"),
            new QuizFileDef("20000000-0000-0000-0000-000000000009", "quiz-ch-009.json"),
            new QuizFileDef("20000000-0000-0000-0000-000000000010", "quiz-ch-010.json"),
            new QuizFileDef("20000000-0000-0000-0000-000000000011", "quiz-ch-011.json"),
            new QuizFileDef("20000000-0000-0000-0000-000000000012", "quiz-ch-012.json"),
            new QuizFileDef("20000000-0000-0000-0000-000000000013", "quiz-ch-013.json")
    );

    @Override
    public void run(ApplicationArguments args) {
        int synced = 0;
        for (QuizFileDef def : DEFINITIONS) {
            try {
                if (syncQuiz(def)) synced++;
            } catch (Exception e) {
                log.warn("同步章节测验失败 chapterId={}: {}", def.chapterId(), e.getMessage());
            }
        }
        if (synced > 0) {
            log.info("已同步 {} 个章节测验", synced);
        }
    }

    private boolean syncQuiz(QuizFileDef def) throws Exception {
        JsonNode root = loadJson(def.jsonFile());
        if (root == null) return false;

        String title = root.path("title").asText();
        String questions = objectMapper.writeValueAsString(root.get("questions"));
        int passScore = root.path("passScore").asInt(60);
        int timeLimit = root.path("timeLimit").asInt(15);

        Optional<ChapterQuiz> existing = quizRepo.findByChapterId(def.chapterId());
        if (existing.isPresent()) {
            ChapterQuiz quiz = existing.get();
            if (title.equals(quiz.getTitle()) && questions.equals(quiz.getQuestions())
                    && passScore == quiz.getPassScore() && timeLimit == quiz.getTimeLimit()) {
                return false;
            }
            quiz.setTitle(title);
            quiz.setQuestions(questions);
            quiz.setPassScore(passScore);
            quiz.setTimeLimit(timeLimit);
            quizRepo.save(quiz);
            return true;
        }

        String quizId = resolveQuizId(def.chapterId());
        ChapterQuiz quiz = new ChapterQuiz();
        quiz.setId(quizId);
        quiz.setChapterId(def.chapterId());
        quiz.setTitle(title);
        quiz.setQuestions(questions);
        quiz.setPassScore(passScore);
        quiz.setTimeLimit(timeLimit);
        quizRepo.save(quiz);
        return true;
    }

    private String resolveQuizId(String chapterId) {
        String preferred = chapterId.replaceFirst("^200", "600");
        if (quizRepo.findById(preferred).map(q -> chapterId.equals(q.getChapterId())).orElse(true)) {
            if (!quizRepo.existsById(preferred)) return preferred;
            if (quizRepo.findById(preferred).map(q -> chapterId.equals(q.getChapterId())).orElse(false)) {
                return preferred;
            }
        }
        return UUID.randomUUID().toString();
    }

    private JsonNode loadJson(String fileName) throws Exception {
        ClassPathResource resource = new ClassPathResource("quiz-content/" + fileName);
        if (!resource.exists()) return null;
        String raw = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        return objectMapper.readTree(raw);
    }
}
