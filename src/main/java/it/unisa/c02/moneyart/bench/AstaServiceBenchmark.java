package it.unisa.c02.moneyart.bench;

import it.unisa.c02.moneyart.gestione.vendite.aste.service.AstaServiceImpl;
import it.unisa.c02.moneyart.model.beans.Asta;
import it.unisa.c02.moneyart.model.beans.Opera;
import it.unisa.c02.moneyart.model.beans.Partecipazione;
import it.unisa.c02.moneyart.model.beans.Utente;
import it.unisa.c02.moneyart.model.dao.AstaDaoImpl;
import it.unisa.c02.moneyart.model.dao.OperaDaoImpl;
import it.unisa.c02.moneyart.model.dao.PartecipazioneDaoImpl;
import it.unisa.c02.moneyart.model.dao.UtenteDaoImpl;
import it.unisa.c02.moneyart.utils.locking.AstaLockingSingleton;
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
public class AstaServiceBenchmark {

    @Param({"10", "100", "500"})
    private int auctionCount;

    @Param({"ASC", "DESC"})
    private String sortOrder;

    private AstaServiceImpl astaService;

    @Setup(Level.Trial)
    public void setup() {
        List<Asta> asteList = new ArrayList<>(auctionCount);
        for (int i = 1; i <= auctionCount; i++) {
            Utente artist = new Utente("Nome" + i, "Cognome" + i, null, "mail" + i + "@test.it",
                    "artist" + i, null, new byte[]{1, 2, 3}, 100.0 * i);
            artist.setId(i);
            artist.setnFollowers(i * 3);

            Opera opera = new Opera("Opera" + i, "Desc", Opera.Stato.ALL_ASTA, null, artist, artist, "cert");
            opera.setId(i);

            Asta asta = new Asta(opera, new Date(),
                    new Date(System.currentTimeMillis() + 3600_000L), Asta.Stato.IN_CORSO);
            asta.setId(i);
            asta.setPartecipazioni(
                    Collections.singletonList(new Partecipazione(asta, artist, 50.0 + i)));
            asteList.add(asta);
        }

        Utente dummyFollower = new Utente("F", "L", null, "f@test.it", "fol", null, new byte[]{1}, 10.0);
        dummyFollower.setId(999);
        UtenteDaoImpl uDao = new UtenteDaoImpl(null) {
            @Override public Utente doRetrieveById(int id) { return dummyFollower; }
            @Override public List<Utente> doRetrieveFollowersByUserId(int id) {
                return Collections.singletonList(dummyFollower);
            }
        };
        OperaDaoImpl oDao = new OperaDaoImpl(null) {
            @Override public Opera doRetrieveById(int id) { return asteList.get(0).getOpera(); }
        };
        AstaDaoImpl aDao = new AstaDaoImpl(null) {
            @Override public List<Asta> doRetrieveAll(String f) { return new ArrayList<>(asteList); }
            @Override public List<Asta> doRetrieveByStato(Asta.Stato s) { return new ArrayList<>(asteList); }
            @Override public List<Asta> doRetrieveByOperaId(int id) { return new ArrayList<>(asteList); }
        };
        PartecipazioneDaoImpl pDao = new PartecipazioneDaoImpl(null) {
            @Override public List<Partecipazione> doRetrieveAllByAuctionId(int id) {
                return asteList.get(0).getPartecipazioni();
            }
        };

        this.astaService = new AstaServiceImpl(
                aDao, oDao, uDao, pDao, null, AstaLockingSingleton.retrieveIstance(), null);
    }

    @Benchmark
    public void baseline_getAuctionsSortedByPrice(Blackhole bh) {
        bh.consume(astaService.getAuctionsSortedByPrice(sortOrder, Asta.Stato.IN_CORSO));
    }

    @Benchmark
    public void baseline_getAuctionsSortedByArtistFollowers(Blackhole bh) {
        bh.consume(astaService.getAuctionsSortedByArtistFollowers(sortOrder, Asta.Stato.IN_CORSO));
    }
}