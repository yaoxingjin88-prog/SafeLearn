<template>
  <div class="sl-page skill-tree-page">
    <div class="sl-page-header sl-page-header--stacked">
      <div>
        <h2 class="sl-page-title">安全进阶路径</h2>
        <p class="sl-page-desc">按阶段解锁课程，构建完整的安全知识体系</p>
      </div>
      <el-button @click="router.push(p('/courses/list'))">
        <el-icon><ArrowLeft /></el-icon>
        返回课程列表
      </el-button>
    </div>

    <!-- 进度概览 -->
    <div class="progress-overview sl-page-section">
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="progress-card basic">
            <div class="progress-icon">
              <el-icon><Reading /></el-icon>
            </div>
            <div class="progress-info">
              <div class="progress-title">初级 - 理论与认知</div>
              <div class="progress-count">
                {{ getCompletedCount(DifficultyLevel.BASIC) }} / {{ getTotalCount(DifficultyLevel.BASIC) }} 完成
              </div>
              <el-progress :percentage="getProgressPercentage(DifficultyLevel.BASIC)" :color="'#67C23A'" />
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="progress-card intermediate">
            <div class="progress-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="progress-info">
              <div class="progress-title">中级 - 案例与分析</div>
              <div class="progress-count">
                {{ getCompletedCount(DifficultyLevel.INTERMEDIATE) }} / {{ getTotalCount(DifficultyLevel.INTERMEDIATE) }} 完成
              </div>
              <el-progress :percentage="getProgressPercentage(DifficultyLevel.INTERMEDIATE)" :color="'#E6A23C'" />
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="progress-card advanced">
            <div class="progress-icon">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="progress-info">
              <div class="progress-title">高级 - 动态与实操</div>
              <div class="progress-count">
                {{ getCompletedCount(DifficultyLevel.ADVANCED) }} / {{ getTotalCount(DifficultyLevel.ADVANCED) }} 完成
              </div>
              <el-progress :percentage="getProgressPercentage(DifficultyLevel.ADVANCED)" :color="'#F56C6C'" />
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 技能树 -->
    <el-card v-loading="loading" class="skill-tree-card">
      <div class="skill-tree-container">
        <!-- 初级层级 -->
        <div class="level-section">
          <div class="level-header">
            <el-tag type="success" size="large" effect="dark">初级 - 理论与认知</el-tag>
            <span class="level-desc">图文、视频、规范解读</span>
          </div>
          <div class="nodes-row">
            <div
              v-for="node in skillTreeData.levels.BASIC"
              :key="node.id"
              class="skill-node"
              :class="{
                'unlocked': node.unlocked,
                'locked': !node.unlocked,
                'completed': node.completed
              }"
              @click="handleNodeClick(node)"
            >
              <div class="node-icon">
                <el-icon v-if="node.completed"><Check /></el-icon>
                <el-icon v-else-if="node.unlocked"><Reading /></el-icon>
                <el-icon v-else><Lock /></el-icon>
              </div>
              <div class="node-content">
                <div class="node-title">{{ node.title }}</div>
                <div class="node-course">{{ node.courseTitle }}</div>
                <div class="node-progress" v-if="node.unlocked">
                  <el-progress
                    :percentage="node.progress"
                    :status="node.completed ? 'success' : ''"
                    :stroke-width="4"
                  />
                </div>
              </div>
              <div class="node-status">
                <el-tag
                  v-if="node.completed"
                  type="success"
                  size="small"
                  effect="plain"
                >已完成</el-tag>
                <el-tag
                  v-else-if="node.unlocked"
                  type="primary"
                  size="small"
                  effect="plain"
                >进行中</el-tag>
                <el-tag
                  v-else
                  type="info"
                  size="small"
                  effect="plain"
                >未解锁</el-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 连接线 -->
        <div class="connection-lines">
          <svg class="connections-svg">
            <defs>
              <marker
                id="arrowhead"
                markerWidth="10"
                markerHeight="7"
                refX="9"
                refY="3.5"
                orient="auto"
              >
                <polygon points="0 0, 10 3.5, 0 7" fill="#909399" />
              </marker>
              <marker
                id="arrowhead-active"
                markerWidth="10"
                markerHeight="7"
                refX="9"
                refY="3.5"
                orient="auto"
              >
                <polygon points="0 0, 10 3.5, 0 7" fill="#67C23A" />
              </marker>
            </defs>
            <line
              v-for="(conn, index) in visibleConnections"
              :key="index"
              :x1="conn.x1"
              :y1="conn.y1"
              :x2="conn.x2"
              :y2="conn.y2"
              :stroke="conn.active ? '#67C23A' : '#dcdfe6'"
              :stroke-width="conn.active ? 3 : 2"
              :marker-end="conn.active ? 'url(#arrowhead-active)' : 'url(#arrowhead)'"
              :class="{ 'active-line': conn.active }"
            />
          </svg>
        </div>

        <!-- 中级层级 -->
        <div class="level-section">
          <div class="level-header">
            <el-tag type="warning" size="large" effect="dark">中级 - 案例与分析</el-tag>
            <span class="level-desc">事故案例、隐患排查、违规推演</span>
          </div>
          <div class="nodes-row">
            <div
              v-for="node in skillTreeData.levels.INTERMEDIATE"
              :key="node.id"
              class="skill-node"
              :class="{
                'unlocked': node.unlocked,
                'locked': !node.unlocked,
                'completed': node.completed
              }"
              @click="handleNodeClick(node)"
            >
              <div class="node-icon">
                <el-icon v-if="node.completed"><Check /></el-icon>
                <el-icon v-else-if="node.unlocked"><DataAnalysis /></el-icon>
                <el-icon v-else><Lock /></el-icon>
              </div>
              <div class="node-content">
                <div class="node-title">{{ node.title }}</div>
                <div class="node-course">{{ node.courseTitle }}</div>
                <div class="node-progress" v-if="node.unlocked">
                  <el-progress
                    :percentage="node.progress"
                    :status="node.completed ? 'success' : ''"
                    :stroke-width="4"
                  />
                </div>
              </div>
              <div class="node-status">
                <el-tag
                  v-if="node.completed"
                  type="success"
                  size="small"
                  effect="plain"
                >已完成</el-tag>
                <el-tag
                  v-else-if="node.unlocked"
                  type="primary"
                  size="small"
                  effect="plain"
                >进行中</el-tag>
                <el-tag
                  v-else
                  type="info"
                  size="small"
                  effect="plain"
                >未解锁</el-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 高级层级 -->
        <div class="level-section">
          <div class="level-header">
            <el-tag type="danger" size="large" effect="dark">高级 - 动态与实操</el-tag>
            <span class="level-desc">3D模拟、应急决策、BatteryScene</span>
          </div>
          <div class="nodes-row">
            <div
              v-for="node in skillTreeData.levels.ADVANCED"
              :key="node.id"
              class="skill-node"
              :class="{
                'unlocked': node.unlocked,
                'locked': !node.unlocked,
                'completed': node.completed
              }"
              @click="handleNodeClick(node)"
            >
              <div class="node-icon">
                <el-icon v-if="node.completed"><Check /></el-icon>
                <el-icon v-else-if="node.unlocked"><Cpu /></el-icon>
                <el-icon v-else><Lock /></el-icon>
              </div>
              <div class="node-content">
                <div class="node-title">{{ node.title }}</div>
                <div class="node-course">{{ node.courseTitle }}</div>
                <div class="node-progress" v-if="node.unlocked">
                  <el-progress
                    :percentage="node.progress"
                    :status="node.completed ? 'success' : ''"
                    :stroke-width="4"
                  />
                </div>
              </div>
              <div class="node-status">
                <el-tag
                  v-if="node.completed"
                  type="success"
                  size="small"
                  effect="plain"
                >已完成</el-tag>
                <el-tag
                  v-else-if="node.unlocked"
                  type="primary"
                  size="small"
                  effect="plain"
                >进行中</el-tag>
                <el-tag
                  v-else
                  type="info"
                  size="small"
                  effect="plain"
                >未解锁</el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 解锁要求弹窗 -->
    <el-dialog
      v-model="unlockDialogVisible"
      title="解锁要求"
      width="500px"
    >
      <div v-if="selectedNode">
        <h4 class="mb-4">{{ selectedNode.title }}</h4>
        <p class="text-gray-500 mb-4">完成以下前置章节即可解锁：</p>
        <div
          v-for="req in selectedNode.prerequisiteIds"
          :key="req"
          class="requirement-item"
        >
          <el-icon v-if="isRequirementMet(req)"><CircleCheck class="text-green-500" /></el-icon>
          <el-icon v-else><CircleClose class="text-gray-400" /></el-icon>
          <span class="ml-2">{{ getChapterTitle(req) }}</span>
          <el-tag
            v-if="isRequirementMet(req)"
            type="success"
            size="small"
            class="ml-auto"
          >已完成</el-tag>
          <el-tag
            v-else
            type="info"
            size="small"
            class="ml-auto"
          >未完成</el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft,
  Reading,
  DataAnalysis,
  Cpu,
  Check,
  Lock,
  CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'
