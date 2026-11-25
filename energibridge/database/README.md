# Database per il benchmark EnergiBridge

Questa configurazione crea un'istanza MySQL dedicata al benchmark, separata dal
database normale dell'applicazione. Usala solo con il progetto Compose
`energibridge`.

## Isolamento e protezione dei dati

- L'istanza usa il progetto Compose `energibridge`, un volume Docker distinto e
  la porta host `3307` (la porta interna del container resta `3306`).
- Lo schema si chiama comunque `moneyart`, ma appartiene a un'istanza MySQL
  separata. Il database normale sulla porta `3306` non viene riutilizzato.
- Al primo avvio su un volume vuoto, gli script di inizializzazione eliminano
  e ricreano gli schemi `moneyart` e `timerservice`. Questo è sicuro solo
  perché vengono eseguiti nel container isolato.
- `seed.sql` è un'operazione esplicitamente distruttiva: cancella tutte le righe
  in `moneyart` e i timer in `timerservice`, poi ricrea il dataset benchmark.
  Non usarlo con un altro database o con dati da conservare.
- Fermare e riavviare il container con `stop` e `up -d` **non** cancella il
  volume né i dati. Evita `down -v`: rimuove il volume e i dati persistenti.
- Le credenziali locali sono in `.env`, che è escluso da Git. Non inserire qui
  password usate altrove.

## Prerequisiti

- Docker Desktop avviato.
- Il file locale `energibridge/database/.env`, creato copiando `.env.example`.
- Tomcat configurato con il descrittore esterno descritto nella sezione
  [Configurare Tomcat](#configurare-tomcat).

## Avviare il database

Esegui i comandi da PowerShell, dalla root del repository.

1. Crea il file `.env` locale:

   ```powershell
   Copy-Item .\energibridge\database\.env.example .\energibridge\database\.env
   ```

2. Imposta `ENERGIBRIDGE_MYSQL_ROOT_PASSWORD` in `.env`. Se la porta `3307` è
   già occupata, cambia `ENERGIBRIDGE_MYSQL_PORT` e usa lo stesso valore anche
   nella configurazione di Tomcat.

3. Avvia l'istanza:

   ```powershell
   docker compose --env-file .\energibridge\database\.env -p energibridge -f .\energibridge\database\docker-compose.yml up -d
   ```

4. Verifica lo stato:

   ```powershell
   docker compose --env-file .\energibridge\database\.env -p energibridge -f .\energibridge\database\docker-compose.yml ps
   ```

Il database è raggiungibile da Tomcat su `localhost:3307`.

## Caricare o ripristinare il dataset

Il seed è ripetibile: a ogni esecuzione ricostruisce lo stesso insieme iniziale
di record, ma elimina prima gli eventuali dati e timer presenti nei due
schemi del database benchmark. Per questo motivo, eseguilo solo quando vuoi
azzerare lo stato del benchmark. Prima di ricaricarlo, ferma Tomcat, così
eventuali timer in memoria non agiscono sul vecchio dataset.

Con il container avviato, esegui:

```powershell
docker compose --env-file .\energibridge\database\.env -p energibridge -f .\energibridge\database\docker-compose.yml exec -T db sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /benchmark/seed.sql'
```

Il seed crea un dataset di dimensione intermedia, deterministico e ripetibile:

- 55 account: 5 account fissi e 50 account generati (`bench_user_001` -
  `bench_user_050`);
- 253 opere, con immagini associate, e 252 aste attive (ID `1` - `252`);
- 1 rivendita attiva;
- 2.502 partecipazioni: due sulle aste fisse e dieci su ciascuna delle 250 aste
  generate;
- date di scadenza fisse nel 2099, per evitare che le aste scadano durante i
  benchmark;
- password sintetica comune agli account e saldi iniziali coerenti con le
  operazioni di prova.

La password sintetica comune agli account è `benchmark-only-123`. Nell'app,
l'utente con ID `1` è amministratore secondo `amministratori.json`. Le aste
generate hanno ID `3` - `252` e offerte da `100` a `199`. Per provare
un'offerta usa un account autenticato, per esempio `bench_bidder_a`, sull'asta
`3` con un importo superiore a `199` e inferiore al saldo dell'utente.
Ripristina il seed tra le prove di scrittura per evitare che lo stato
accumulato cambi il carico.

### Immagini del dataset

Non è necessario inserire le immagini manualmente nel database. Compose monta
in sola lettura la cartella
`src/main/webapp/static/demo` dentro MySQL come `/benchmark-assets`.
`seed.sql` usa `LOAD_FILE()` per caricare le immagini esistenti direttamente
nei campi BLOB delle opere e degli utenti. La configurazione limita
`LOAD_FILE()` a quella cartella. Il mount è scrivibile perché l'entrypoint
MySQL deve impostare i permessi della directory; lo script di seed si limita a
leggere i file e non ne modifica il contenuto.

Il dataset usa le immagini demo `bears-deluxe-3742.png`, `shibosis.jpg`,
`cupcat.jpg` e `profilePics/gattoProfilePic.jpg`. Sono file già presenti nel
repository: rispettivamente circa 2 KB, 8 KB, 35 KB e 53 KB. Sono adatti per
verificare il caricamento e il trasferimento delle immagini; per stimare il
costo di immagini più grandi, usa fixture di dimensioni rappresentative e
mantienile uguali tra tutte le esecuzioni. Se aggiungi fixture, collocale nella
stessa cartella montata e aggiorna i percorsi in `seed.sql`.

## Verificare app e database

Per controllare la lettura delle aste dopo il seed:

```powershell
curl.exe -i -H "X-Requested-With: XMLHttpRequest" "http://localhost:8080/MoneyArt-1.0-SNAPSHOT/getAuctions?action=inCorso"
```

Con il dataset caricato ci si aspetta `HTTP 200` e una risposta JSON con due
aste. Una risposta `[]` indica che la richiesta è riuscita ma non trova aste
attive. Un `404` di solito indica un context path o una distribuzione WAR
errati; un `500` richiede di consultare i log di Tomcat e MySQL.

Per le immagini, usa `ServletFotoOpera` con un ID esistente, ad esempio:

```powershell
curl.exe -I "http://localhost:8080/MoneyArt-1.0-SNAPSHOT/artworkPicture?id=1"
```

## Configurare Tomcat

Copia `..\config\MoneyArt-1.0-SNAPSHOT.xml.example` nella directory esterna di
Tomcat `conf\Catalina\localhost` chiamandolo
`MoneyArt-1.0-SNAPSHOT.xml`. In entrambe le risorse JNDI, sostituisci
`CHANGE_ME` in `username` con `root` e in `password` con lo stesso valore di
`ENERGIBRIDGE_MYSQL_ROOT_PASSWORD` in `.env`. Riavvia Tomcat dopo aver
modificato il descrittore. La configurazione esterna punta a `localhost:3307`;
non modificare il `context.xml` normale del progetto.

## Fermare il database

Per fermare il container conservando il dataset:

```powershell
docker compose --env-file .\energibridge\database\.env -p energibridge -f .\energibridge\database\docker-compose.yml stop
```

Per riavviarlo, ripeti il comando `up -d` della sezione precedente.
