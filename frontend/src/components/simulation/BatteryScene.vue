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
let instancedMesh: THREE.InstancedMesh | null = null
let rackGroup: THREE.Group | null = null
let hotspotLight: THREE.PointLight | null = null
let animationId = 0
let clock = 0
let cellCount = 0

const CELL_W = 0.8
const CELL_H = 1.6
const CELL_D = 0.4
const GAP = 0.15

const tempColors = {
  normal: new THREE.Color(0x10b981),
  warning: new THREE.Color(0xf59e0b),
  danger: new THREE.Color(0xf97316),
  critical: new THREE.Color(0xef4444),
}

const dummy = new THREE.Object3D()
const color = new THREE.Color()

function getCols(count: number) {
  if (count <= 1) return 1
  if (count <= 4) return 2
  return 4
}

function getCellPosition(index: number, cols: number) {
  const row = Math.floor(index / cols)
  const col = index % cols
  const rows = Math.ceil(cellCount / cols)
  return {
    x: (col - (cols - 1) / 2) * (CELL_W + GAP),
    y: CELL_H / 2 + 0.2,
    z: (row - (rows - 1) / 2) * (CELL_D + GAP),
  }
}

function initScene() {
  if (!container.value) return

  const w = container.value.clientWidth
  const h = container.value.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x1a1a2e)
  scene.fog = new THREE.Fog(0x1a1a2e, 15, 30)

  camera = new THREE.PerspectiveCamera(50, w / h, 0.1, 100)
  camera.position.set(6, 5, 8)
  camera.lookAt(0, CELL_H / 2, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  container.value.appendChild(renderer.domElement)

  scene.add(new THREE.AmbientLight(0x404060, 0.5))

  const dirLight = new THREE.DirectionalLight(0xffffff, 1)
  dirLight.position.set(5, 8, 5)
  dirLight.castShadow = true
  dirLight.shadow.mapSize.set(1024, 1024)
  scene.add(dirLight)

  const pointLight = new THREE.PointLight(0x4488ff, 0.4, 20)
  pointLight.position.set(-3, 3, 3)
  scene.add(pointLight)

  hotspotLight = new THREE.PointLight(0xff4400, 0, 12)
  scene.add(hotspotLight)

  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(20, 20),
    new THREE.MeshStandardMaterial({ color: 0x2a2a3e, roughness: 0.8 }),
  )
  floor.rotation.x = -Math.PI / 2
  floor.position.y = -0.01
  floor.receiveShadow = true
  scene.add(floor)

  scene.add(new THREE.GridHelper(20, 20, 0x3a3a5e, 0x3a3a5e))
}

function disposeObject3D(obj: THREE.Object3D) {
  obj.traverse(child => {
    if (child instanceof THREE.Mesh) {
      child.geometry.dispose()
      const mat = child.material
      if (Array.isArray(mat)) mat.forEach(m => m.dispose())
      else mat.dispose()
    }
  })
}

