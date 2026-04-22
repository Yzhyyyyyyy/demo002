import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { initLeanCloud } from './utils/leancloud'

const app = createApp(App)

app.use(createPinia())
app.use(router)

initLeanCloud()

app.mount('#app')
