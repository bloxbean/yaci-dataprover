import { apiPost } from './client';
import type {
	ProofGenerationRequest,
	ProofGenerationResponse,
	ProofVerificationRequest,
	ProofVerificationResponse
} from './types';

export const proofApi = {
	/**
	 * Generate a proof for a single key
	 */
	generate: (merkleId: string, request: ProofGenerationRequest) =>
		apiPost<ProofGenerationResponse>(
			`/merkle/${encodeURIComponent(merkleId)}/proofs`,
			request
		),

	/**
	 * Generate proofs for multiple keys
	 */
	generateBatch: (merkleId: string, requests: ProofGenerationRequest[]) =>
		apiPost<ProofGenerationResponse[]>(
			`/merkle/${encodeURIComponent(merkleId)}/proofs/batch`,
			requests
		),

	/**
	 * Verify a proof
	 */
	verify: (merkleId: string, request: ProofVerificationRequest) =>
		apiPost<ProofVerificationResponse>(
			`/merkle/${encodeURIComponent(merkleId)}/proofs/verify`,
			request
		)
};
