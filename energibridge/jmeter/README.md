# JMeter

Questa cartella contiene i piani JMeter e lo script che li avvia insieme a
EnergiBridge.

## Piani disponibili

I file `.jmx` in `plans` eseguono richieste GET in sola lettura:

- `01-auctions-list.jmx`: elenco delle aste attive.
- `02-resales-list.jmx`: elenco delle rivendite attive.
- `03-auction-detail.jmx`: dettaglio dell'asta con ID `1`.

## Endpoint verificati nei tre scenari

Tutte le richieste sono inviate a `http://localhost:8080` e includono l'header
`X-Requested-With: XMLHttpRequest`. JMeter verifica che la risposta abbia codice
HTTP `200`.

| Scenario | Piano JMeter | Endpoint e parametri | Classe Java associata |
|---|---|---|---|
| Elenco delle aste attive | `01-auctions-list.jmx` | `GET http://localhost:8080/MoneyArt-1.0-SNAPSHOT/getAuctions?action=inCorso` | [`ServletGetAste.java`](/C:/master_degree_projects/sustainable/sse-MoneyArt/src/main/java/it/unisa/c02/moneyart/gestione/vendite/aste/control/ServletGetAste.java) |
| Elenco delle rivendite attive | `02-resales-list.jmx` | `GET http://localhost:8080/MoneyArt-1.0-SNAPSHOT/getResells?action=inCorso` | [`ServletGetRivendite.java`](/C:/master_degree_projects/sustainable/sse-MoneyArt/src/main/java/it/unisa/c02/moneyart/gestione/vendite/rivendite/control/ServletGetRivendite.java) |
| Dettaglio dell'asta con ID 1 | `03-auction-detail.jmx` | `GET http://localhost:8080/MoneyArt-1.0-SNAPSHOT/getAuction?id=1` | [`ServletGetAsta.java`](/C:/master_degree_projects/sustainable/sse-MoneyArt/src/main/java/it/unisa/c02/moneyart/gestione/vendite/aste/control/ServletGetAsta.java) |

Ogni piano usa il context path `MoneyArt-1.0-SNAPSHOT` su
`http://localhost:8080`, invia l'header AJAX richiesto dall'applicazione e
verifica una risposta HTTP `200`.

## Avvio consigliato

Avvia prima il database benchmark e Tomcat, poi esegui PowerShell come
amministratore e lancia dalla root del repository:

```powershell
.\energibridge\jmeter\run-benchmark.ps1 -Scenario auctions-list -Threads 1 -RampUp 1 -Duration 120
```

Valori ammessi per `-Scenario`:

- `auctions-list`
- `resales-list`
- `auction-detail`

`-Threads` imposta gli utenti concorrenti, `-RampUp` i secondi di avvio degli
utenti e `-Duration` la durata del test in secondi. Ogni esecuzione crea una
cartella con data e ora in `energibridge/results`, contenente il CSV di
EnergiBridge e il JTL di JMeter.

Lo script usa i percorsi locali `C:\tools\EnergiBridge\energibridge.exe` e
`C:\tools\apache-jmeter-5.6.3\bin\jmeter.bat`. Se installi i programmi in
posizioni diverse, aggiorna quei percorsi all'inizio di
`run-benchmark.ps1`.

EnergiBridge misura le risorse dell'host durante l'esecuzione di JMeter. Se
Tomcat, MySQL e JMeter sono sullo stesso computer, la misura include l'intero
host, compreso il generatore di carico.
