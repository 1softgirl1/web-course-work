import { createRouter, createWebHistory } from 'vue-router'
import Main from '../views/Main.vue'
import About from '../components/About.vue'
import Articles from "@/components/articles.vue";
import Blog from "@/components/blog.vue";
import Faq from "@/components/faq.vue";

const routes = [
    { path: '/', component: Main },
    { path: '/about', component: About },
    { path: '/articles', component: Articles },
    { path: '/blog', component: Blog},
    { path: '/faq', component: Faq },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

export default router