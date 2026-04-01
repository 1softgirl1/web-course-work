import { createRouter, createWebHistory } from 'vue-router'
import Main from '../views/Main.vue'
import About from '../components/main/about.vue'
import Faq from "../components/main/faq.vue";
import Login from "../views/Login.vue";



const routes = [
    { path: '/', component: Main },
    { path: '/about', component: About },
    { path: '/faq', component: Faq },
    { path: '/login', component: Login },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

export default router