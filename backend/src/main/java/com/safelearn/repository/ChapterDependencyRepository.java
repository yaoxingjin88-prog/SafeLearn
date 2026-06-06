package com.safelearn.repository;

import com.safelearn.entity.ChapterDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterDependencyRepository extends JpaRepository<ChapterDependency, String> {

    /**
     * 查找目标章节的所有前置依赖
     */
    List<ChapterDependency> findByTargetChapterId(String targetChapterId);

    /**
     * 查找某个章节作为前置依赖的所有目标章节
     */
    List<ChapterDependency> findByPrerequisiteChapterId(String prerequisiteChapterId);

    /**
     * 批量查找多个章节的前置依赖
     */
    @Query("SELECT d FROM ChapterDependency d WHERE d.targetChapterId IN :chapterIds")
    List<ChapterDependency> findByTargetChapterIds(@Param("chapterIds") List<String> chapterIds);

    /**
     * 检查是否存在依赖关系
     */
    boolean existsByTargetChapterIdAndPrerequisiteChapterId(String targetChapterId, String prerequisiteChapterId);

    /**
     * 删除指定目标章节的所有依赖关系
     */
    void deleteByTargetChapterId(String targetChapterId);
}
