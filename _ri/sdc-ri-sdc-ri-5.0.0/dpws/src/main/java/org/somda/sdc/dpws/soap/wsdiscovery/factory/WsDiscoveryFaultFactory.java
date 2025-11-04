package org.somda.sdc.dpws.soap.wsdiscovery.factory;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.MatchBy;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Factory to create WS-Discovery fault messages.
 */
public class WsDiscoveryFaultFactory {
    private final SoapFaultFactory soapFaultFactory;
    private final ObjectFactory wsdFactory;

    @Inject
    WsDiscoveryFaultFactory(SoapFaultFactory soapFaultFactory, ObjectFactory wsdFactory) {
        this.soapFaultFactory = soapFaultFactory;
        this.wsdFactory = wsdFactory;
    }

    /**
     * Creates a MatchingRuleNotSupported fault.
     *
     * @return a {@link SoapMessage} the encloses the fault.
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231831"
     * >Probe</a>
     */
    public SoapMessage createMatchingRuleNotSupported() {
        final List<String> supportedMatchBys = new ArrayList<>();
        Arrays.stream(MatchBy.values()).forEach(matchBy -> supportedMatchBys.add(matchBy.getUri()));

        return soapFaultFactory.createFault(
                WsDiscoveryConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsDiscoveryConstants.MATCHING_RULE_NOT_SUPPORTED,
                "The matching rule specified is not supported.",
                wsdFactory.createSupportedMatchingRules(supportedMatchBys));
    }
}
