# Pipeline di misurazione

Questa guida descrive come confrontare il consumo dell'host locale per le tre
funzionalità MoneyArt, prima e dopo un green refactoring:

1. elenco aste attive (`auctions-list`);
2. elenco rivendite attive (`resales-list`);
3. dettaglio di un'asta (`auction-detail`).

La misura EnergiBridge riguarda l'host durante il test. Se JMeter, Tomcat e
MySQL sono sullo stesso PC, i dati includono il consumo di tutti e tre e degli
altri processi attivi.

## Preparazione

1. Avvia Docker Desktop, il database benchmark e Tomcat.
2. Verifica che MoneyArt risponda correttamente prima di misurare.
3. Chiudi o evita attività non necessarie e pesanti sul PC, come compilazioni,
   download o altri test. Non disattivare servizi di sistema.
4. Usa lo stesso dataset e gli stessi parametri JMeter per tutte le prove:
   numero di utenti, ramp-up e durata. Un punto di partenza è 1 utente,
   ramp-up di 1 secondo e durata di 120 secondi.

## Prima del refactoring

1. Fai una breve prova di riscaldamento con JMeter, senza considerare quei
   risultati nel benchmark. Serve a far gestire alcune richieste a Tomcat e al
   database prima della misura.
2. Attendi che il PC torni in condizioni tranquille. Mantieni un intervallo
   simile tra tutte le prove successive.
3. Esegui cinque ripetizioni per ciascuno scenario. Dai un nome esplicito a
   ogni ciclo con `-RunId`, per esempio `prima-r01`. Usa lo stesso RunId per i
   tre scenari di quel ciclo: ciascuno viene salvato in una sottocartella
   separata. Per distribuire l'effetto
   dell'ordine e del riscaldamento, alterna l'ordine tra i cicli:
   - ciclo 1: aste, rivendite, dettaglio;
   - ciclo 2: rivendite, dettaglio, aste;
   - ciclo 3: dettaglio, aste, rivendite;
   - ciclo 4: aste, dettaglio, rivendite;
   - ciclo 5: rivendite, aste, dettaglio.
4. Lancia un solo scenario alla volta, dalla root del repository. Per il primo
   ciclo:

   ```powershell
   .\energibridge\jmeter\run-benchmark.ps1 -Scenario auctions-list -Threads 1 -RampUp 1 -Duration 120 -RunId prima-r01
   .\energibridge\jmeter\run-benchmark.ps1 -Scenario resales-list -Threads 1 -RampUp 1 -Duration 120 -RunId prima-r01
   .\energibridge\jmeter\run-benchmark.ps1 -Scenario auction-detail -Threads 1 -RampUp 1 -Duration 120 -RunId prima-r01
   ```

   Nei cicli successivi cambia il RunId (`prima-r02`, `prima-r03` e così via)
   e segui l'ordine indicato sopra. Ogni esecuzione crea una cartella
   contenente `energibridge.csv` e `jmeter.jtl`.
5. Se una prova ha errori, interruzioni o attività insolite sul PC, annotala
   e ripetila; non eliminare una prova solo perché il risultato è sfavorevole.
6. Conserva i risultati come baseline "prima" e annota eventuali condizioni
   particolari.

## Dopo il refactoring

Ripeti la stessa procedura sul codice modificato: stesso PC, dataset,
parametri, riscaldamento, numero di ripetizioni, intervalli e ordine degli
scenari. Usa RunId diversi, per esempio `dopo-r01` fino a `dopo-r05`. Se
possibile, usa una copia o un backup coerente del dataset prima di iniziare
ciascuna serie di misure.

## Organizzazione dei file

I piani `.jmx` e lo script restano in `energibridge/jmeter`; i risultati delle
prove vanno in `energibridge/results`. Con i RunId suggeriti, la struttura
risulta così:

```text
energibridge/
├── jmeter/
│   ├── plans/
│   └── run-benchmark.ps1
└── results/
    ├── prima-r01/
    │   ├── auctions-list/
    │   │   ├── energibridge.csv
    │   │   └── jmeter.jtl
    │   ├── resales-list/
    │   └── auction-detail/
    ├── prima-r02/
    ├── ...
    ├── dopo-r01/
    └── ...
```

In ogni cartella di ciclo puoi aggiungere un file di note con la data, la
versione o commit del codice, la modalità di alimentazione del PC e qualsiasi
evento insolito. Non modificare né sovrascrivere i risultati "prima" quando
prepari quelli "dopo".

## Analisi dei risultati

Per ogni esecuzione controlla nel JTL:

- numero di richieste riuscite e di errori;
- tempi di risposta medio e 95° percentile;
- eventuali differenze rilevanti nella dimensione delle risposte.

Nel CSV EnergiBridge usa i campioni corrispondenti alla finestra del test.
Allinea i timestamp del CSV (`Time`) con quelli del JTL (`timeStamp`), perché
il CSV può includere anche l'avvio di JMeter o il tempo successivo alle ultime
richieste.

Per ciascuna ripetizione calcola:

```text
energia per richiesta riuscita = energia misurata in joule / richieste riuscite
```

Poi, per ogni scenario e per ciascuna serie ("prima" e "dopo"), calcola la
mediana dei cinque valori di energia per richiesta riuscita. Usa la mediana
come risultato principale; puoi riportare anche media e deviazione standard
per mostrare quanto variano le prove.

Confronta prima e dopo separatamente per aste, rivendite e dettaglio. Un
consumo minore per richiesta riuscita è un segnale positivo solo se il lavoro
completato è comparabile e non peggiorano in modo significativo errori o
tempi di risposta. Riporta anche le metriche prestazionali, non solo l'energia.