function rebuildCells() {
  if (!scene) return

  if (instancedMesh) {
    scene.remove(instancedMesh)
    instancedMesh.geometry.dispose()
    ;(instancedMesh.material as THREE.Material).dispose()
    instancedMesh = null
  }

  if (rackGroup) {
    scene.remove(rackGroup)
    disposeObject3D(rackGroup)
    rackGroup = null
  }

  cellCount = Math.max(1, props.cells.length)
  const cols = getCols(cellCount)
  const rows = Math.ceil(cellCount / cols)

  rackGroup = new THREE.Group()

  const rackGeo = new THREE.BoxGeometry(
    cols * (CELL_W + GAP) + 0.6,
    CELL_H + 0.4,
    rows * (CELL_D + GAP) + 0.4,
  )
  const rack = new THREE.Mesh(
    rackGeo,
    new THREE.MeshStandardMaterial({
      color: 0x374151,
      metalness: 0.3,
      roughness: 0.7,
      transparent: true,
      opacity: 0.25,
    }),
  )
  rack.position.y = CELL_H / 2 + 0.1
  rackGroup.add(rack)

  const edges = new THREE.LineSegments(
    new THREE.EdgesGeometry(rackGeo),
    new THREE.LineBasicMaterial({ color: 0x4a5568 }),
  )
  edges.position.copy(rack.position)
  rackGroup.add(edges)
  scene.add(rackGroup)

  const cellMat = new THREE.MeshStandardMaterial({
    color: 0xffffff,
    vertexColors: true,
    metalness: 0.25,
    roughness: 0.55,
  })

  instancedMesh = new THREE.InstancedMesh(
    new THREE.BoxGeometry(CELL_W, CELL_H, CELL_D),
    cellMat,
    cellCount,
  )
  instancedMesh.castShadow = true

  for (let i = 0; i < cellCount; i++) {
    const pos = getCellPosition(i, cols)
    dummy.position.set(pos.x, pos.y, pos.z)
    dummy.scale.set(1, 1, 1)
    dummy.updateMatrix()
    instancedMesh.setMatrixAt(i, dummy.matrix)
    instancedMesh.setColorAt(i, tempColors.normal.clone())
  }

  instancedMesh.instanceMatrix.needsUpdate = true
  if (instancedMesh.instanceColor) instancedMesh.instanceColor.needsUpdate = true
  scene.add(instancedMesh)
}

function updateCells() {
  if (!instancedMesh) return

  const cols = getCols(cellCount)
  let hottest = { index: 0, temp: 0, fire: 0 }

  props.cells.forEach((cell, i) => {
    if (i >= instancedMesh!.count) return

    const temp = cell.temperature
    if (temp < 40) color.copy(tempColors.normal)
    else if (temp < 60) color.lerpColors(tempColors.normal, tempColors.warning, (temp - 40) / 20)
    else if (temp < 100) color.lerpColors(tempColors.warning, tempColors.danger, (temp - 60) / 40)
    else color.lerpColors(tempColors.danger, tempColors.critical, Math.min(1, (temp - 100) / 100))

    if (cell.fireIntensity > 0.2) {
      const flicker = 0.85 + Math.sin(clock * 12 + i) * 0.15
      color.multiplyScalar(flicker)
    }

    instancedMesh!.setColorAt(i, color)

    const pulse = 1 + cell.fireIntensity * 0.12 * (1 + Math.sin(clock * 6 + i * 0.5) * 0.4)
    const pos = getCellPosition(i, cols)
    dummy.position.set(pos.x, pos.y + cell.fireIntensity * 0.08, pos.z)
    dummy.scale.set(pulse, pulse, pulse)
    dummy.updateMatrix()
    instancedMesh!.setMatrixAt(i, dummy.matrix)

    if (temp > hottest.temp) hottest = { index: i, temp, fire: cell.fireIntensity }
  })

  instancedMesh.instanceMatrix.needsUpdate = true
  if (instancedMesh.instanceColor) instancedMesh.instanceColor.needsUpdate = true

  if (hotspotLight) {
    const pos = getCellPosition(hottest.index, cols)
    hotspotLight.position.set(pos.x, pos.y + CELL_H * 0.6, pos.z)
    const heat = Math.max(0, (hottest.temp - 40) / 160)
    hotspotLight.intensity = heat * 3 + hottest.fire * 4
    hotspotLight.color.setHSL(0.05 + heat * 0.05, 1, 0.45 + heat * 0.2)
  }
}

function animate() {
  animationId = requestAnimationFrame(animate)
  clock += 0.016

  const radius = 7.5
  const height = 4.5 + Math.sin(clock * 0.4) * 0.3
  camera.position.x = Math.cos(clock * 0.25) * radius
  camera.position.z = Math.sin(clock * 0.25) * radius
  camera.position.y = height
  camera.lookAt(0, CELL_H / 2, 0)

  updateCells()
  renderer.render(scene, camera)
}

function onResize() {
  if (!container.value || !camera || !renderer) return
  const w = container.value.clientWidth
  const h = container.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

onMounted(() => {
  initScene()
  rebuildCells()
  animate()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', onResize)
  renderer?.dispose()
})

watch(() => props.cells.length, () => {
  rebuildCells()
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
