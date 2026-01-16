// Merkle types
export interface MerkleResponse {
	identifier: string;
	scheme: string;
	rootHash: string;
	recordCount: number;
	status: MerkleStatus;
	createdAt: string;
	lastUpdated: string;
	description?: string;
	metadata?: Record<string, unknown>;
}

export type MerkleStatus = 'ACTIVE' | 'BUILDING' | 'ARCHIVED' | 'DELETED';

export interface CreateMerkleRequest {
	identifier: string;
	scheme?: string;
	description?: string;
	metadata?: Record<string, unknown>;
}

export interface PageResponse<T> {
	content: T[];
	totalElements: number;
	totalPages: number;
	size: number;
	number: number;
	first: boolean;
	last: boolean;
}

// Entry types
export interface EntryItem {
	key: string;
	value: string;
}

export interface AddEntriesRequest {
	entries: EntryItem[];
}

export interface AddEntriesResponse {
	merkleIdentifier: string;
	entriesAdded: number;
	entriesSkipped: number;
	rootHash: string;
	durationMs: number;
	errors: string[];
}

// Proof types
export interface ProofGenerationRequest {
	key: string;
	format?: string;
}

export interface ProofGenerationResponse {
	key: string;
	value: string | null;
	proof: string;
	rootHash: string;
	proofFormat: string;
}

export interface ProofVerificationRequest {
	key: string;
	value?: string | null;
	proof: string;
	rootHash: string;
}

export interface ProofVerificationResponse {
	key: string;
	value: string | null;
	verified: boolean;
	rootHash: string;
}

export interface RootHashResponse {
	merkleIdentifier: string;
	rootHash: string;
}

// Value lookup types
export interface ValueLookupResponse {
	key: string;
	value: string | null;
	found: boolean;
}

export interface BatchValueLookupRequest {
	keys: string[];
}

export interface BatchValueLookupResponse {
	results: ValueLookupResponse[];
}

// Ingestion types
export interface IngestRequest {
	provider: string;
	config?: Record<string, unknown>;
}

export interface IngestResponse {
	merkleIdentifier: string;
	provider: string;
	recordsProcessed: number;
	recordsSkipped: number;
	rootHash: string;
	durationMs: number;
	errors: string[];
}

// Admin types
export interface HealthResponse {
	status: string;
	rocksdb: string;
}

export interface CacheStats {
	currentSize: number;
	maxSize: number;
	cacheHits: number;
	cacheMisses: number;
	hitRate: string;
}

export interface SystemStats {
	cache: CacheStats;
	rocksdb: {
		isOpen: boolean;
		columnFamilyCount: number;
	};
}

export interface CacheInfo {
	cachedMerkle: string[];
	currentSize: number;
	maxSize: number;
	cacheHits: number;
	cacheMisses: number;
	hitRate: string;
}

export interface StorageInfo {
	isOpen: boolean;
	columnFamilies: string[];
	columnFamilyCount: number;
}

// Error types
export interface ErrorResponse {
	code: string;
	message: string;
	fieldErrors?: Record<string, string>;
	timestamp: string;
}

export class ApiError extends Error {
	code: string;
	status: number;
	fieldErrors?: Record<string, string>;

	constructor(status: number, code: string, message: string, fieldErrors?: Record<string, string>) {
		super(message);
		this.status = status;
		this.code = code;
		this.fieldErrors = fieldErrors;
	}
}
