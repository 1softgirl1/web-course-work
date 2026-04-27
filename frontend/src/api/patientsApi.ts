import type {
  CreateExaminationRequest,
  CreatePatientRequest,
  CreatedPatientResponse,
  ExaminationListResponse,
  ExaminationResponse,
  ListDoctorPatientsParams,
  PatientCardResponse,
  PatientListResponse,
  UpdateExaminationRequest,
  UpdatePatientRequest,
} from '@/api/patientApi.contract'
import { apiRequest } from '@/api/httpClient'

export const patientsApi = {
  listDoctorPatients(params: ListDoctorPatientsParams = {}) {
    return apiRequest<PatientListResponse>('/api/doctor/patients', {
      query: {
        scope: params.scope ?? 'own',
        page: params.page ?? 0,
        limit: params.limit ?? 20,
        regionId: params.regionId,
        diagnosis: params.diagnosis,
      },
    })
  },

  createPatient(payload: CreatePatientRequest) {
    return apiRequest<CreatedPatientResponse>('/api/doctor/patients', {
      method: 'POST',
      body: payload,
    })
  },

  getPatientCard(patientId: number) {
    return apiRequest<PatientCardResponse>(`/api/patients/${patientId}`)
  },

  updatePatientCard(patientId: number, payload: UpdatePatientRequest) {
    return apiRequest<PatientCardResponse>(`/api/patients/${patientId}`, {
      method: 'PATCH',
      body: payload,
    })
  },

  listPatientExaminations(patientId: number) {
    return apiRequest<ExaminationListResponse>(`/api/patients/${patientId}/examinations`)
  },

  addPatientExamination(patientId: number, payload: CreateExaminationRequest) {
    return apiRequest<ExaminationResponse>(`/api/patients/${patientId}/examinations`, {
      method: 'POST',
      body: payload,
    })
  },

  updatePatientExamination(patientId: number, examId: number, payload: UpdateExaminationRequest) {
    return apiRequest<ExaminationResponse>(`/api/patients/${patientId}/examinations/${examId}`, {
      method: 'PATCH',
      body: payload,
    })
  },
}
