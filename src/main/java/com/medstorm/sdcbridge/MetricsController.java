// package com.medstorm.sdcbridge;

// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;
// import java.time.Instant;

// @RestController
// public class MetricsController {
//   record MetricsDTO(double pain, double awk, double nbv, double sc, int badSignal, Instant ts) {}
//   private final SdcProvider provider;
//   public MetricsController(SdcProvider p) { this.provider = p; }

//   @PostMapping("/metrics")
//   public ResponseEntity<?> post(@RequestBody MetricsDTO d) {
//     double pain = clamp(d.pain(), 0, 10);
//     double awk  = clamp(d.awk(), 0, 100);
//     double nbv  = clamp(d.nbv(), 0, 10);
//     double sc   = clamp(d.sc(), 0, 250);
//     int bs      = d.badSignal() != 0 ? 1 : 0;
//     provider.updateAll(pain, awk, nbv, sc, bs, d.ts() != null ? d.ts() : Instant.now());
//     return ResponseEntity.accepted().build();
//   }

//   @GetMapping("/status")
//   public ResponseEntity<?> status() {
//     var v = provider.snapshot();
//     return ResponseEntity.ok(new Object(){ public final boolean sdcReady = false; public final Object last = v; });
//   }
//   private static double clamp(double v, double lo, double hi){ return Math.max(lo, Math.min(hi, v)); }
// }


package com.medstorm.sdcbridge;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
public class MetricsController {

    record MetricsDTO(double pain, double awk, double nbv, double sc, int badSignal, Instant ts) {}

    private final SdcProvider provider;

    public MetricsController(SdcProvider p) {
        this.provider = p;
    }

    @PostMapping("/metrics")
    public ResponseEntity<?> post(@RequestBody MetricsDTO d) {
        double pain = clamp(d.pain(), 0, 10);
        double awk  = clamp(d.awk(), 0, 100);
        double nbv  = clamp(d.nbv(), 0, 10);
        double sc   = clamp(d.sc(), 0, 250);
        int bs      = d.badSignal() != 0 ? 1 : 0;
        provider.updateAll(
                pain,
                awk,
                nbv,
                sc,
                bs,
                d.ts() != null ? d.ts() : Instant.now()
        );
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        var v = provider.snapshot();
        boolean ready = provider.isMdibReady();
        return ResponseEntity.ok(new Object() {
            public final boolean sdcReady = ready;
            public final Object last = v;
        });
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
