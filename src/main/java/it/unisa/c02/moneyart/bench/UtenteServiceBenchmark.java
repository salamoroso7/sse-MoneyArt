package it.unisa.c02.moneyart.bench;

import it.unisa.c02.moneyart.gestione.utente.service.UtenteServiceImpl;
import it.unisa.c02.moneyart.model.beans.Utente;
import it.unisa.c02.moneyart.model.dao.NotificaDaoImpl;
import it.unisa.c02.moneyart.model.dao.OperaDaoImpl;
import it.unisa.c02.moneyart.model.dao.PartecipazioneDaoImpl;
import it.unisa.c02.moneyart.model.dao.UtenteDaoImpl;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
public class UtenteServiceBenchmark {

    @Param({"8", "32", "128"})
    private int passwordLength;

    private String rawPassword;
    private UtenteServiceImpl utenteService;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        this.rawPassword = "A".repeat(passwordLength);
        byte[] pwdHash = MessageDigest.getInstance("SHA-256")
                .digest(rawPassword.getBytes(StandardCharsets.UTF_8));

        Utente user = new Utente("Mario", "Rossi", null, "mario@mail.it",
                "mario", null, pwdHash, 500.0);
        user.setId(1);

        UtenteDaoImpl uDao = new UtenteDaoImpl(null) {
            @Override public Utente doRetrieveByUsername(String u) { return user; }
            @Override public Utente doRetrieveById(int id) { return user; }
        };

        this.utenteService = new UtenteServiceImpl(
                uDao, new OperaDaoImpl(null), new NotificaDaoImpl(null), new PartecipazioneDaoImpl(null));
    }

    @Benchmark
    public void baseline_checkUser(Blackhole bh) {
        bh.consume(utenteService.checkUser("mario", rawPassword));
    }
}