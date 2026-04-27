import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router/index'

// 1. Создаем инстанс приложения
const app = createApp(App)

// 2. Подключаем роутер к приложению
app.use(router)

// 3. Монтируем приложение
app.mount('#app')