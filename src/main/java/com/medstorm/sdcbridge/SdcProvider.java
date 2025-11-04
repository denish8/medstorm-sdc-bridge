package com.medstorm.sdcbridge;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.time.Instant;

/**
 * Lightweight provider:
 *  - stores latest metrics
 *  - tries to parse the MDIB XML with plain JDK DOM (no JAXB)
 *  - if XML is well-formed -> mdibReady = true
 *  - if not -> mdibReady = false, but app still runs
 */
@Component
public class SdcProvider {
    private static final Logger log = LoggerFactory.getLogger(SdcProvider.class);

    public record Snapshot(
            double pain,
            double awk,
            double nbv,
            double sc,
            int badSignal,
            Instant tsUtc
    ) {}

    @Value("${bridge.mdibPath:}")
    private String mdibPath;

    private volatile Snapshot last = null;
    private volatile boolean mdibReady = false;

    @PostConstruct
    public void start() {
        log.info("SdcProvider starting …");
        if (mdibPath == null || mdibPath.isBlank()) {
            log.warn("No MDIB path provided -> running in NO-MDIB mode.");
            return;
        }
        tryLoadMdibWithDom(mdibPath);
    }

    @PreDestroy
    public void stop() {
        log.info("SdcProvider stopped.");
    }

    public void updateAll(double pain,
                          double awk,
                          double nbv,
                          double sc,
                          int badSignal,
                          Instant tsUtc) {
        last = new Snapshot(pain, awk, nbv, sc, badSignal, tsUtc);
    }

    public Snapshot snapshot() {
        return last;
    }

    public boolean isMdibReady() {
        return mdibReady;
    }

    /**
     * "Is this a well-formed XML file I can read?" – that’s enough for /status.
     * No JAXB, no SDC classes, just DOM.
     */
    private void tryLoadMdibWithDom(String path) {
        File f = new File(path);
        if (!f.exists()) {
            log.error("MDIB file not found: {}", path);
            mdibReady = false;
            return;
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(f);

            String rootName = doc.getDocumentElement() != null
                    ? doc.getDocumentElement().getNodeName()
                    : "<no-root>";

            log.info("MDIB XML loaded OK from {} (root={})", path, rootName);
            mdibReady = true;
        } catch (Exception ex) {
            log.error("Failed to parse MDIB XML from {} – running in NO-MDIB mode. Reason: {}",
                    path, ex.toString());
            mdibReady = false;
        }
    }
}
