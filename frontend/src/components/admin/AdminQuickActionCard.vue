<template>
  <button type="button" class="action-card" :class="`tone-${tone}`" @click="$emit('click')">
    <div class="action-corner" aria-hidden="true" />

    <div class="action-head">
      <div class="action-icon-box">
        <el-icon :size="22"><component :is="icon" /></el-icon>
      </div>
      <span class="action-badge">{{ badge }}</span>
    </div>

    <h3 class="action-title">{{ title }}</h3>
    <p class="action-desc">{{ desc }}</p>

    <div class="action-footer">
      <span class="action-meta">{{ meta }}</span>
      <span class="action-link">
        立即进入
        <el-icon :size="14"><ArrowRight /></el-icon>
      </span>
    </div>
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
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
  padding: 20px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-bottom: 4px solid var(--tone-border);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.07);
  cursor: pointer;
  text-align: left;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(31, 38, 135, 0.12);
}

.tone-blue { --tone-border: #3b82f6; --tone: #2563eb; --tone-bg: #eff6ff; }
.tone-green { --tone-border: #10b981; --tone: #059669; --tone-bg: #ecfdf5; }
.tone-orange { --tone-border: #f59e0b; --tone: #d97706; --tone-bg: #fffbeb; }
.tone-purple { --tone-border: #8b5cf6; --tone: #7c3aed; --tone-bg: #f5f3ff; }

.action-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 96px;
  height: 96px;
  border-bottom-left-radius: 100px;
  background: linear-gradient(to bottom right, color-mix(in srgb, var(--tone-bg) 30%, transparent), transparent);
  pointer-events: none;
  transition: transform 0.3s ease;
}

.action-card:hover .action-corner {
  transform: scale(1.1);
}

.action-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 16px;
}

.action-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--tone-bg);
  color: var(--tone);
  box-shadow: inset 0 2px 4px rgba(15, 23, 42, 0.04);
  transition: background 0.3s, color 0.3s;
}

.action-card:hover .action-icon-box {
  background: var(--tone);
  color: #fff;
}

.action-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--tone-bg);
  color: var(--tone);
  opacity: 0;
  transition: opacity 0.25s;
}

.action-card:hover .action-badge {
  opacity: 1;
}

.action-title {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  transition: color 0.2s;
}

.action-card:hover .action-title {
  color: var(--tone);
}

.action-desc {
  margin: 0;
  min-height: 32px;
  font-size: 12px;
  line-height: 1.5;
  color: #94a3b8;
}

.action-footer {
  width: 100%;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(241, 245, 249, 0.8);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.action-meta {
  font-size: 11px;
  font-weight: 500;
  color: #94a3b8;
}

.action-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 700;
  color: var(--tone);
  transition: gap 0.2s;
}

.action-card:hover .action-link {
  gap: 6px;
}
</style>
