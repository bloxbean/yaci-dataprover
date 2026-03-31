// Merkle types
export interface MerkleResponse {
	identifier: string;
	scheme: string;
	rootHash: string;
	status: MerkleStatus;
	createdAt: string;
	lastUpdated: string;
	description?: string;
	metadata?: Record<string, unknown>;
	storeOriginalKeys?: boolean;
}

export type MerkleStatus = 'ACTIVE' | 'BUILDING' | 'ARCHIVED' | 'DELETED';

export interface CreateMerkleRequest {
	identifier: string;
	scheme?: string;
	description?: string;
	metadata?: Record<string, unknown>;
	storeOriginalKeys?: boolean;
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

export interface MerkleSizeResponse {
	merkleIdentifier: string;
	size: number;
	computationTimeMs: number;
}

// Merkle entries types
export interface MerkleEntryResponse {
	originalKey: string | null;
	hashedKey: string;
	value: string;
}

export interface MerkleEntriesResponse {
	merkleIdentifier: string;
	entries: MerkleEntryResponse[];
	totalReturned: number;
	hasMore: boolean;
	computationTimeMs: number;
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

// Tree structure types for MPF trie visualization
export type TreeNodeType = 'branch' | 'extension' | 'leaf' | 'truncated';

export interface BranchTreeNode {
	type: 'branch';
	hash: string;
	value: string | null;
	children: Record<string, TreeNode>;
}

export interface ExtensionTreeNode {
	type: 'extension';
	hash: string;
	path: string;
	child: TreeNode | null;
}

export interface LeafTreeNode {
	type: 'leaf';
	hash: string;
	path: string;
	value: string;
	originalKey: string | null;
}

export interface TruncatedTreeNode {
	type: 'truncated';
	hash: string;
	nodeType: string;
	childCount: number;
	prefix: string;
}

export type TreeNode = BranchTreeNode | ExtensionTreeNode | LeafTreeNode | TruncatedTreeNode;

export interface TreeStructureResponse {
	merkleIdentifier: string;
	root: TreeNode | null;
	totalNodes: number;
	truncated: boolean;
	computationTimeMs: number;
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

// Provider types
export type ProviderStatus = 'AVAILABLE' | 'NOT_CONFIGURED' | 'ERROR';

export type FieldType = 'STRING' | 'INTEGER' | 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'PASSWORD';

export interface SelectOption {
	value: string;
	label: string;
}

export interface FieldValidation {
	min?: number;
	max?: number;
	pattern?: string;
	minLength?: number;
	maxLength?: number;
}

export interface ConfigField {
	name: string;
	label: string;
	type: FieldType;
	required: boolean;
	description?: string;
	placeholder?: string;
	defaultValue?: unknown;
	validation?: FieldValidation;
	options?: SelectOption[];
}

export interface ConfigSchema {
	fields: ConfigField[];
}

export interface ConnectionConfigSchema {
	fields: ConfigField[];
}

export interface KeySerializationSchema {
	keyFieldName: string;
	keyFieldLabel: string;
	keyFieldType?: string;
	keyFieldPlaceholder?: string;
	keyDescription?: string;
}

export interface ProviderInfo {
	name: string;
	description: string;
	dataType: string;
	status: ProviderStatus;
	statusMessage?: string;
	configSchema?: ConfigSchema;
	connectionConfigSchema?: ConnectionConfigSchema;
	keySerializationSchema?: KeySerializationSchema;
	currentConnectionConfig?: Record<string, unknown>;
	configSource?: 'UI' | 'ENV';
}

export interface ProviderListResponse {
	providers: ProviderInfo[];
}

export interface SerializeKeyRequest {
	key: string;
}

export interface SerializeKeyResponse {
	originalKey: string;
	serializedKeyHex: string;
	keyLength: number;
}

export interface ProviderIngestRequest {
	merkleName: string;
	createIfNotExists: boolean;
	merkleScheme?: string;
	merkleDescription?: string;
	storeOriginalKeys?: boolean;
	provider: string;
	config: Record<string, unknown>;
}

export interface ProviderIngestResponse {
	merkleIdentifier: string;
	merkleCreated: boolean;
	provider: string;
	recordsProcessed: number;
	recordsSkipped: number;
	rootHash: string;
	durationMs: number;
	errors?: string[];
}

// Provider configuration types
export interface ProviderConfigRequest {
	config: Record<string, unknown>;
}

export interface ProviderConfigResponse {
	providerName: string;
	config: Record<string, unknown>;
	source: 'UI' | 'ENV';
	schema?: ConnectionConfigSchema;
}

export interface ConfigTestRequest {
	config: Record<string, unknown>;
}

export interface ConfigTestResponse {
	success: boolean;
	message: string;
}
