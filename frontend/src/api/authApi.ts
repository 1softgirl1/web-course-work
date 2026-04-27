import type {
  AuthResponse,
  ChangePasswordRequest,
  LoginRequest,
  RefreshTokenRequest,
} from '@/api/patientApi.contract'
import { apiRequest } from '@/api/httpClient'

export const authApi = {
  login(payload: LoginRequest) {
    return apiRequest<AuthResponse>('/auth/login', {
      method: 'POST',
      auth: false,
      body: payload,
    })
  },

  refresh(payload: RefreshTokenRequest) {
    return apiRequest<AuthResponse>('/auth/refresh', {
      method: 'POST',
      auth: false,
      body: payload,
      retryOn401: false,
    })
  },

  logout(payload: RefreshTokenRequest) {
    return apiRequest<void>('/auth/logout', {
      method: 'POST',
      auth: false,
      body: payload,
      retryOn401: false,
    })
  },

  changeOwnPassword(payload: ChangePasswordRequest) {
    return apiRequest<AuthResponse>('/auth/password/change', {
      method: 'POST',
      body: payload,
    })
  },
}
