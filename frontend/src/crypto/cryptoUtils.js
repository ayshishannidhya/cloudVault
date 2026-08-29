/**
 * Web Crypto API utility module for client-side cryptographic operations.
 * 
 * This module handles:
 * - RSA-2048-OAEP key pair generation (in browser)
 * - RSA private key storage in IndexedDB (never leaves the browser)
 * - RSA decryption of AES keys received from server
 * - AES-256-GCM decryption of file data
 * 
 * SECURITY: The user's RSA private key is NEVER sent to the server.
 * Only the public key is sent during registration.
 */

const DB_NAME = 'CloudSecurityKeyStore';
const DB_VERSION = 1;
const STORE_NAME = 'keys';
const PRIVATE_KEY_ID = 'user-rsa-private-key';

// ==========================================
// IndexedDB Helper Functions
// ==========================================

function openDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onerror = () => reject(request.error);
    request.onsuccess = () => resolve(request.result);
    request.onupgradeneeded = (event) => {
      const db = event.target.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'id' });
      }
    };
  });
}

async function saveToDB(id, data) {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);
    store.put({ id, ...data });
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

async function getFromDB(id) {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly');
    const store = tx.objectStore(STORE_NAME);
    const request = store.get(id);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

// ==========================================
// RSA Key Pair Generation
// ==========================================

/**
 * Generate an RSA-2048-OAEP key pair using the Web Crypto API.
 * Returns the public key as Base64 (SPKI format) and stores the private key in IndexedDB.
 */
export async function generateAndStoreKeyPair() {
  const keyPair = await window.crypto.subtle.generateKey(
    {
      name: 'RSA-OAEP',
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]), // 65537
      hash: 'SHA-256',
    },
    true, // extractable (needed for export)
    ['encrypt', 'decrypt']
  );

  // Export public key as SPKI format → Base64
  const publicKeyBuffer = await window.crypto.subtle.exportKey('spki', keyPair.publicKey);
  const publicKeyBase64 = bufferToBase64(publicKeyBuffer);

  // Export private key as PKCS8 format for storage
  const privateKeyBuffer = await window.crypto.subtle.exportKey('pkcs8', keyPair.privateKey);
  const privateKeyBase64 = bufferToBase64(privateKeyBuffer);

  // Store private key in IndexedDB
  await saveToDB(PRIVATE_KEY_ID, { privateKey: privateKeyBase64 });

  return publicKeyBase64;
}

/**
 * Load the user's RSA private key from IndexedDB.
 * Returns a CryptoKey object ready for decryption.
 */
export async function loadPrivateKey() {
  const record = await getFromDB(PRIVATE_KEY_ID);
  if (!record || !record.privateKey) {
    throw new Error('No RSA private key found. Please register again.');
  }

  const privateKeyBuffer = base64ToBuffer(record.privateKey);

  return window.crypto.subtle.importKey(
    'pkcs8',
    privateKeyBuffer,
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    false,
    ['decrypt']
  );
}

/**
 * Check if a private key exists in IndexedDB.
 */
export async function hasPrivateKey() {
  try {
    const record = await getFromDB(PRIVATE_KEY_ID);
    return !!(record && record.privateKey);
  } catch {
    return false;
  }
}

// ==========================================
// Decryption Functions
// ==========================================

/**
 * Decrypt an RSA-encrypted AES key using the user's RSA private key.
 * 
 * @param {string} encryptedAesKeyBase64 - Base64-encoded RSA-encrypted AES key from server
 * @param {CryptoKey} privateKey - RSA private key from IndexedDB
 * @returns {CryptoKey} AES-GCM key ready for file decryption
 */
export async function decryptAESKey(encryptedAesKeyBase64, privateKey) {
  const encryptedKeyBuffer = base64ToBuffer(encryptedAesKeyBase64);

  const aesKeyBuffer = await window.crypto.subtle.decrypt(
    { name: 'RSA-OAEP' },
    privateKey,
    encryptedKeyBuffer
  );

  // Import the raw AES key bytes as a CryptoKey
  return window.crypto.subtle.importKey(
    'raw',
    aesKeyBuffer,
    { name: 'AES-GCM', length: 256 },
    false,
    ['decrypt']
  );
}

/**
 * Decrypt file data using AES-256-GCM.
 * 
 * @param {ArrayBuffer} encryptedData - The encrypted file bytes
 * @param {CryptoKey} aesKey - The AES-GCM key
 * @param {string} ivBase64 - Base64-encoded IV used during encryption
 * @returns {ArrayBuffer} Original file data
 */
export async function decryptFile(encryptedData, aesKey, ivBase64) {
  const iv = base64ToBuffer(ivBase64);

  const decryptedBuffer = await window.crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: iv, tagLength: 128 },
    aesKey,
    encryptedData
  );

  return decryptedBuffer;
}

/**
 * Full decryption pipeline: encrypted AES key + encrypted file → original file.
 * 
 * @param {ArrayBuffer} encryptedFileData - Encrypted file bytes from server
 * @param {string} encryptedAesKeyBase64 - RSA-encrypted AES key (Base64)
 * @param {string} ivBase64 - IV (Base64)
 * @returns {ArrayBuffer} Decrypted original file data
 */
export async function decryptFileComplete(encryptedFileData, encryptedAesKeyBase64, ivBase64) {
  // Step 1: Load private key from IndexedDB
  const privateKey = await loadPrivateKey();

  // Step 2: Decrypt AES key with RSA private key
  const aesKey = await decryptAESKey(encryptedAesKeyBase64, privateKey);

  // Step 3: Decrypt file with AES key
  const decryptedData = await decryptFile(encryptedFileData, aesKey, ivBase64);

  return decryptedData;
}

// ==========================================
// Utility Functions
// ==========================================

function bufferToBase64(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

function base64ToBuffer(base64) {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

/**
 * Format file size for display.
 */
export function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}