import type { SkillTreeData, SkillTreeNode } from '@/types'
import { DifficultyLevel } from '@/types'

const router = useRouter()
const { p } = useAppBase()
const loading = ref(false)
const skillTreeData = ref<SkillTreeData>({
  levels: { BASIC: [], INTERMEDIATE: [], ADVANCED: [] },
  connections: []
})
const unlockDialogVisible = ref(false)
const selectedNode = ref<SkillTreeNode | null>(null)

// 所有节点的映射
const allNodes = computed(() => {
  return [
    ...skillTreeData.value.levels.BASIC,
    ...skillTreeData.value.levels.INTERMEDIATE,
    ...skillTreeData.value.levels.ADVANCED
  ]
})

// 可见的连接线
const visibleConnections = computed(() => {
  return skillTreeData.value.connections.map(conn => {
    const fromNode = allNodes.value.find(n => n.id === conn.from)
    const toNode = allNodes.value.find(n => n.id === conn.to)
    const active = fromNode?.completed && toNode?.unlocked
    return { ...conn, active, x1: 0, y1: 0, x2: 0, y2: 0 }
  })
})

onMounted(async () => {
  await loadSkillTree()
  nextTick(() => {
    calculateConnections()
  })
})

async function loadSkillTree() {
  loading.value = true
  try {
    const res = await request.get('/skill-tree')
    skillTreeData.value = res.data
  } catch (error) {
    console.error('加载技能树失败', error)
    ElMessage.error('加载技能树失败')
  } finally {
    loading.value = false
  }
}

