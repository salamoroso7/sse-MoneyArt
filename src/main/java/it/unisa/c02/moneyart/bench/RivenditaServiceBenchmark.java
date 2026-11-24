package it.unisa.c02.moneyart.bench;

import it.unisa.c02.moneyart.gestione.vendite.rivendite.service.RivenditaServiceImpl;
import it.unisa.c02.moneyart.model.beans.Asta;
import it.unisa.c02.moneyart.model.beans.Opera;
import it.unisa.c02.moneyart.model.beans.Partecipazione;
import it.unisa.c02.moneyart.model.beans.Rivendita;
import it.unisa.c02.moneyart.model.beans.Utente;
import it.unisa.c02.moneyart.model.dao.AstaDaoImpl;
import it.unisa.c02.moneyart.model.dao.OperaDaoImpl;
import it.unisa.c02.moneyart.model.dao.PartecipazioneDaoImpl;
import it.unisa.c02.moneyart.model.dao.RivenditaDaoImpl;
import it.unisa.c02.moneyart.model.dao.UtenteDaoImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
public class RivenditaServiceBenchmark {

    @Param({"5", "50", "200"})
    private int bidsCount;

    private RivenditaServiceImpl rivenditaService;
    private Opera singleOpera;

    @Setup(Level.Trial)
    public void setup() {
        Utente user = new Utente("Mario", "Rossi", null, "mario@mail.it",
                "mario", null, new byte[]{1}, 500.0);
        user.setId(1);

        this.singleOpera = new Opera("OperaTest", "Desc", Opera.Stato.ALL_ASTA, null, user, user, "cert");
        this.singleOpera.setId(1);

        Asta singleAsta = new Asta(singleOpera, new Date(),
                new Date(System.currentTimeMillis() + 3600_000L), Asta.Stato.IN_CORSO);
        singleAsta.setId(1);

        List<Partecipazione> partecipazioni = new ArrayList<>(bidsCount);
        for (int i = 1; i <= bidsCount; i++) {
            partecipazioni.add(new Partecipazione(singleAsta, user, 100.0 + i));
        }
        singleAsta.setPartecipazioni(partecipazioni);

        List<Asta> asteList = Collections.singletonList(singleAsta);
        List<Rivendita> rivenditeList = Collections.singletonList(
                new Rivendita(singleOpera, Rivendita.Stato.IN_CORSO, 150.0));

        UtenteDaoImpl uDao = new UtenteDaoImpl(null) {
            @Override public Utente doRetrieveById(int id) { return user; }
            @Override public List<Utente> doRetrieveFollowersByUserId(int id) {
                return Collections.singletonList(user);
            }
        };
        OperaDaoImpl oDao = new OperaDaoImpl(null) {
            @Override public Opera doRetrieveById(int id) { return singleOpera; }
        };
        AstaDaoImpl aDao = new AstaDaoImpl(null) {
            @Override public List<Asta> doRetrieveByOperaId(int id) { return asteList; }
        };
        PartecipazioneDaoImpl pDao = new PartecipazioneDaoImpl(null) {
            @Override public List<Partecipazione> doRetrieveAllByAuctionId(int id) { return partecipazioni; }
        };
        RivenditaDaoImpl rDao = new RivenditaDaoImpl(null) {
            @Override public List<Rivendita> doRetrieveAll(String f) { return rivenditeList; }
        };

        this.rivenditaService = new RivenditaServiceImpl(uDao, oDao, rDao, null, aDao, pDao);
    }

    @Benchmark
    public void baseline_getResellPrice(Blackhole bh) {
        bh.consume(rivenditaService.getResellPrice(singleOpera));
    }
}