import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
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
import allDoctors from "../components/doctor/allDoctors.vue"
import doctorProfileAdmin from "../components/doctor/doctorProfileAdmin.vue"
import addPatient from "../components/doctor/addPatient.vue"
import addDoctor from "../components/doctor/addDoctor.vue"
import doctorCard from "@/components/doctor/doctorCard.vue";

const routes = [
    { path: '/', component: Main },
    { path: '/about', component: About },
    { path: '/faq', component: Faq },
    { path: '/login', component: Login, meta: { guestOnly: true } },

    {
        path: '/patient',
        component: Patient,
        meta: { requiresAuth: true, roles: ['PATIENT'] },
        children: [
            { path: '', redirect: '/patient/myCard' },
            { path: 'myCard/:code?', component: myCard },
            { path: 'examinations', component: Examination },
        ],
    },

    {
        path: '/doctor',
        component: Doctor,
        meta: { requiresAuth: true, roles: ['DOCTOR', 'DOCTOR_EXTENDED'] },
        children: [
            { path: 'myPatients', component: myPatients, meta: { roles: ['DOCTOR'] } },
            { path: 'myPatients/addPatient', component: addPatient, meta: { roles: ['DOCTOR', 'DOCTOR_EXTENDED'] } },
            { path: 'allPatients', component: allPatients, meta: { roles: ['DOCTOR', 'DOCTOR_EXTENDED'] } },
            { path: 'allDoctors', component: allDoctors, meta: { roles: ['DOCTOR_EXTENDED'] } },
            { path: 'allDoctors/addDoctor', component: addDoctor, meta: { roles: ['DOCTOR_EXTENDED'] } },
            { path: 'allDoctors/:id', component: doctorProfileAdmin, meta: { roles: ['DOCTOR_EXTENDED'] } },
            { path: 'patientCard/:code', component: doctorPatientCard },
            { path: 'patientCard/:code/addExamination', component: uploadExamination },
            { path: 'doctorCard', component: doctorCard},

        ],
    },

]

const router = createRouter({
    history: createWebHashHistory(import.meta.env.BASE_URL),
    routes,
})

router.beforeEach((to) => {
    const authStore = useAuthStore()
    const requiresAuth = Boolean(to.meta.requiresAuth)
    const guestOnly = Boolean(to.meta.guestOnly)
    const allowedRoles = Array.isArray(to.meta.roles) ? to.meta.roles as string[] : []
    const selectedLoginRole = authStore.selectedLoginRole.value

    if (to.path.startsWith('/patient') && selectedLoginRole === 'doctor') {
        if (authStore.isDoctor.value) return { path: '/doctor' }
        return { path: '/login', query: { role: 'doctor' } }
    }

    if (to.path.startsWith('/doctor') && selectedLoginRole === 'patient') {
        if (authStore.isPatient.value) return { path: '/patient' }
        return { path: '/login', query: { role: 'patient' } }
    }

    if (requiresAuth && !authStore.isAuthenticated.value) {
        return { path: '/login', query: { role: selectedLoginRole } }
    }

    if (requiresAuth && allowedRoles.length > 0) {
        const role = authStore.role.value
        if (!role || !allowedRoles.includes(role)) {
            if (authStore.isDoctor.value) return { path: '/doctor' }
            if (authStore.isPatient.value) return { path: '/patient' }
            return { path: '/login' }
        }
    }

    if (to.path === '/doctor' && authStore.isDoctor.value) {
        if (authStore.isDoctorExtended.value) return { path: '/doctor/allPatients' }
        return { path: '/doctor/myPatients' }
    }

    if (guestOnly && authStore.isAuthenticated.value) {
        if (authStore.isDoctor.value) return { path: '/doctor' }
        if (authStore.isPatient.value) return { path: '/patient' }
    }

    return true
})

export default router


