package org.somda.sdc.dpws.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.ThisDeviceBuilder;
import org.somda.sdc.dpws.ThisModelBuilder;
import org.somda.sdc.dpws.model.LocalizedStringType;
import org.somda.sdc.dpws.service.factory.HostingServiceFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class HostingServiceInterceptorTest extends DpwsTest {

    @BeforeEach
    void beforeEach() throws Exception {
        setUp();
    }

    @Test
    void thisDeviceMaxSizes() {
        var hostingServiceInterceptor = getInjector().getInstance(HostingServiceFactory.class)
                .createHostingService(mock(WsDiscoveryTargetService.class));

        var thisDeviceType = new ThisDeviceBuilder()
                .setFriendlyName(createTexts(3, DpwsConstants.MAX_FIELD_SIZE, "a"))
                .setSerialNumber(createText(DpwsConstants.MAX_FIELD_SIZE + 50, "b"))
                .setFirmwareVersion(createText(DpwsConstants.MAX_FIELD_SIZE + 100, "c"))
                .get();

        hostingServiceInterceptor.setThisDevice(thisDeviceType);
        var actualThisDevice = hostingServiceInterceptor.getThisDevice();

        checkTexts(actualThisDevice.getFriendlyName(), DpwsConstants.MAX_FIELD_SIZE, "a");
        checkText(actualThisDevice.getSerialNumber(), DpwsConstants.MAX_FIELD_SIZE, "b");
        checkText(actualThisDevice.getFirmwareVersion(), DpwsConstants.MAX_FIELD_SIZE, "c");
    }

    @Test
    void thisModelMaxSizes() {
        var hostingServiceInterceptor = getInjector().getInstance(HostingServiceFactory.class)
                .createHostingService(mock(WsDiscoveryTargetService.class));

        var thisModelType = new ThisModelBuilder()
                .setManufacturer(createTexts(3, DpwsConstants.MAX_FIELD_SIZE, "d"))
                .setModelName(createTexts(3, DpwsConstants.MAX_FIELD_SIZE + 50, "e"))
                .setModelNumber(createText(DpwsConstants.MAX_FIELD_SIZE, "f"))
                .setPresentationUrl(createText(DpwsConstants.MAX_URI_SIZE, "g"))
                .setManufacturerUrl(createText(DpwsConstants.MAX_URI_SIZE, "h"))
                .setModelUrl(createText(DpwsConstants.MAX_URI_SIZE, "i"))
                .get();

        hostingServiceInterceptor.setThisModel(thisModelType);
        var actualThisModel = hostingServiceInterceptor.getThisModel();

        checkTexts(actualThisModel.getManufacturer(), DpwsConstants.MAX_FIELD_SIZE, "d");
        checkTexts(actualThisModel.getModelName(), DpwsConstants.MAX_FIELD_SIZE, "e");
        checkText(actualThisModel.getModelNumber(), DpwsConstants.MAX_FIELD_SIZE, "f");
        checkText(actualThisModel.getPresentationUrl(), DpwsConstants.MAX_URI_SIZE, "g");
        checkText(actualThisModel.getManufacturerUrl(), DpwsConstants.MAX_URI_SIZE, "h");
        checkText(actualThisModel.getModelUrl(), DpwsConstants.MAX_URI_SIZE, "i");
    }

    void checkTexts(List<LocalizedStringType> texts, int size, String repeatedSequence) {
        for (var text : texts) {
            checkText(text.getValue(), size, repeatedSequence);
        }
    }

    void checkText(String text, int size, String repeatedSequence) {
        assertEquals(size - 1, text.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(createText(size - 1, repeatedSequence), text);
    }

    private List<LocalizedStringType> createTexts(int count, int length, String sequenceToRepeat) {
        var texts = new ArrayList<LocalizedStringType>(count);
        for (int i = 0; i < count; ++i) {
            var localizedStringType = new LocalizedStringType();
            localizedStringType.setValue(createText(length, sequenceToRepeat));
            texts.add(localizedStringType);
        }

        return texts;
    }

    private String createText(int length, String sequenceToRepeat) {
        return sequenceToRepeat.repeat(length);
    }
}