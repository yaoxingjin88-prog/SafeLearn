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

const CELL_W = 0.9
const CELL_H = 1.8
const CELL_D = 0.45
const GAP = 0.15

let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let rackGroup: THREE.Group | null = null
let cellMeshes: THREE.Mesh[] = []
let instancedMesh: THREE.InstancedMesh | null = null
let hotspotLight: THREE.PointLight | null = null
let fillLight: THREE.PointLight | null = null
let alertRing: THREE.Mesh | null = null
let heatSprite: THREE.Sprite | null = null
let smokePoints: THREE.Points | null = null
let firePoints: THREE.Points | null = null
let statusLed: THREE.Mesh | null = null
let animationId = 0
let clock = 0
let cellCount = 0
let isSingleCell = false

const color = new THREE.Color()
const emissive = new THREE.Color()

interface FxParticle {
  x: number
  y: number
  z: number
  vx: number
  vy: number
  vz: number
  life: number
  maxLife: number
}

const smokePool: FxParticle[] = []
const firePool: FxParticle[] = []
const SMOKE_COUNT = 400
const FIRE_COUNT = 200

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
    y: CELL_H / 2 + 0.25,
    z: (row - (rows - 1) / 2) * (CELL_D + GAP),
  }
}

function tempToColors(temp: number, status: string) {
  if (temp < 40) {
    color.setHex(0x38bdf8)
    emissive.setHex(0x0ea5e9)
  } else if (temp < 60) {
    const t = (temp - 40) / 20
    color.lerpColors(new THREE.Color(0x38bdf8), new THREE.Color(0xfbbf24), t)
    emissive.lerpColors(new THREE.Color(0x0ea5e9), new THREE.Color(0xf59e0b), t)
  } else if (temp < 100) {
    const t = (temp - 60) / 40
    color.lerpColors(new THREE.Color(0xfbbf24), new THREE.Color(0xf97316), t)
    emissive.lerpColors(new THREE.Color(0xf59e0b), new THREE.Color(0xea580c), t)
  } else if (temp < 200) {
    const t = Math.min(1, (temp - 100) / 100)
    color.lerpColors(new THREE.Color(0xf97316), new THREE.Color(0xef4444), t)
    emissive.lerpColors(new THREE.Color(0xea580c), new THREE.Color(0xdc2626), t)
  } else {
    color.setHex(0xff2200)
    emissive.setHex(0xff4400)
  }
  if (status === 'critical') {
    emissive.multiplyScalar(1.4 + Math.sin(clock * 10) * 0.3)
  }
}

function initScene() {
  if (!container.value) return

  const w = container.value.clientWidth
  const h = container.value.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0c1222)
  scene.fog = new THREE.FogExp2(0x0c1222, 0.045)

  camera = new THREE.PerspectiveCamera(42, w / h, 0.1, 100)
  camera.position.set(4.2, 3.2, 5.5)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.15
  container.value.appendChild(renderer.domElement)

  scene.add(new THREE.AmbientLight(0x6080b0, 0.65))

  const dirLight = new THREE.DirectionalLight(0xffffff, 1.2)
  dirLight.position.set(6, 10, 4)
  dirLight.castShadow = true
  dirLight.shadow.mapSize.set(2048, 2048)
  scene.add(dirLight)

  const rimLight = new THREE.DirectionalLight(0x60a5fa, 0.45)
  rimLight.position.set(-5, 3, -4)
  scene.add(rimLight)

  fillLight = new THREE.PointLight(0x3b82f6, 0.5, 15)
  fillLight.position.set(-2, 2, 3)
  scene.add(fillLight)

  hotspotLight = new THREE.PointLight(0xff5500, 0, 10)
  scene.add(hotspotLight)

  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(24, 24),
    new THREE.MeshStandardMaterial({ color: 0x1e293b, roughness: 0.85, metalness: 0.15 }),
  )
  floor.rotation.x = -Math.PI / 2
  floor.receiveShadow = true
  scene.add(floor)

  const grid = new THREE.GridHelper(24, 24, 0x334155, 0x1e293b)
  scene.add(grid)

  initFxSystems()
}