function calculateConnections() {
  // 简化版：实际项目中需要根据DOM位置计算连线
  // 这里仅作示意
}

function handleNodeClick(node: SkillTreeNode) {
  if (node.unlocked) {
    router.push(p(`/courses/${node.courseId}`))
  } else {
    selectedNode.value = node
    unlockDialogVisible.value = true
  }
}

function isRequirementMet(chapterId: string): boolean {
  const chapter = allNodes.value.find(n => n.id === chapterId)
  return chapter?.completed || false
}

function getChapterTitle(chapterId: string): string {
  const chapter = allNodes.value.find(n => n.id === chapterId)
  return chapter?.title || '未知章节'
}

function getCompletedCount(level: DifficultyLevel): number {
  return skillTreeData.value.levels[level].filter(n => n.completed).length
}

function getTotalCount(level: DifficultyLevel): number {
  return skillTreeData.value.levels[level].length
}

function getProgressPercentage(level: DifficultyLevel): number {
  const total = getTotalCount(level)
  if (total === 0) return 0
  return Math.round((getCompletedCount(level) / total) * 100)
}
</script>

<style scoped>
.progress-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.progress-card.basic {
  border-left: 4px solid #67C23A;
}

.progress-card.intermediate {
  border-left: 4px solid #E6A23C;
}

.progress-card.advanced {
  border-left: 4px solid #F56C6C;
}

.progress-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
}

.progress-card.basic .progress-icon {
  background: #f0f9eb;
  color: #67C23A;
}

.progress-card.intermediate .progress-icon {
  background: #fdf6ec;
  color: #E6A23C;
}

.progress-card.advanced .progress-icon {
  background: #fef0f0;
  color: #F56C6C;
}

.progress-info {
  flex: 1;
}

.progress-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.progress-count {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}

.skill-tree-card {
  min-height: 600px;
}

.skill-tree-container {
  position: relative;
  padding: 20px;
}

.level-section {
  margin-bottom: 48px;
}

.level-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.level-desc {
  font-size: 13px;
  color: #6b7280;
}

.nodes-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.skill-node {
  display: flex;
  align-items: center;
  width: 320px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  border: 2px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.3s;
}

.skill-node:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.skill-node.unlocked {
  border-color: #409eff;
  background: #f5f7fa;
}

.skill-node.completed {
  border-color: #67c23a;
  background: #f0f9eb;
}

.skill-node.locked {
  opacity: 0.6;
  cursor: not-allowed;
}

.node-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-right: 12px;
  background: #f3f4f6;
  color: #6b7280;
}

.skill-node.unlocked .node-icon {
  background: #ecf5ff;
  color: #409eff;
}

.skill-node.completed .node-icon {
  background: #e1f3d8;
  color: #67c23a;
}

.node-content {
  flex: 1;
  min-width: 0;
}

.node-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-course {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}

.node-progress {
  width: 100%;
}

.node-status {
  margin-left: 12px;
}

.connection-lines {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.connections-svg {
  width: 100%;
  height: 100%;
}

.active-line {
  animation: dash 1s linear infinite;
}

@keyframes dash {
  to {
    stroke-dashoffset: -20;
  }
}

.requirement-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 8px;
}
</style>
