# Risultati EnergiBridge e JMeter

Questa cartella raccoglie i risultati generati da
`energibridge/jmeter/run-benchmark.ps1`. I file vengono organizzati per
esecuzione e scenario, ad esempio:

```text
results/
└── 20261125-112433/
    └── auctions-list/
        ├── energibridge.csv
        └── jmeter.jtl
```

## File prodotti

- `energibridge.csv` contiene i campioni EnergiBridge, come energia e potenza
  del processore, utilizzo CPU e memoria.
- `jmeter.jtl` contiene una riga per ogni richiesta JMeter, con timestamp,
  durata, codice HTTP e indicazione di successo o errore.

## Interpretazione

EnergiBridge registra le misure dell'host mentre esegue JMeter. Sullo stesso
computer, i valori includono Tomcat, MySQL, JMeter e gli altri processi attivi.
Il CSV può includere anche l'avvio di JMeter e il tempo dopo l'ultima
richiesta: prima di calcolare l'energia per richiesta, allinea i timestamp
EnergiBridge con quelli del JTL e considera la finestra effettiva del test.

Ripeti ogni scenario separatamente e mantieni costanti utenti, ramp-up e
durata per confrontare i risultati. I risultati generati sono esclusi da Git;
questo README resta incluso.
