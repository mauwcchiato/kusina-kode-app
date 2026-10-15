# Kusina Kode System Primer

A study module for the capstone defense. It explains how Kusina Kode is built, in six units:

1. OOP and architecture
2. Data structures
3. Algorithms
4. Cybersecurity
5. Cloud computing
6. System diagrams

Every concept follows the same steps: **definition**, **why it matters here**, **how we applied it**, **in plain words**, and **insights / what the panel might ask**.

- 📄 **[Kusina-Kode-System-Primer.pdf](Kusina-Kode-System-Primer.pdf)**: the full primer, ready to print (54 pages).
- 🌐 **[index.html](index.html)**: the same content as a web page. Download it together with the `diagrams/` folder and open it in a browser.


Unit 6 is a refresher on each kind of diagram in the paper (what it is for, what every shape means, and how to read it), followed by our own diagram and a walk-through. The full lesson for each one is in [index.html](index.html#diagrams). Everything was checked against the code and the live system on 15 Oct 2026.

---

## 6.1 Data flow diagram (DFD)

What it is: how **data moves** through the system: who sends it, which process changes it, and which store keeps it. Gane & Sarson symbols: external entity (rectangle), process (rounded box with its number), data store (open box with its ID), data flow (labelled arrow). Level 0 is the whole system as one process; Level 1 opens it into the main processes; Level 2 opens one of those.

> **Waiting for the corrected version.** Kyla is fixing Levels 0, 1 and 2. When they are ready, save them as `diagrams/dfd-level0.png`, `diagrams/dfd-level1.png` and `diagrams/dfd-level2.png`, and add them here and in `index.html` (each slot has a ready-made figure in a comment).

## 6.2 Entity relationship diagram (ERD)

![ERD](diagrams/erd.png)

What it is: the **database design**: tables, columns, keys and relationships, in crow's foot notation (two bars = exactly one, circle and bar = zero or one, bar and crow's foot = one or many, circle and crow's foot = zero or many). All 35 tables match the Azure SQL schema. `reports` is left out because the feature is hidden on the adviser's advice.

## 6.3 Process flow

What it is: a **flowchart with swimlanes** that follows one scenario. Oval = start or end, rectangle = step, diamond = yes/no decision, cylinder = data store, double-sided box = a process explained elsewhere, lane = who does the step.

> **PF1 is waiting for the corrected version** (`diagrams/pf1-solve-dish.png`): step 6 "Is the guess right?" belongs to the App lane, because the app checks the guess and the API only stores it.

![PF2](diagrams/pf2-add-level.png)
**PF2:** an administrator adds a level and it reaches the app.

![PF3](diagrams/pf3-sign-in-lockout.png)
**PF3:** a player signs in, with the lockout after three failed attempts in 15 minutes.

![PF4](diagrams/pf4-unlock-reel.png)
**PF4:** a player spends KK to unlock a reel; all four reward phases run before the reel is granted.

## 6.4 Data pipeline

What it is: the **stages** data passes through. The reward pipeline is a transaction lifecycle (one record through four phases); the reporting pipeline is batch ETL (extract, transform, load, serve) run on a schedule.

![Reward pipeline](diagrams/pipeline-reward.png)
![Reporting pipeline](diagrams/pipeline-reporting.png)

## 6.5 Use case diagram

![Use case](diagrams/use-case.png)

What it is: **who** uses the system and **for what**. Stick figure = actor (a role), ellipse = use case (a goal), box = system boundary, solid line = takes part, dashed «include» = always happens, dashed «extend» = optional or conditional, hollow triangle = "is a kind of".

## 6.6 System architecture

![System architecture](diagrams/system-architecture.png)

What it is: the **main parts** and how they connect, in three tiers: client, middleware and cloud services, and the blockchain ledger. Only the API sends reward transactions into the ledger tier.

## 6.7 Activity diagram

![Player](diagrams/activity-player.png)
![Administrator](diagrams/activity-admin.png)

What it is: **every path** one user can take (UML). Filled circle = start, bullseye = end, rounded box = action, diamond = decision or merge, thick bar = fork/join (steps that all happen), [guards] on the exits.

## 6.8 Other diagrams

![Cloud architecture](diagrams/cloud-architecture.png)
**Cloud architecture:** the actual Azure resources (resource group, VNet, VM, NSG, App Service, SQL, Blob, Key Vault, Monitor, Entra ID) and the services outside Azure.

![Cybersecurity architecture](diagrams/cybersecurity-architecture.png)
**Cybersecurity architecture:** the controls grouped by the CIA triad: confidentiality, integrity (tamper evidence) and access control.

![Conceptual framework](diagrams/conceptual-framework.png)
**Conceptual framework:** the research model as Input → Process → Output.
