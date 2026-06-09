<template>
  <el-button
    :type="favorited ? 'warning' : 'default'"
    :link="link"
    :size="size"
    :loading="loading"
    @click.stop="toggle"
  >
    <el-icon class="mr-1"><StarFilled v-if="favorited" /><Star v-else /></el-icon>
    {{ favorited ? '已收藏' : '收藏' }}
  </el-button>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { favoriteApi } from '@/api/favorite'

const props = withDefaults(defineProps<{
  targetType: 'course' | 'case'
  targetId: string
  link?: boolean
  size?: 'small' | 'default' | 'large'
}>(), { link: true, size: 'small' })

const favorited = ref(false)
const loading = ref(false)

async function load() {
  if (!props.targetId) return
  try {
    const res = await favoriteApi.check(props.targetType, props.targetId)
    favorited.value = res.data.favorited
  } catch { /* ignore */ }
}

async function toggle() {
  loading.value = true
  try {
    const res = await favoriteApi.toggle(props.targetType, props.targetId)
    favorited.value = res.data.favorited
    ElMessage.success(favorited.value ? '已加入收藏' : '已取消收藏')
  } catch {
    ElMessage.error('操作失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => props.targetId, load)
</script>
