package org.somda.sdc.dpws.soap.wsdiscovery;

import org.somda.sdc.dpws.soap.wsdiscovery.model.AppSequenceType;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Convenience class to represent WS-Discovery header information.
 *
 * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231847"
 * >Application Sequencing</a>
 */
public class WsDiscoveryHeader {
    private AppSequenceType appSequence;

    public Optional<AppSequenceType> getAppSequence() {
        return Optional.ofNullable(appSequence);
    }

    /**
     * Sets the wsd:AppSequence convenience header.
     *
     * @param appSequence to set to
     */
    public void setAppSequence(@Nullable AppSequenceType appSequence) {
        this.appSequence = appSequence;
    }
}
