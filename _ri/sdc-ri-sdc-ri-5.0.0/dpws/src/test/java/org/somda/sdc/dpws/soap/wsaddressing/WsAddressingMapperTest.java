package org.somda.sdc.dpws.soap.wsaddressing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ObjectFactory;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WsAddressingMapperTest extends DpwsTest {

    private static final String REFERENCE = "no test for the wicked";

    private WsAddressingMapper mapper;
    private WsAddressingUtil wsaUtil;
    private ObjectFactory wsaFactory;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        this.mapper = getInjector().getInstance(WsAddressingMapper.class);
        this.wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
        this.wsaFactory = getInjector().getInstance(ObjectFactory.class);
    }

    @Test
    void testDuplicateJaxbElements() {

        var actionUri = "http://somda.org/someuri";
        var actionUri2 = "ws://somda.org/websocket";

        {
            var actualHeader = new ArrayList<>();
            actualHeader.add(wsaFactory.createAction(wsaUtil.createAttributedURIType(actionUri)));

            var headerContainer = new WsAddressingHeader();
            headerContainer.setAction(wsaUtil.createAttributedURIType(actionUri2));

            // mapping replaces the action value
            assertEquals(1, actualHeader.size());
            assertEquals(actionUri, ((JAXBElement<AttributedURIType>) actualHeader.get(0)).getValue().getValue());
            mapper.mapToJaxbSoapHeader(headerContainer, actualHeader);
            assertEquals(1, actualHeader.size());
            assertEquals(actionUri2, ((JAXBElement<AttributedURIType>) actualHeader.get(0)).getValue().getValue());
        }
        {
            var actualHeader = new ArrayList<>();

            var headerContainer = new WsAddressingHeader();
            headerContainer.setAction(wsaUtil.createAttributedURIType(actionUri));

            // mapping only adds header once
            assertEquals(0, actualHeader.size());
            mapper.mapToJaxbSoapHeader(headerContainer, actualHeader);
            assertEquals(1, actualHeader.size());
            mapper.mapToJaxbSoapHeader(headerContainer, actualHeader);
            assertEquals(1, actualHeader.size());
        }
    }

    @Test
    void testReferenceParameters() {
        // create an element to use as reference parameter
        var node = createNode();
        var actualHeader = new ArrayList<>();
        actualHeader.add(node);

        var headerContainer = new WsAddressingHeader();
        headerContainer.setMappedReferenceParameters(List.of(node));

        assertEquals(1, actualHeader.size());
        mapper.mapToJaxbSoapHeader(headerContainer, actualHeader);
        assertEquals(2, actualHeader.size());
        mapper.mapToJaxbSoapHeader(headerContainer, actualHeader);
        assertEquals(3, actualHeader.size());

        actualHeader.forEach(elem -> {
            assertTrue(
                    ((Element) elem).hasAttributeNS(
                            WsAddressingConstants.NAMESPACE,
                            WsAddressingConstants.IS_REFERENCE_PARAMETER.getLocalPart()
                    )
            );
        });
    }

    private Element createNode() {
        var fac = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = fac.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        var doc = builder.newDocument();

        var root = doc.createElementNS("ftp://namespace.example.com", "MyFunkyRoot");
        root.setTextContent(REFERENCE);
        return root;
    }
}
