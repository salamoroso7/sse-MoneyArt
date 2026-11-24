package it.unisa.c02.moneyart.bench;

import it.unisa.c02.moneyart.model.beans.Asta;
import it.unisa.c02.moneyart.model.beans.Opera;
import it.unisa.c02.moneyart.utils.locking.AstaLockingSingleton;
import java.util.Date;
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
public class AstaLockingBenchmark {

    @Param({"10", "100", "1000"})
    private int distinctAuctions;

    private AstaLockingSingleton lockingSingleton;
    private Asta[] aste;
    private int index;

    @Setup(Level.Trial)
    public void setup() {
        this.lockingSingleton = AstaLockingSingleton.retrieveIstance();
        this.aste = new Asta[distinctAuctions];
        for (int i = 0; i < distinctAuctions; i++) {
            Opera op = new Opera();
            op.setId(i + 1);
            Asta a = new Asta(op, new Date(), new Date(System.currentTimeMillis() + 3600_000L), Asta.Stato.IN_CORSO);
            a.setId(i + 1);
            aste[i] = a;
        }
        this.index = 0;
    }

    @Benchmark
    public void baseline_lockAndUnlock(Blackhole bh) {
        Asta target = aste[index++ % distinctAuctions];
        lockingSingleton.lockAsta(target);
        try {
            bh.consume(target);
        } finally {
            lockingSingleton.unlockAsta(target);
        }
    }
}