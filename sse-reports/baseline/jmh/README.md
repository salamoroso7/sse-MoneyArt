# Analisi delle Prestazioni a Livello Micro (JMH) – Baseline MoneyArt

## 1. Obiettivo e Contesto
**JMH (Java Microbenchmark Harness)** è stato utilizzato per eseguire microbenchmark su componenti selezionate del backend di **MoneyArt**, con l’obiettivo di misurare le prestazioni computazionali e la scalabilità di singoli metodi in modo isolato.

L'analisi si concentra esclusivamente sulle operazioni eseguite in CPU e memoria centrale, escludendo le latenze di rete e di I/O verso il database.

---

## 2. Metodologia e Architettura dei Test

### 2.1 Isolamento e Simulazione in Memoria
Per evitare che le chiamate JDBC falsassero le misurazioni, la persistenza è stata isolata tramite tre accorgimenti:
1. **Inizializzazione `@Setup(Level.Trial)`:** prima dell'avvio delle misurazioni di ciascun *trial*, vengono istanziati in memoria i bean del modello (`Utente`, `Opera`, `Asta`, `Partecipazione`, `Rivendita`) con dati deterministici scalati in base ai parametri `@Param`.
2. **Stub anonimi dei DAO:** i DAO reali (`UtenteDaoImpl`, `AstaDaoImpl`, `OperaDaoImpl`, `PartecipazioneDaoImpl`, `RivenditaDaoImpl`) vengono estesi tramite classi anonime passando `null` come `DataSource` e sovrascrivendo i metodi invocati dai service affinché restituiscano direttamente le strutture dati pre-allocate in RAM.
3. **Prevenzione della *Dead Code Elimination*:** tutti i valori restituiti dai metodi sotto test vengono passati a `Blackhole.consume(...)` per impedire al compilatore JIT di rimuovere codice ritenuto inutilizzato.

### 2.2 Componenti Misurati
La suite è suddivisa in **4 classi di benchmark**, ciascuna dedicata a un dominio applicativo specifico:

* **`AstaServiceBenchmark`** (`AstaServiceImpl`):
    * `baseline_getAuctionsSortedByArtistFollowers`: aggregazione delle aste attive e ordinamento per numero di follower dell'artista.
    * `baseline_getAuctionsSortedByPrice`: aggregazione delle aste attive e ordinamento in base all'offerta più alta.
    * *Parametri:* `auctionCount` $\in \{10, 100, 500\}$, `sortOrder` $\in \{\text{ASC}, \text{DESC}\}$.
* **`UtenteServiceBenchmark`** (`UtenteServiceImpl`):
    * `baseline_checkUser`: calcolo dell'hash **SHA-256** della password e confronto byte-a-byte per la verifica delle credenziali di autenticazione.
    * *Parametri:* `passwordLength` $\in \{8, 32, 128\}$ caratteri.
* **`RivenditaServiceBenchmark`** (`RivenditaServiceImpl`):
    * `baseline_getResellPrice`: calcolo del prezzo di rivendita di un'opera digitale ricostruendo lo storico delle aste e delle offerte ricevute.
    * *Parametri:* `bidsCount` $\in \{5, 50, 200\}$ offerte storiche.
* **`AstaLockingBenchmark`** (`AstaLockingSingleton`):
    * `baseline_lockAndUnlock`: acquisizione e rilascio del `ReentrantLock` associato a ciascuna asta per garantire la sicurezza concorrente durante le puntate.
    * *Parametri:* `distinctAuctions` $\in \{10, 100, 1000\}$ aste distinte.

### 2.3 Configurazione Sperimentale e Automazione
* **Ambiente di esecuzione:** Windows, JDK 21 (`21.0.12.1`, Java HotSpot 64-Bit Server VM).
* **Parametri JMH:**
    * Modalità: `AverageTime` (`ns/op`, tempo medio per singola operazione).
    * Warmup: `5` iterazioni da `300 ms` ciascuna (stabilizzazione del compilatore JIT).
    * Measurement: `10` iterazioni da `400 ms` ciascuna.
    * Fork: `2` processi JVM indipendenti per configurazione (`Cnt = 20` campioni totali per riga).
* **Automazione e Artefatti:** l'esecuzione è automatizzata tramite lo script PowerShell `run_jmh_baseline.ps1`, che esegue `org.openjdk.jmh.Main` filtrando ciascuna classe (`".*$b.*"`) e salvando i risultati in `sse-reports/baseline/jmh/<NomeClasse>/`:
    * `results.json`: metriche strutturate, medie, percentili e dati grezzi compatibili con JMH Visualizer.
    * `jmh.log`: log completo delle iterazioni di warmup/measurement e tabella riepilogativa.
    * `metadata.txt`: timestamp, versione JVM e specifiche hardware della CPU.

---

## 3. Risultati della Baseline

La tabella seguente riporta i tempi medi di esecuzione (`Score`) e l'intervallo di confidenza al 99.9% (`Error`) rilevati su `Cnt = 20` misurazioni per ciascuna combinazione parametrica:

