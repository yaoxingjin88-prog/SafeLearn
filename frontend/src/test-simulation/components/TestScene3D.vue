<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as THREE from 'three'
import type { SceneState } from '../types'

const props = defineProps<{ scene: SceneState; riskIndex: number }>()
const mountRef = ref<HTMLDivElement | null>(null)
let renderer: THREE.WebGLRenderer
let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let animId = 0
let packMesh: THREE.Mesh
let smokePts: THREE.Points
let flashLight: THREE.PointLight

const colorMap = {
  normal: 0x22d3ee,
  warning: 0xfacc15,
  danger: 0xf97316,
  critical: 0xef4444,
}

function init() {
  const el = mountRef.value!
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x050a12)
  scene.fog = new THREE.Fog(0x050a12, 12, 40)
  camera = new THREE.PerspectiveCamera(50, el.clientWidth / el.clientHeight, 0.1, 80)
  camera.position.set(6, 5, 8)
  camera.lookAt(0, 1, 0)
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(el.clientWidth, el.clientHeight)
  el.appendChild(renderer.domElement)

  scene.add(new THREE.AmbientLight(0x1e3a5f, 0.8))
  const dl = new THREE.DirectionalLight(0x7dd3fc, 0.9)
  dl.position.set(4, 8, 6)
  scene.add(dl)
  flashLight = new THREE.PointLight(0xff6600, 0, 25)
  flashLight.position.set(0, 2, 0)
  scene.add(flashLight)

  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(20, 14),
    new THREE.MeshStandardMaterial({ color: 0x0c1929, metalness: 0.3, roughness: 0.9 }),
  )
  floor.rotation.x = -Math.PI / 2
  scene.add(floor)
  scene.add(new THREE.GridHelper(20, 20, 0x0ea5e9, 0x1e293b))

  packMesh = new THREE.Mesh(
    new THREE.BoxGeometry(2.5, 1.2, 1.6),
    new THREE.MeshStandardMaterial({ color: colorMap.normal, emissive: 0x000000, metalness: 0.6, roughness: 0.4 }),
  )
  packMesh.position.set(0, 0.6, 0)
  scene.add(packMesh)

  const rack = new THREE.Mesh(
    new THREE.BoxGeometry(1.2, 2, 0.8),
    new THREE.MeshStandardMaterial({ color: 0x334155 }),
  )
  rack.position.set(-3, 1, -2)
  scene.add(rack)

  const monitor = new THREE.Mesh(
    new THREE.BoxGeometry(1.5, 1, 0.1),
    new THREE.MeshBasicMaterial({ color: 0x0ea5e9 }),
  )
  monitor.position.set(3, 1.5, -2)
  scene.add(monitor)

  const geo = new THREE.BufferGeometry()
  const pos = new Float32Array(300)
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
  smokePts = new THREE.Points(geo, new THREE.PointsMaterial({ color: 0x94a3b8, size: 0.1, transparent: true, opacity: 0.4 }))
  scene.add(smokePts)

  const loop = () => {
    animId = requestAnimationFrame(loop)
    camera.position.x = 6 + Math.sin(Date.now() * 0.0003) * 0.4
    camera.lookAt(0, 1, 0)
    renderer.render(scene, camera)
  }
  loop()
}

function apply() {
  if (!packMesh) return
  const s = props.scene
  const mat = packMesh.material as THREE.MeshStandardMaterial
  mat.color.setHex(colorMap[s.packColor])
  mat.emissive.setHex(colorMap[s.packColor])
  mat.emissiveIntensity = s.packHeat * 0.8
  flashLight.intensity = s.flash * 6
  const attr = smokePts.geometry.getAttribute('position') as THREE.BufferAttribute
  for (let i = 0; i < attr.count; i++) {
    if (Math.random() > s.smoke) { attr.setXYZ(i, 0, -5, 0); continue }
    attr.setXYZ(i, (Math.random() - 0.5) * 2, 0.5 + Math.random() * 2, (Math.random() - 0.5) * 1.5)
  }
  attr.needsUpdate = true
}

watch(() => props.scene, apply, { deep: true })
onMounted(() => { init(); apply() })
onBeforeUnmount(() => { cancelAnimationFrame(animId); renderer?.dispose() })
</script>

<template>
  <div ref="mountRef" class="scene" />
</template>

<style scoped>
.scene { width: 100%; height: 100%; min-height: 280px; border-radius: 8px; overflow: hidden; }
</style>
