<template>
  <div ref="container" class="battery-scene" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as THREE from 'three'
import type { BatteryCellState } from '@/composables/useSimulation'

const props = defineProps<{
  cells: BatteryCellState[]
}>()

const container = ref<HTMLElement>()

let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let instancedMesh: THREE.InstancedMesh
let animationId: number

const CELL_W = 0.8
const CELL_H = 1.6
const CELL_D = 0.4
const GAP = 0.15
const COLS = 4

const tempColors = {
  normal: new THREE.Color(0x10b981),
  warning: new THREE.Color(0xf59e0b),
  danger: new THREE.Color(0xf97316),
  critical: new THREE.Color(0xef4444),
}

const dummy = new THREE.Object3D()
const color = new THREE.Color()

function init() {
  if (!container.value) return

  const w = container.value.clientWidth
  const h = container.value.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x1a1a2e)
  scene.fog = new THREE.Fog(0x1a1a2e, 15, 30)

  camera = new THREE.PerspectiveCamera(50, w / h, 0.1, 100)
  camera.position.set(6, 5, 8)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  container.value.appendChild(renderer.domElement)

  const ambient = new THREE.AmbientLight(0x404060, 0.6)
  scene.add(ambient)

  const dirLight = new THREE.DirectionalLight(0xffffff, 1)
  dirLight.position.set(5, 8, 5)
  dirLight.castShadow = true
  dirLight.shadow.mapSize.set(1024, 1024)
  scene.add(dirLight)

  const pointLight = new THREE.PointLight(0x4488ff, 0.5, 20)
  pointLight.position.set(-3, 3, 3)
  scene.add(pointLight)

  const floorGeo = new THREE.PlaneGeometry(20, 20)
  const floorMat = new THREE.MeshStandardMaterial({
    color: 0x2a2a3e,
    roughness: 0.8,
  })
  const floor = new THREE.Mesh(floorGeo, floorMat)
  floor.rotation.x = -Math.PI / 2
  floor.position.y = -0.01
  floor.receiveShadow = true
  scene.add(floor)

  const gridHelper = new THREE.GridHelper(20, 20, 0x3a3a5e, 0x3a3a5e)
  gridHelper.position.y = 0
  scene.add(gridHelper)

  const rackGeo = new THREE.BoxGeometry(
    COLS * (CELL_W + GAP) + 0.6,
    CELL_H + 0.4,
    2 * (CELL_D + GAP) + 0.4
  )
  const rackMat = new THREE.MeshStandardMaterial({
    color: 0x374151,
    metalness: 0.3,
    roughness: 0.7,
    transparent: true,
    opacity: 0.3,
  })
  const rack = new THREE.Mesh(rackGeo, rackMat)
  rack.position.y = CELL_H / 2 + 0.1
  scene.add(rack)

  const cellGeo = new THREE.BoxGeometry(CELL_W, CELL_H, CELL_D)
  const cellMat = new THREE.MeshStandardMaterial({
    color: 0x10b981,
    metalness: 0.2,
    roughness: 0.6,
  })

  const count = props.cells.length
  instancedMesh = new THREE.InstancedMesh(cellGeo, cellMat, count)
  instancedMesh.castShadow = true

  for (let i = 0; i < count; i++) {
    const row = Math.floor(i / COLS)
    const col = i % COLS
    dummy.position.set(
      (col - (COLS - 1) / 2) * (CELL_W + GAP),
      CELL_H / 2 + 0.2,
      (row - 0.5) * (CELL_D + GAP)
    )
    dummy.updateMatrix()
    instancedMesh.setMatrixAt(i, dummy.matrix)
    instancedMesh.setColorAt(i, tempColors.normal.clone())
  }

  instancedMesh.instanceMatrix.needsUpdate = true
  if (instancedMesh.instanceColor) instancedMesh.instanceColor.needsUpdate = true
  scene.add(instancedMesh)

  const edgeGeo = new THREE.EdgesGeometry(rackGeo)
  const edgeMat = new THREE.LineBasicMaterial({ color: 0x4a5568 })
  const edges = new THREE.LineSegments(edgeGeo, edgeMat)
  edges.position.copy(rack.position)
  scene.add(edges)
}

function updateCells() {
  if (!instancedMesh) return

  props.cells.forEach((cell, i) => {
    if (i >= instancedMesh.count) return

    const temp = cell.temperature
    if (temp < 40) color.copy(tempColors.normal)
    else if (temp < 60) color.lerpColors(tempColors.normal, tempColors.warning, (temp - 40) / 20)
    else if (temp < 100) color.lerpColors(tempColors.warning, tempColors.danger, (temp - 60) / 40)
    else color.lerpColors(tempColors.danger, tempColors.critical, Math.min(1, (temp - 100) / 100))

    instancedMesh.setColorAt(i, color)

    const row = Math.floor(i / COLS)
    const col = i % COLS
    const scale = 1 + cell.fireIntensity * 0.1
    dummy.position.set(
      (col - (COLS - 1) / 2) * (CELL_W + GAP),
      CELL_H / 2 + 0.2 + cell.fireIntensity * 0.1,
      (row - 0.5) * (CELL_D + GAP)
    )
    dummy.scale.set(scale, scale, scale)
    dummy.updateMatrix()
    instancedMesh.setMatrixAt(i, dummy.matrix)
  })

  instancedMesh.instanceMatrix.needsUpdate = true
  if (instancedMesh.instanceColor) instancedMesh.instanceColor.needsUpdate = true
}

function animate() {
  animationId = requestAnimationFrame(animate)
  updateCells()
  renderer.render(scene, camera)
}

function onResize() {
  if (!container.value) return
  const w = container.value.clientWidth
  const h = container.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

onMounted(() => {
  init()
  animate()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', onResize)
  renderer?.dispose()
})

watch(() => props.cells, updateCells, { deep: true })
</script>

<style scoped>
.battery-scene {
  width: 100%;
  height: 100%;
  min-height: 400px;
}
</style>
