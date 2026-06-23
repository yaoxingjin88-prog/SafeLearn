<template>
  <button type="button" class="action-card" :class="`tone-${tone}`" :title="`${badge} · ${meta}`" @click="$emit('click')">
    <div class="action-head">
      <span class="action-icon-box"><el-icon :size="18"><component :is="icon" /></el-icon></span>
      <el-icon class="action-arrow" :size="14"><ArrowRight /></el-icon>
    </div>
    <span class="action-title">{{ title }}</span>
    <span class="action-desc">{{ desc }}</span>
    <span class="action-meta">{{ meta }}</span>
  </button>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'

defineProps<{
  title: string
  desc: string
  meta: string
  badge: string
  icon: Component
  tone: 'blue' | 'green' | 'orange' | 'purple'
}>()

defineEmits<{ click: [] }>()
</script>

<style scoped>
.action-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
  min-width: 0;
  padding: 12px;
  border: 1px solid #e9edf3;
  border-radius: 10px;
  background: #fbfcfd;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

.action-card:hover {
  transform: translateY(-1px);
  border-color: var(--tone-border);
  background: #fff;
}

.tone-blue { --tone-border: #3b82f6; --tone: #2563eb; --tone-bg: #eff6ff; }
.tone-green { --tone-border: #10b981; --tone: #059669; --tone-bg: #ecfdf5; }
.tone-orange { --tone-border: #f59e0b; --tone: #d97706; --tone-bg: #fffbeb; }
.tone-purple { --tone-border: #8b5cf6; --tone: #7c3aed; --tone-bg: #f5f3ff; }

.action-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 9px;
}

.action-icon-box {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  background: var(--tone-bg);
  color: var(--tone);
}

.action-arrow {
  color: #b0b8c5;
  transition: color 0.2s ease, transform 0.2s ease;
}

.action-card:hover .action-arrow {
  color: var(--tone);
  transform: translateX(2px);
}

.action-title {
  color: #303b4c;
  font-size: 12px;
  font-weight: 700;
}

.action-desc {
  width: 100%;
  margin-top: 2px;
  overflow: hidden;
  color: #949eac;
  font-size: 10px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-meta {
  width: 100%;
  margin-top: 8px;
  padding-top: 7px;
  overflow: hidden;
  border-top: 1px solid #edf0f4;
  color: #7f8999;
  font-size: 9px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
