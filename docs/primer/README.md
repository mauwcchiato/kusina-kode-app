# Kusina Kode System Primer

A study module for the capstone defense. It explains how Kusina Kode is built, in six units:

1. OOP and architecture
2. Data structures
3. Algorithms
4. Cybersecurity
5. Cloud computing
6. System diagrams

Every concept follows the same steps: **definition**, **why it matters here**, **how we applied it**, **in plain words**, and **insights / what the panel might ask**.

- 📄 **[Kusina-Kode-System-Primer.pdf](Kusina-Kode-System-Primer.pdf)**: the full primer, ready to print (40 pages).
- 🌐 **[index.html](index.html)**: the same content as a web page. Download it together with the `diagrams/` folder and open it in a browser.

The six diagrams from Unit 6 are below, each with a short explanation. They were checked against the Azure resources and the live database on 13 Oct 2026.

---

## 6.1 Cloud architecture

![Cloud architecture](diagrams/cloud-architecture.png)

Where each part of Kusina Kode runs on Microsoft Azure (resource group `kusina-kode`, Southeast Asia).

- **Clients:** admins use a web browser; players use the Android app. Both connect over **HTTPS**.
- **Admin Console** (App Service, `admin.kusinakode.com`): calls the API over REST, and also reads Azure SQL and Blob Storage directly.
- **Player path:** the app goes through the **NSG** firewall (only ports 443/80 open; SSH from one IP) to the **PHP REST API** on the VM (`api.kusinakode.com`, inside `vnet-kusinakode`).
- **Ledger:** the **PoA Reward Ledger** runs on the same VM, using SHA-256, ECDSA (secp256k1) and an AES-256-GCM wallet vault.
- **Data:** **Azure SQL** stores players, dishes and rewards (AES-256 at rest, TDE). **Blob Storage** stores images and media.
- **Supporting services:**
  - **Key Vault** holds the secrets; both the console and the VM read it through their managed identities.
  - **Azure Monitor** tracks CPU, DTU and response time.
  - **Microsoft Entra ID** signs admins in. Players sign in with Google, or with email and a bcrypt-hashed password.

**In plain words:** one rented server runs the game's brain and its receipt book. Microsoft looks after the database and the photo album. A guard at the door only lets normal web traffic in.

**Panel might ask:**
- **Why a VM for the API?** The ledger keeps files on disk, runs scheduled jobs (every 15 minutes and every hour) and has a private key folder. A VM gives that control.
- **Why is the blockchain on the same VM?** It's a private proof-of-authority ledger with one validator. The hourly integrity check, not a second machine, is what proves nothing was changed.

## 6.2 Cybersecurity architecture

![Cybersecurity architecture](diagrams/cybersecurity-architecture.png)

The security controls, grouped by the **CIA triad**:

- **Confidentiality:**
  - TLS protects data in transit; AES-256 protects it at rest (SQL, Blob); reward payloads use AES-256-GCM.
  - The admin sign-in secret is in Key Vault. The reward, validator and wallet keys are in a vault folder on the API VM, outside the app code.
- **Integrity:**
  - Each block is linked to the one before by its SHA-256 hash, and signed by the validator with ECDSA (secp256k1).
  - The ledger checks each block as it is added and re-verifies the whole chain every hour.
- **Availability and API security:**
  - **Zero-trust API:** a bearer token is needed on protected endpoints.
  - **Role-based sign-in:** players use bcrypt or Google; admins use Microsoft Entra ID.
  - **Endpoint protection:** login locks for 15 minutes after 3 wrong passwords, and inputs are validated.

**In plain words:** everything travels and rests in locked boxes. Every reward is written in a numbered, fingerprinted and stamped receipt book. Only people with a valid pass get in.

## 6.3 System architecture

![System architecture](diagrams/system-architecture.png)

The system is split into three tiers:

- **Client tier:** the admin web console (ASP.NET Web Forms, Clean Architecture) and the Android app (MVVM, Clean Architecture).
- **Middleware and cloud services tier:** the PHP REST API (VM), the admin console (App Service), the data layer (Azure SQL and Blob Storage) and the supporting services.
- **Blockchain ledger tier** (private PoA, on the API VM):
  1. The KKCoin and KKBadge contract rules decide what a reward is worth.
  2. The ledger records it as a signed, hash-linked block.
  3. The TxHash receipts appear in the admin audit panel.

The apps never write to the ledger themselves. They ask the API, and the API applies the contract rules first.

## 6.4 Use case diagram

![Use case diagram](diagrams/use-case.png)

- **Actors:** Player, Admin and Super admin (a kind of Admin), plus Google, the Email service, Microsoft Entra and the Scheduler (timed server jobs).
- **Lines:** solid lines mean the actor uses a feature. Dashed arrows are «include» (always happens as part of it) or «extend» (optional add-on).
- **Examples:**
  - *Play a dish level* «include» *Earn KK & badges*.
  - *Use a hint* «extend» *Play a dish level*.
  - Every KK action «include» *Record transaction on the chain*.

> **Note:** *Report a reward problem* and *Resolve player reports* are switched off in the app at the adviser's request. The feature is kept in the code and database so it can be restored.

## 6.5 Data flow diagram, Level 0 (context)

![DFD Level 0](diagrams/dfd-level0.png)

The whole system as one process, and the outside parties around it:

- **Player:** sends logins, guesses and purchases; receives levels, dish stories, KK, badges and rank.
- **Admin:** sends content and publish decisions; receives analytics, the KK ledger, the audit log and chain integrity.
- **Google and Microsoft Entra:** confirm identities.
- **Email service:** receives the password reset codes to send.

## 6.6 Data flow diagram, Level 1

![DFD Level 1](diagrams/dfd-level1.png)

Level 0 opened up: 8 processes (1.0–8.0), 7 data stores (D1–D7) and the flows between them.

- **Gameplay and rewards:** gameplay (3.0) tells reward processing (4.0) when a level is solved.
- **The chain:** every KK change goes through the chain (6.0).
- **Admin side:** content management (7.0) and monitoring and audit (8.0).

Since the 12–13 Oct normalization, D2 also covers the new detail tables (`level_ingredients`, `level_steps`, `places`), and D4 covers `badge_definitions` and `shop_items`. The flows are unchanged.

The Scheduler isn't an outside party, so it isn't drawn as an entity. It runs two timed jobs inside 4.0 (every 15 minutes) and 6.0 (every hour).
