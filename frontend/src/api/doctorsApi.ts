import type {
  CreateDoctorRequest,
  CreatedDoctorResponse,
  DoctorListResponse,
  DoctorPasswordResetResponse,
  DoctorResponse,
  ListDoctorsParams,
  UpdateDoctorRequest,
} from '@/api/patientApi.contract'
import { apiRequest } from '@/api/httpClient'

export const doctorsApi = {
  getCurrentDoctor() {
    return apiRequest<DoctorResponse>('/api/doctors/me')
  },

  listDoctors(params: ListDoctorsParams = {}) {
    return apiRequest<DoctorListResponse>('/api/doctors', {
      query: {
        page: params.page ?? 0,
        limit: params.limit ?? 20,
        regionId: params.regionId,
        role: params.role,
        status: params.status,
        search: params.search,
      },
    })
  },

  createDoctor(payload: CreateDoctorRequest) {
    return apiRequest<CreatedDoctorResponse>('/api/doctors', {
      method: 'POST',
      body: payload,
    })
  },

  updateDoctor(doctorId: number, payload: UpdateDoctorRequest) {
    return apiRequest<DoctorResponse>(`/api/doctors/${doctorId}`, {
      method: 'PATCH',
      body: payload,
    })
  },

  resetDoctorPassword(doctorId: number) {
    return apiRequest<DoctorPasswordResetResponse>(`/api/doctors/${doctorId}/password-reset`, {
      method: 'POST',
    })
  },
}
