import { ApiError, type ErrorResponse } from './types';

const API_BASE = '/api/v1';

async function handleResponse<T>(response: Response): Promise<T> {
	if (!response.ok) {
		let errorData: ErrorResponse;
		try {
			errorData = await response.json();
		} catch {
			throw new ApiError(response.status, 'UNKNOWN_ERROR', response.statusText);
		}
		throw new ApiError(response.status, errorData.code, errorData.message, errorData.fieldErrors);
	}

	// Handle 204 No Content
	if (response.status === 204) {
		return undefined as T;
	}

	return response.json();
}

export async function apiGet<T>(endpoint: string): Promise<T> {
	const response = await fetch(`${API_BASE}${endpoint}`, {
		method: 'GET',
		headers: {
			Accept: 'application/json'
		}
	});
	return handleResponse<T>(response);
}

export async function apiPost<T>(endpoint: string, data?: unknown): Promise<T> {
	const response = await fetch(`${API_BASE}${endpoint}`, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
			Accept: 'application/json'
		},
		body: data ? JSON.stringify(data) : undefined
	});
	return handleResponse<T>(response);
}

export async function apiPut<T>(endpoint: string, data?: unknown): Promise<T> {
	const response = await fetch(`${API_BASE}${endpoint}`, {
		method: 'PUT',
		headers: {
			'Content-Type': 'application/json',
			Accept: 'application/json'
		},
		body: data ? JSON.stringify(data) : undefined
	});
	return handleResponse<T>(response);
}

export async function apiDelete<T = void>(endpoint: string): Promise<T> {
	const response = await fetch(`${API_BASE}${endpoint}`, {
		method: 'DELETE',
		headers: {
			Accept: 'application/json'
		}
	});
	return handleResponse<T>(response);
}
