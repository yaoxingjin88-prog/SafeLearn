import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'virtual:uno.css'
import './styles/global.css'
import './style.css'

import App from './App.vue'
import router from './router'
import { useUserStore } from '@/stores'

const app = createApp(App)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { size: 'default' })

app.mount('#app')

// 尝试在启动时拉取用户信息，避免刷新后丢失角色/用户名
const userStore = useUserStore()
const savedToken = localStorage.getItem('token')
if (savedToken && !userStore.userInfo) {
  userStore.getUserInfo().catch(() => {
    // 忽略错误，保持未登录状态
  })
}
