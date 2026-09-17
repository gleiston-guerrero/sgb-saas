package com.uteq.backend.repository;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Set;

/**
 * Funciones solo-test para H2 (CREATE ALIAS): aproximan
 * {@code similarity()} de pg_trgm y {@code date_trunc()} de PostgreSQL.
 * No replican valores exactos del motor (la equivalencia real está
 * probada contra PG en P5SpikeIT/P5TabularSpikeIT); existen para que la
 * suite por defecto ejercite las rutas Criteria/JPQL que las invocan.
 */
public class H2Funciones {

    private H2Funciones() {
    }

    /** Jaccard sobre bigramas (0..1). */
    public static double similarity(String a, String b) {
        if (a == null || b == null) {
            return 0;
        }
        Set<String> ba = bigramas(a.toLowerCase());
        Set<String> bb = bigramas(b.toLowerCase());
        if (ba.isEmpty() || bb.isEmpty()) {
            return 0;
        }
        Set<String> inter = new HashSet<>(ba);
        inter.retainAll(bb);
        return (2.0 * inter.size()) / (ba.size() + bb.size());
    }

    private static Set<String> bigramas(String s) {
        Set<String> out = new HashSet<>();
        String t = " " + s + " ";
        for (int i = 0; i + 1 < t.length(); i++) {
            out.add(t.substring(i, i + 2));
        }
        return out;
    }

    /** Trunca a día/semana (lunes)/mes conservando el offset. */
    public static OffsetDateTime date_trunc(String campo, OffsetDateTime ts) {
        if (ts == null) {
            return null;
        }
        var ldt = ts.toLocalDateTime();
        var truncado = switch (campo) {
            case "week" -> ldt.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .truncatedTo(ChronoUnit.DAYS);
            case "month" -> ldt.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            default -> ldt.truncatedTo(ChronoUnit.DAYS);
        };
        return truncado.atOffset(ts.getOffset());
    }
}
