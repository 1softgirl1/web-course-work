import { createRouter, createWebHistory } from 'vue-router'
import Main from '../views/Main.vue'
import About from '../components/main/about.vue'
import Faq from "../components/main/faq.vue";
import Login from "../views/Login.vue";
import Patient from "../views/Patient.vue";
import myCard from "../components/patient/myCard.vue";

import ask from "../components/patient/ask.vue";
import Examination from "../components/patient/examination.vue";
import uploadExamination from "../components/patient/uploadExamination.vue";


const routes = [
    { path: '/', component: Main },
    { path: '/about', component: About },
    { path: '/faq', component: Faq },
    { path: '/login', component: Login },

    {
        path: '/patient',
        component: Patient,
        children: [
            { path: 'myCard', component: myCard },
            { path: 'examinations', component: Examination },
            { path: 'upload', component: uploadExamination },
            { path: 'ask', component: ask },
        ],
    },

]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

export default router