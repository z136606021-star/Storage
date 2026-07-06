import { createApp } from 'vue'
import { createPinia } from 'pinia'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import './style.less'

dayjs.locale('zh-cn')

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.provide('antdLocale', zhCN)

router.isReady().then(() => {
  app.mount('#app')
})
