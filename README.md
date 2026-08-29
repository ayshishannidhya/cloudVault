# CloudVault

A cloud-based file security system using **hybrid encryption** (AES-256-GCM + RSA-2048). Upload, encrypt, share, and download files securely — with zero-effort sharing powered by server-side key management.

---

## Features

- **Hybrid Encryption** — Files encrypted with AES-256-GCM (fast), keys wrapped with RSA-2048 (secure)
- **User Authentication** — JWT-based auth with BCrypt password hashing
- **Encrypted File Upload** — Files encrypted on the server before storage
- **Zero-Effort Sharing** — Owner just enters a username; server re-encrypts the AES key for the recipient
- **Client-Side Decryption** — RSA private keys generated and stored in the browser (IndexedDB via Web Crypto API)
- **Access Revocation** — Owner can revoke shared access at any time

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                      Browser (React)                      │
│  ┌─────────────────┐  ┌────────────────────────────────┐ │
│  │  RSA Key Gen     │  │  Client-Side AES Decryption    │ │
│  │  (Web Crypto API)│  │  (Download → Decrypt → File)   │ │
│  └─────────────────┘  └────────────────────────────────┘ │
└──────────────────────────┬───────────────────────────────┘
                           │ REST API (JWT Auth)
┌──────────────────────────▼───────────────────────────────┐
│                  Spring Boot Server                       │
│  ┌──────────┐  ┌──────────────┐  ┌────────────────────┐ │
│  │ Auth     │  │ Encryption   │  │ Key Management     │ │
│  │ Service  │  │ Engine       │  │ Service            │ │
│  │ (JWT)    │  │ (AES-256-GCM)│  │ (RSA-2048 Wrap)    │ │
│  └──────────┘  └──────────────┘  └────────────────────┘ │
│  ┌──────────────────────────────────────────────────────┐ │
│  │              Share Service (Zero-Effort)              │ │
│  │  Server unwraps its AES key copy → re-wraps for      │ │
│  │  recipient's RSA public key                           │ │
│  └──────────────────────────────────────────────────────┘ │
└──────────────────────────┬───────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────┐
│                      Storage                              │
│  ┌──────────────┐  ┌────────────────────────────────────┐ │
│  │ H2 Database  │  │ Encrypted Files (Disk)             │ │
│  │ (Users, Keys)│  │ (AES-256-GCM encrypted)            │ │
│  └──────────────┘  └────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

## Encryption Flow

### Upload
1. Server generates random AES-256 session key
2. File data encrypted using AES-256-GCM
3. AES key wrapped with owner's RSA public key (OWNER entry)
4. AES key wrapped with server's RSA public key (SERVER entry)
5. Encrypted file stored on disk

### Share (Zero-Effort)
1. Server decrypts AES key using server's RSA private key
2. Server re-encrypts AES key with recipient's RSA public key
3. New key entry (SHARED) stored for the recipient
4. **Owner does nothing** — no re-upload, no re-encryption

### Download
1. Server sends encrypted file + RSA-wrapped AES key
2. Browser decrypts AES key using user's RSA private key (from IndexedDB)
3. Browser decrypts file data using AES key → original file

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3, Spring Security, JPA |
| Database | H2 (file-based) |
| Encryption | JCA — AES-256-GCM, RSA-2048-OAEP (SHA-256) |
| Frontend | React 19, Vite, Axios |
| Client Crypto | Web Crypto API (RSA key gen, AES decrypt) |
| Auth | JWT (JSON Web Tokens) + BCrypt |

## Project Structure

```
cloudVault/
├── backend/
│   ├── src/main/java/com/cloudsecurity/
│   │   ├── config/          # SecurityConfig, ServerKeyConfig, CorsConfig
│   │   ├── controller/      # AuthController, FileController, ShareController
│   │   ├── dto/             # Request/Response DTOs
│   │   ├── model/           # User, EncryptedFile, FileKeyEntry entities
│   │   ├── repository/      # JPA repositories
│   │   ├── security/        # JWT provider, filter
│   │   └── service/         # AuthService, FileStorageService, ShareService,
│   │                        # CryptoService, KeyManagementService
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/      # Navbar, FileUpload, FileCard, FileList, ShareModal
│   │   ├── context/         # AuthContext (JWT session)
│   │   ├── crypto/          # cryptoUtils.js (Web Crypto API)
│   │   ├── pages/           # LoginPage, RegisterPage, DashboardPage
│   │   ├── services/        # api.js (Axios)
│   │   ├── App.jsx
│   │   ├── main.jsx
│   │   └── index.css
│   ├── index.html
│   ├── vite.config.js
│   └── package.json
└── README.md
```

## Setup & Run

### Prerequisites
- Java 17+
- Node.js 18+
- Maven

### Backend
```bash
cd backend
mvn spring-boot:run
# Runs on http://localhost:8080
```

### Frontend
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173 (proxies API to :8080)
```

### Usage
1. Open `http://localhost:5173`
2. **Register** — RSA-2048 key pair auto-generates in your browser
3. **Upload** — Drop a file; it's encrypted with AES-256-GCM
4. **Share** — Click share icon → enter recipient's username → done
5. **Download** — Click download; file is decrypted in your browser
6. **Revoke** — Open share modal → click "Revoke" next to any user

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register (sends RSA public key) |
| POST | `/api/auth/login` | Login (returns JWT) |
| POST | `/api/files/upload` | Upload & encrypt file |
| GET | `/api/files/my-files` | List own files |
| GET | `/api/files/shared-with-me` | List files shared with user |
| GET | `/api/files/download/{id}` | Download encrypted file |
| DELETE | `/api/files/{id}` | Delete file |
| POST | `/api/share` | Share file with user |
| DELETE | `/api/share/{fileId}/{username}` | Revoke access |
| GET | `/api/share/{fileId}/users` | List shared users |

## Security Model

- **AES-256-GCM** — Authenticated encryption (confidentiality + integrity)
- **RSA-2048-OAEP** — Asymmetric key wrapping with SHA-256
- **Key Envelope Table** — Each file has per-user encrypted AES key entries
- **Private keys never leave the browser** — Generated via Web Crypto API, stored in IndexedDB
- **JWT authentication** — Stateless, token-based session management
- **BCrypt** — Password hashing with salt
