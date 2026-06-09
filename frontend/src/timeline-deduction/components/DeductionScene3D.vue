<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as THREE from 'three'
import type { SceneVisualState, TimelinePhase } from '../types'

const props = defineProps<{
  scene: SceneVisualState
  phase: TimelinePhase
}>()

const mountRef = ref<HTMLDivElement | null>(null)

let renderer: THREE.WebGLRenderer
let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let animationId = 0
const containers: THREE.Mesh[] = []
let smokePoints: THREE.Points
let fireLight: THREE.PointLight
let explosionLight: THREE.PointLight

function heatColor(heat: number) {
  const c = new THREE.Color()
  c.setHSL(0.08 - heat * 0.08, 1, 0.35 + heat * 0.25)
  return c
}

function init() {
  const el = mountRef.value!
  const w = el.clientWidth
  const h = el.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0a0e17)
  scene.fog = new THREE.Fog(0x0a0e17, 18, 55)

  camera = new THREE.PerspectiveCamera(48, w / h, 0.1, 100)
  camera.position.set(8, 6, 12)
  camera.lookAt(0, 1.2, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  el.appendChild(renderer.domElement)

  const ambient = new THREE.AmbientLight(0x445566, 0.6)
  scene.add(ambient)

  const dir = new THREE.DirectionalLight(0xffffff, 0.85)
  dir.position.set(5, 12, 8)
  scene.add(dir)

  fireLight = new THREE.PointLight(0xff4400, 0, 20)
  fireLight.position.set(-2, 2.5, 0)
  scene.add(fireLight)

  explosionLight = new THREE.PointLight(0xffaa00, 0, 40)
  explosionLight.position.set(0, 3, 0)
  scene.add(explosionLight)

  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(30, 20),
    new THREE.MeshStandardMaterial({ color: 0x1a2332, metalness: 0.2, roughness: 0.85 }),
  )
  floor.rotation.x = -Math.PI / 2
  scene.add(floor)

  const grid = new THREE.GridHelper(30, 30, 0x2a3a50, 0x1e2a3a)
  scene.add(grid)

  const boxGeo = new THREE.BoxGeometry(2.2, 2.4, 1.4)
  const positions = [-4.5, -1.5, 1.5, 4.5]
  positions.forEach((x, i) => {
    const mat = new THREE.MeshStandardMaterial({
      color: heatColor(0.1),
      metalness: 0.45,
      roughness: 0.55,
    })
    const mesh = new THREE.Mesh(boxGeo, mat)
    mesh.position.set(x, 1.2, 0)
    mesh.userData.index = i
    scene.add(mesh)
    containers.push(mesh)

    const label = new THREE.Mesh(
      new THREE.BoxGeometry(0.15, 0.15, 0.15),
      new THREE.MeshBasicMaterial({ color: 0x00e5ff }),
    )
    label.position.set(x, 2.55, 0.75)
    scene.add(label)
  })

  const pcs = new THREE.Mesh(
    new THREE.BoxGeometry(1.8, 1.6, 1),
    new THREE.MeshStandardMaterial({ color: 0x3d5a80, metalness: 0.5, roughness: 0.4 }),
  )
  pcs.position.set(0, 0.8, -3.5)
  scene.add(pcs)

  const smokeGeo = new THREE.BufferGeometry()
  const smokeCount = 400
  const positions3 = new Float32Array(smokeCount * 3)
  smokeGeo.setAttribute('position', new THREE.BufferAttribute(positions3, 3))
  smokePoints = new THREE.Points(
    smokeGeo,
    new THREE.PointsMaterial({ color: 0x888888, size: 0.12, transparent: true, opacity: 0.35 }),
  )
  scene.add(smokePoints)

  const animate = () => {
    animationId = requestAnimationFrame(animate)
    camera.position.x = 8 + Math.sin(Date.now() * 0.00025) * 0.6
    camera.lookAt(0, 1.2, 0)
    renderer.render(scene, camera)
  }
  animate()
}

function applyVisuals() {
  if (!containers.length) return
  const s = props.scene
  s.containerHeat.forEach((heat, i) => {
    const mat = containers[i]?.material as THREE.MeshStandardMaterial
    if (mat) mat.color = heatColor(heat)
  })

  fireLight.intensity = s.fireLevel * 3.5
  explosionLight.intensity = s.explosionFlash * 8

  const posAttr = smokePoints.geometry.getAttribute('position') as THREE.BufferAttribute
  const smokeN = s.smokeLevel
  for (let i = 0; i < posAttr.count; i++) {
    if (Math.random() > smokeN) {
      posAttr.setXYZ(i, 0, -10, 0)
      continue
    }
    const x = -2 + (Math.random() - 0.5) * 3
    const y = 1 + Math.random() * (2 + smokeN * 3)
    const z = (Math.random() - 0.5) * 2
    posAttr.setXYZ(i, x, y, z)
  }
  posAttr.needsUpdate = true
  ;(smokePoints.material as THREE.PointsMaterial).opacity = 0.15 + smokeN * 0.55
}

watch(() => props.scene, applyVisuals, { deep: true })
watch(() => props.phase, applyVisuals)

function onResize() {
  const el = mountRef.value
  if (!el || !renderer) return
  const w = el.clientWidth
  const h = el.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

onMounted(() => {
  init()
  applyVisuals()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', onResize)
  renderer?.dispose()
})
</script>

<template>
  <div ref="mountRef" class="scene3d" />
</template>

<style scoped>
.scene3d {
  width: 100%;
  height: 100%;
  min-height: 320px;
  border-radius: 12px;
  overflow: hidden;
  background: #0a0e17;
}
</style>
