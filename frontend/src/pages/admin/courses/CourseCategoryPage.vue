<template>
  <div class="course-category-page">
    <div class="page-head">
      <div>
        <h2>分类配置</h2>
        <p>管理学员端课程分类标签与排序</p>
      </div>
      <el-button @click="$router.push('/admin/learning/courses')">返回课程列表</el-button>
    </div>
    <CategoryConfigPanel @changed="() => {}" />
  </div>
</template>

<script setup lang="ts">
import CategoryConfigPanel from '@/components/admin/CategoryConfigPanel.vue'
</script>

<style scoped>
.course-category-page { padding: 20px 24px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.page-head h2 { margin: 0; font-size: 18px; color: #1f2937; }
.page-head p { margin: 6px 0 0; font-size: 13px; color: #6b7280; }
</style>
