<template>
  <div class="not-found">
    <div class="content">
      <h1 class="title">404</h1>
      <p class="subtitle">页面不存在</p>
      <p class="desc">抱歉，您访问的页面不存在或已被移除</p>
      <el-button type="primary" size="large" @click="$router.push('/')">
        返回首页
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.content {
  text-align: center;
  color: white;
}

.title {
  font-size: 120px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 16px;
}

.subtitle {
  font-size: 32px;
  font-weight: 600;
  margin-bottom: 12px;
}

.desc {
  font-size: 18px;
  opacity: 0.9;
  margin-bottom: 32px;
}
</style>