function initFxSystems() {
  const smokeGeo = new THREE.BufferGeometry()
  const smokePos = new Float32Array(SMOKE_COUNT * 3)
  smokeGeo.setAttribute('position', new THREE.BufferAttribute(smokePos, 3))
  smokePoints = new THREE.Points(
    smokeGeo,
    new THREE.PointsMaterial({
      size: 0.18,
      color: 0xaaaaaa,
      transparent: true,
      opacity: 0.55,
      depthWrite: false,
      blending: THREE.NormalBlending,
      sizeAttenuation: true,
    }),
  )
  scene.add(smokePoints)

  const fireGeo = new THREE.BufferGeometry()
  const firePos = new Float32Array(FIRE_COUNT * 3)
  fireGeo.setAttribute('position', new THREE.BufferAttribute(firePos, 3))
  firePoints = new THREE.Points(
    fireGeo,
    new THREE.PointsMaterial({
      size: 0.22,
      color: 0xff6600,
      transparent: true,
      opacity: 0.85,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
      sizeAttenuation: true,
    }),
  )
  scene.add(firePoints)

  for (let i = 0; i < SMOKE_COUNT; i++) {
    smokePool.push({ x: 0, y: -99, z: 0, vx: 0, vy: 0, vz: 0, life: 0, maxLife: 1 })
  }
  for (let i = 0; i < FIRE_COUNT; i++) {
    firePool.push({ x: 0, y: -99, z: 0, vx: 0, vy: 0, vz: 0, life: 0, maxLife: 1 })
  }

  const heatTex = createHeatSpriteTexture()
  heatSprite = new THREE.Sprite(
    new THREE.SpriteMaterial({ map: heatTex, transparent: true, opacity: 0, blending: THREE.AdditiveBlending }),
  )
  heatSprite.scale.set(2.5, 2.5, 1)
  scene.add(heatSprite)
}

function createHeatSpriteTexture() {
  const c = document.createElement('canvas')
  c.width = 128
  c.height = 128
  const g = c.getContext('2d')!
  const grad = g.createRadialGradient(64, 64, 0, 64, 64, 64)
  grad.addColorStop(0, 'rgba(255,120,0,0.9)')
  grad.addColorStop(0.35, 'rgba(255,60,0,0.45)')
  grad.addColorStop(0.7, 'rgba(255,30,0,0.12)')
  grad.addColorStop(1, 'rgba(255,0,0,0)')
  g.fillStyle = grad
  g.fillRect(0, 0, 128, 128)
  return new THREE.CanvasTexture(c)
}

function disposeObject3D(obj: THREE.Object3D) {
  obj.traverse(child => {
    if (child instanceof THREE.Mesh || child instanceof THREE.Points) {
      child.geometry.dispose()
      const mat = child.material
      if (Array.isArray(mat)) mat.forEach(m => m.dispose())
      else mat.dispose()
    }
  })
}