| Classe Benchmark | Metodo | Parametri | Score (ns/op) | Errore ±99.9% (ns/op) |
| :--- | :--- | :--- | :---: | :---: |
| `AstaServiceBenchmark` | `baseline_getAuctionsSortedByArtistFollowers` | `auctionCount=10, sortOrder=ASC` | **245,458** | ± 17,324 |
| `AstaServiceBenchmark` | `baseline_getAuctionsSortedByArtistFollowers` | `auctionCount=10, sortOrder=DESC` | **278,325** | ± 6,245 |
| `AstaServiceBenchmark` | `baseline_getAuctionsSortedByArtistFollowers` | `auctionCount=100, sortOrder=ASC` | **2.451,008** | ± 1.375,774 |
| `AstaServiceBenchmark` | `baseline_getAuctionsSortedByArtistFollowers` | `auctionCount=100, sortOrder=DESC` | **2.600,951** | ± 702,408 |
| `AstaServiceBenchmark` | `baseline_getAuctionsSortedByArtistFollowers` | `auctionCount=500, sortOrder=ASC` | **10.769,307** | ± 1.652,055 |
| `AstaServiceBenchmark` | `baseline_getAuctionsSortedByArtistFollowers` | `auctionCount=500, sortOrder=DESC` | **13.349,904** | ± 3.673,420 |
| `AstaServiceBenchmark` | `baseline_getAuctionsSortedByPrice` | `auctionCount=10, sortOrder=ASC` | **314,196** | ± 75,737 |
| `AstaServiceBenchmark` | `baseline_getAuctionsSortedByPrice` | `auctionCount=10, sortOrder=DESC` | **324,608** | ± 21,224 |
| `AstaServiceBenchmark` | `baseline_getAuctionsSortedByPrice` | `auctionCount=100, sortOrder=ASC` | **2.334,752** | ± 139,553 |
| `AstaServiceBenchmark` | `baseline_getAuctionsSortedByPrice` | `auctionCount=100, sortOrder=DESC` | **3.105,409** | ± 503,199 |
| `AstaServiceBenchmark` | `baseline_getAuctionsSortedByPrice` | `auctionCount=500, sortOrder=ASC` | **12.908,525** | ± 4.577,439 |
| `AstaServiceBenchmark` | `baseline_getAuctionsSortedByPrice` | `auctionCount=500, sortOrder=DESC` | **15.464,931** | ± 9.085,166 |
| `UtenteServiceBenchmark` | `baseline_checkUser` | `passwordLength=8` | **416,425** | ± 111,889 |
| `UtenteServiceBenchmark` | `baseline_checkUser` | `passwordLength=32` | **370,880** | ± 14,160 |
| `UtenteServiceBenchmark` | `baseline_checkUser` | `passwordLength=128` | **554,031** | ± 120,975 |
| `RivenditaServiceBenchmark` | `baseline_getResellPrice` | `bidsCount=5` | **9,329** | ± 0,228 |
| `RivenditaServiceBenchmark` | `baseline_getResellPrice` | `bidsCount=50` | **9,882** | ± 1,733 |
| `RivenditaServiceBenchmark` | `baseline_getResellPrice` | `bidsCount=200` | **10,135** | ± 1,594 |
| `AstaLockingBenchmark` | `baseline_lockAndUnlock` | `distinctAuctions=10` | **79,817** | ± 23,014 |
| `AstaLockingBenchmark` | `baseline_lockAndUnlock` | `distinctAuctions=100` | **85,543** | ± 3,023 |
| `AstaLockingBenchmark` | `baseline_lockAndUnlock` | `distinctAuctions=1000` | **82,254** | ± 2,636 |

---

## 4. Osservazioni Principali
* **Scalabilità lineare $O(N)$ in `AstaServiceImpl`:** passando da 10 a 100 aste ($\times 10$) il tempo cresce di circa $\times 10$ (~`250 ns` $\rightarrow$ ~`2.450 ns`), e da 100 a 500 ($\times 5$) cresce di circa $\times 4,5\text{--}5$ (~`11–15 µs`). Il costo dominante risiede nel ciclo di aggregazione dei metadati collegati a ciascuna asta (`getAuctionsByState`), rendendo questo componente il candidato principale per eventuali ottimizzazioni di refactoring.
* **Comportamento a blocchi di SHA-256 (`UtenteServiceImpl`):** per password da 8 e 32 caratteri il costo resta pressoché invariato (~`370–416 ns/op`) poiché entrambi gli input rientrano in un singolo blocco SHA-256 da 512 bit (64 byte); con 128 caratteri l'elaborazione su più blocchi porta il tempo a `554 ns/op`.
* **Complessità costante $O(1)$ per Rivendita e Locking:** sia `getResellPrice` (~`9–10 ns/op` al variare delle offerte) sia `lockAsta`/`unlockAsta` (~`80–85 ns/op` fino a 1.000 aste distinte) mostrano tempi costanti e indipendenti dalla dimensione del carico.