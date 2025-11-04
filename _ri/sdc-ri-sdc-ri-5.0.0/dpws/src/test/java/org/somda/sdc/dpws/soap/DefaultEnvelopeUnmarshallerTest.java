package org.somda.sdc.dpws.soap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.soap.model.Envelope;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultEnvelopeUnmarshallerTest extends DpwsTest {
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    void testUnmarshal() throws Exception {
        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        SoapMarshalling unmarshaller = getInjector().getInstance(SoapMarshalling.class);
        unmarshaller.startAsync().awaitRunning();
        Envelope actualEnv = unmarshaller.unmarshal(getClass().getResourceAsStream("soap-envelope.xml"));
        assertNotNull(actualEnv);
    }
}