import { createRouter, createWebHistory } from 'vue-router'
import Main from '../views/Main.vue'
import About from '../components/main/about.vue'
import Faq from "../components/main/faq.vue";
import Login from "../views/Login.vue";
import Patient from "../views/Patient.vue";
import Doctor from "../views/Doctor.vue";
import myCard from "../components/patient/myCard.vue";
import doctorPatientCard from "../components/doctor/patientCard.vue";

import Examination from "../components/patient/examination.vue";
import uploadExamination from "../components/doctor/uploadExamination.vue";
import myPatients from "../components/doctor/myPatients.vue";
import allPatients from "../components/doctor/allPatients.vue"
import addPatient from "../components/doctor/addPatient.vue"

const routes = [
    { path: '/', component: Main },
    { path: '/about', component: About },
    { path: '/faq', component: Faq },
    { path: '/login', component: Login },

    {
        path: '/patient',
        component: Patient,
        children: [
            { path: 'myCard/:code?', component: myCard },
            { path: 'examinations', component: Examination },
        ],
    },

    {
        path: '/doctor',
        component: Doctor,
        children: [
            { path: '', redirect: '/doctor/myPatients' },
            { path: 'myPatients', component: myPatients },
            { path: 'myPatients/addPatient', component: addPatient },
            { path: 'allPatients', component: allPatients },
            { path: 'patientCard/:code', component: doctorPatientCard },
            { path: 'patientCard/:code/addExamination', component: uploadExamination }

        ],
    },

]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

export default router