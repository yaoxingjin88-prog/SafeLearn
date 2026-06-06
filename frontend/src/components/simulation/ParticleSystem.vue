<template>
  <canvas ref="canvasRef" class="particle-canvas" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import type { BatteryCellState } from '@/composables/useSimulation'

const props = defineProps<{
  cells: BatteryCellState[]
  width?: number
  height?: number
}>()

const canvasRef = ref<HTMLCanvasElement>()
let ctx: CanvasRenderingContext2D | null = null
let animId: number

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  life: number
  maxLife: number
  size: number
  alpha: number
  type: 'smoke' | 'fire'
  color: string
}

const particles: Particle[] = []

const smokeColors = ['rgba(100,100,100,', 'rgba(80,80,80,', 'rgba(120,120,120,']
const fireColors = ['rgba(255,100,0,', 'rgba(255,60,0,', 'rgba(255,200,0,', 'rgba(255,150,0,']

function emitParticles() {
  const w = props.width || 400
  const h = props.height || 300
  const cols = 4

  props.cells.forEach((cell, i) => {
    const row = Math.floor(i / cols)
    const col = i % cols
    const cx = (col + 0.5) * (w / cols)
    const cy = (row + 0.5) * (h / Math.ceil(props.cells.length / cols))

    if (cell.smokeIntensity > 0 && Math.random() < cell.smokeIntensity * 0.4) {
      const color = smokeColors[Math.floor(Math.random() * smokeColors.length)]
      particles.push({
        x: cx + (Math.random() - 0.5) * 20,
        y: cy,
        vx: (Math.random() - 0.5) * 0.5,
        vy: -0.5 - Math.random() * 1.5,
        life: 0,
        maxLife: 60 + Math.random() * 40,
        size: 3 + Math.random() * 5,
        alpha: 0.6 * cell.smokeIntensity,
        type: 'smoke',
        color,
      })
    }

    if (cell.fireIntensity > 0 && Math.random() < cell.fireIntensity * 0.6) {
      const color = fireColors[Math.floor(Math.random() * fireColors.length)]
      particles.push({
        x: cx + (Math.random() - 0.5) * 16,
        y: cy + (Math.random() - 0.5) * 10,
        vx: (Math.random() - 0.5) * 1,
        vy: -1 - Math.random() * 2,
        life: 0,
        maxLife: 20 + Math.random() * 20,
        size: 2 + Math.random() * 4,
        alpha: 0.8 * cell.fireIntensity,
        type: 'fire',
        color,
      })
    }
  })
}

function animate() {
  if (!ctx || !canvasRef.value) return
  const w = canvasRef.value.width
  const h = canvasRef.value.height

  ctx.clearRect(0, 0, w, h)

  emitParticles()

  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i]
    p.life++
    p.x += p.vx
    p.y += p.vy
    p.alpha *= 0.98

    if (p.type === 'smoke') {
      p.size += 0.05
      p.vy *= 0.99
    } else {
      p.size *= 0.97
    }

    if (p.life >= p.maxLife || p.alpha < 0.01) {
      particles.splice(i, 1)
      continue
    }

    const lifeRatio = p.life / p.maxLife
    const a = p.alpha * (1 - lifeRatio)
    ctx.fillStyle = `${p.color}${a})`
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
    ctx.fill()
  }

  animId = requestAnimationFrame(animate)
}

onMounted(() => {
  if (!canvasRef.value) return
  canvasRef.value.width = props.width || 400
  canvasRef.value.height = props.height || 300
  ctx = canvasRef.value.getContext('2d')
  animate()
})

onUnmounted(() => {
  cancelAnimationFrame(animId)
})

watch(() => [props.width, props.height], () => {
  if (canvasRef.value) {
    canvasRef.value.width = props.width || 400
    canvasRef.value.height = props.height || 300
  }
})
</script>

<style scoped>
.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>