function buildSingleCellCabinet() {
  const g = new THREE.Group()

  const cabW = 2.4
  const cabH = 2.6
  const cabD = 1.4

  const shell = new THREE.Mesh(
    new THREE.BoxGeometry(cabW, cabH, cabD),
    new THREE.MeshStandardMaterial({
      color: 0x475569,
      metalness: 0.55,
      roughness: 0.45,
      transparent: true,
      opacity: 0.35,
    }),
  )
  shell.position.y = cabH / 2
  g.add(shell)

  const shellEdge = new THREE.LineSegments(
    new THREE.EdgesGeometry(new THREE.BoxGeometry(cabW, cabH, cabD)),
    new THREE.LineBasicMaterial({ color: 0x94a3b8 }),
  )
  shellEdge.position.y = cabH / 2
  g.add(shellEdge)

  const innerFloor = new THREE.Mesh(
    new THREE.PlaneGeometry(cabW - 0.2, cabD - 0.2),
    new THREE.MeshStandardMaterial({ color: 0x1e293b, roughness: 0.9 }),
  )
  innerFloor.rotation.x = -Math.PI / 2
  innerFloor.position.y = 0.15
  g.add(innerFloor)

  const batteryMat = new THREE.MeshPhysicalMaterial({
    color: 0x334155,
    metalness: 0.75,
    roughness: 0.28,
    emissive: 0x0ea5e9,
    emissiveIntensity: 0.35,
    clearcoat: 0.4,
    clearcoatRoughness: 0.2,
  })
  const battery = new THREE.Mesh(new THREE.BoxGeometry(CELL_W, CELL_H, CELL_D), batteryMat)
  battery.position.set(0, CELL_H / 2 + 0.25, 0)
  battery.castShadow = true
  g.add(battery)
  cellMeshes = [battery]

  const capGeo = new THREE.CylinderGeometry(0.06, 0.06, 0.12, 16)
  const capMat = new THREE.MeshStandardMaterial({ color: 0xcbd5e1, metalness: 0.9, roughness: 0.2 })
  const cap1 = new THREE.Mesh(capGeo, capMat)
  cap1.position.set(-0.2, CELL_H + 0.3, 0.1)
  const cap2 = cap1.clone()
  cap2.position.x = 0.2
  g.add(cap1, cap2)

  statusLed = new THREE.Mesh(
    new THREE.BoxGeometry(0.5, 0.08, 0.04),
    new THREE.MeshStandardMaterial({ color: 0x22c55e, emissive: 0x22c55e, emissiveIntensity: 2 }),
  )
  statusLed.position.set(0, cabH - 0.25, cabD / 2 + 0.02)
  g.add(statusLed)

  const label = new THREE.Mesh(
    new THREE.PlaneGeometry(0.7, 0.35),
    new THREE.MeshBasicMaterial({
      color: 0x0f172a,
      transparent: true,
      opacity: 0.85,
    }),
  )
  label.position.set(0, 1.2, cabD / 2 + 0.03)
  g.add(label)

  alertRing = new THREE.Mesh(
    new THREE.RingGeometry(0.55, 0.72, 48),
    new THREE.MeshBasicMaterial({
      color: 0xfbbf24,
      transparent: true,
      opacity: 0,
      side: THREE.DoubleSide,
      blending: THREE.AdditiveBlending,
    }),
  )
  alertRing.rotation.x = -Math.PI / 2
  alertRing.position.y = 0.12
  g.add(alertRing)

  const ventGeo = new THREE.BoxGeometry(0.8, 0.04, 0.25)
  const ventMat = new THREE.MeshStandardMaterial({ color: 0x64748b, metalness: 0.6, roughness: 0.5 })
  const vent = new THREE.Mesh(ventGeo, ventMat)
  vent.position.set(0, cabH - 0.08, 0)
  g.add(vent)

  return g
}

function buildMultiCellRack() {
  const g = new THREE.Group()
  const cols = getCols(cellCount)
  const rows = Math.ceil(cellCount / cols)

  const rackGeo = new THREE.BoxGeometry(
    cols * (CELL_W + GAP) + 0.6,
    CELL_H + 0.5,
    rows * (CELL_D + GAP) + 0.6,
  )
  const rack = new THREE.Mesh(
    rackGeo,
    new THREE.MeshStandardMaterial({
      color: 0x374151,
      metalness: 0.35,
      roughness: 0.65,
      transparent: true,
      opacity: 0.3,
    }),
  )
  rack.position.y = CELL_H / 2 + 0.15
  g.add(rack)

  const edges = new THREE.LineSegments(
    new THREE.EdgesGeometry(rackGeo),
    new THREE.LineBasicMaterial({ color: 0x64748b }),
  )
  edges.position.copy(rack.position)
  g.add(edges)

  cellMeshes = []
  for (let i = 0; i < cellCount; i++) {
    const mat = new THREE.MeshPhysicalMaterial({
      color: 0x334155,
      metalness: 0.7,
      roughness: 0.3,
      emissive: 0x0ea5e9,
      emissiveIntensity: 0.25,
    })
    const mesh = new THREE.Mesh(new THREE.BoxGeometry(CELL_W, CELL_H, CELL_D), mat)
    const pos = getCellPosition(i, cols)
    mesh.position.set(pos.x, pos.y, pos.z)
    mesh.castShadow = true
    g.add(mesh)
    cellMeshes.push(mesh)
  }

  return g
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

  cellMeshes = []
  alertRing = null
  statusLed = null

  cellCount = Math.max(1, props.cells.length)
  isSingleCell = cellCount === 1

  rackGroup = isSingleCell ? buildSingleCellCabinet() : buildMultiCellRack()
  scene.add(rackGroup)

  if (isSingleCell && alertRing) {
    // alertRing already in rackGroup
  }
}

function emitSmoke(x: number, y: number, z: number, intensity: number) {
  if (intensity <= 0 || Math.random() > intensity * 0.35) return
  const p = smokePool.find(s => s.life <= 0)
  if (!p) return
  p.x = x + (Math.random() - 0.5) * 0.35
  p.y = y + Math.random() * 0.1
  p.z = z + (Math.random() - 0.5) * 0.25
  p.vx = (Math.random() - 0.5) * 0.02
  p.vy = 0.025 + Math.random() * 0.04 * intensity
  p.vz = (Math.random() - 0.5) * 0.02
  p.life = 1
  p.maxLife = 80 + Math.random() * 60
}

function emitFire(x: number, y: number, z: number, intensity: number) {
  if (intensity <= 0 || Math.random() > intensity * 0.5) return
  const p = firePool.find(s => s.life <= 0)
  if (!p) return
  p.x = x + (Math.random() - 0.5) * 0.3
  p.y = y
  p.z = z + (Math.random() - 0.5) * 0.2
  p.vx = (Math.random() - 0.5) * 0.04
  p.vy = 0.04 + Math.random() * 0.06
  p.vz = (Math.random() - 0.5) * 0.04
  p.life = 1
  p.maxLife = 25 + Math.random() * 20
}

function updateFxSystems(origin: { x: number; y: number; z: number }, cell: BatteryCellState) {
  emitSmoke(origin.x, origin.y + CELL_H / 2, origin.z, cell.smokeIntensity)
  emitFire(origin.x, origin.y + CELL_H / 2 - 0.1, origin.z, cell.fireIntensity)

  const smokePos = smokePoints!.geometry.attributes.position as THREE.BufferAttribute
  smokePool.forEach((p, i) => {
    if (p.life > 0) {
      p.life++
      p.x += p.vx
      p.y += p.vy
      p.z += p.vz
      p.vx *= 0.99
      p.vy *= 0.995
      if (p.life > p.maxLife) p.life = 0
    }
    smokePos.setXYZ(i, p.x, p.y, p.z)
  })
  smokePos.needsUpdate = true
  ;(smokePoints!.material as THREE.PointsMaterial).opacity = 0.35 + cell.smokeIntensity * 0.45

  const firePos = firePoints!.geometry.attributes.position as THREE.BufferAttribute
  firePool.forEach((p, i) => {
    if (p.life > 0) {
      p.life++
      p.x += p.vx
      p.y += p.vy
      p.z += p.vz
      if (p.life > p.maxLife) p.life = 0
    }
    firePos.setXYZ(i, p.x, p.y, p.z)
  })
  firePos.needsUpdate = true
  ;(firePoints!.material as THREE.PointsMaterial).opacity = 0.4 + cell.fireIntensity * 0.55
}

function updateCells() {
  if (!cellMeshes.length) return

  const cols = getCols(cellCount)
  let hottest = { index: 0, temp: 0, fire: 0, smoke: 0, status: 'normal' as string }

  props.cells.forEach((cell, i) => {
    if (i >= cellMeshes.length) return
    const mesh = cellMeshes[i]
    const mat = mesh.material as THREE.MeshPhysicalMaterial

    tempToColors(cell.temperature, cell.status)
    const heatFactor = Math.min(1, Math.max(0, (cell.temperature - 30) / 170))
    const pulse = 1 + cell.fireIntensity * 0.08 * Math.sin(clock * 8)

    mat.color.copy(color)
    mat.emissive.copy(emissive)
    mat.emissiveIntensity = 0.2 + heatFactor * 1.8 + cell.fireIntensity * 1.2

    if (cell.fireIntensity > 0.15) {
      const flicker = 0.85 + Math.sin(clock * 14 + i) * 0.15
      mat.emissiveIntensity *= flicker
    }

    mesh.scale.set(pulse, pulse, pulse)

    const pos = isSingleCell
      ? { x: 0, y: CELL_H / 2 + 0.25, z: 0 }
      : getCellPosition(i, cols)

    if (cell.temperature > hottest.temp) {
      hottest = { index: i, temp: cell.temperature, fire: cell.fireIntensity, smoke: cell.smokeIntensity, status: cell.status }
    }

    if (i === hottest.index) {
      updateFxSystems(pos, cell)
    }
  })

  const hotPos = isSingleCell
    ? { x: 0, y: CELL_H / 2 + 0.25, z: 0 }
    : getCellPosition(hottest.index, cols)

  if (hotspotLight) {
    hotspotLight.position.set(hotPos.x, hotPos.y + CELL_H * 0.5, hotPos.z)
    const heat = Math.max(0, (hottest.temp - 35) / 165)
    hotspotLight.intensity = heat * 4 + hottest.fire * 6
    hotspotLight.color.setHSL(0.08 - heat * 0.02, 1, 0.45 + heat * 0.25)
  }

  if (heatSprite) {
    const heat = Math.max(0, (hottest.temp - 50) / 150)
    heatSprite.position.set(hotPos.x, hotPos.y + CELL_H * 0.45, hotPos.z)
    const mat = heatSprite.material as THREE.SpriteMaterial
    mat.opacity = heat * 0.65 + hottest.fire * 0.5
    const s = 1.5 + heat * 2.5 + hottest.fire
    heatSprite.scale.set(s, s, 1)
  }

  if (alertRing) {
    const warn = hottest.temp >= 55 || hottest.status !== 'normal'
    const mat = alertRing.material as THREE.MeshBasicMaterial
    mat.opacity = warn ? 0.25 + Math.sin(clock * 5) * 0.2 : 0
    if (hottest.temp >= 100 || hottest.status === 'critical') mat.color.setHex(0xef4444)
    else if (hottest.temp >= 60) mat.color.setHex(0xf97316)
    else mat.color.setHex(0xfbbf24)
    alertRing.scale.setScalar(1 + Math.sin(clock * 4) * 0.06)
  }

  if (statusLed) {
    const mat = statusLed.material as THREE.MeshStandardMaterial
    if (hottest.temp >= 100 || hottest.status === 'critical') {
      mat.color.setHex(0xef4444)
      mat.emissive.setHex(0xef4444)
    } else if (hottest.temp >= 60) {
      mat.color.setHex(0xf59e0b)
      mat.emissive.setHex(0xf59e0b)
    } else {
      mat.color.setHex(0x22c55e)
      mat.emissive.setHex(0x22c55e)
    }
    mat.emissiveIntensity = 1.5 + Math.sin(clock * 3) * 0.3
  }

  if (scene.fog) {
    const fog = scene.fog as THREE.FogExp2
    fog.density = 0.035 + hottest.smoke * 0.025
  }
}

function animate() {
  animationId = requestAnimationFrame(animate)
  clock += 0.016

  const targetY = isSingleCell ? 1.4 : CELL_H / 2 + 0.2
  const radius = isSingleCell ? 4.8 : 7.5
  const speed = isSingleCell ? 0.18 : 0.25

  camera.position.x = Math.cos(clock * speed) * radius
  camera.position.z = Math.sin(clock * speed) * radius
  camera.position.y = (isSingleCell ? 2.8 : 4.5) + Math.sin(clock * 0.35) * 0.15
  camera.lookAt(0, targetY, 0)

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

watch(() => props.cells.length, rebuildCells)
watch(() => props.cells, updateCells, { deep: true })
</script>

<style scoped>
.battery-scene {
  width: 100%;
  height: 100%;
  min-height: 400px;
}
</style>
