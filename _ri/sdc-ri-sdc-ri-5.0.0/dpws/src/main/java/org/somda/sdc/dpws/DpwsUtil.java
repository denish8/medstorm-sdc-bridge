package org.somda.sdc.dpws;

import com.google.inject.Inject;
import org.somda.sdc.dpws.model.LocalizedStringType;
import org.somda.sdc.dpws.model.ObjectFactory;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Utility functions for DPWS.
 * <p>
 * Can be used to get convenient builders for DPWS' ThisDevice and ThisModel objects.
 */
public class DpwsUtil {
    private final ObjectFactory dpwsFactory;

    @Inject
    DpwsUtil(ObjectFactory dpwsFactory) {
        this.dpwsFactory = dpwsFactory;
    }

    /**
     * Creates a {@link ThisDeviceType} from given parameters.
     *
     * @param friendlyName    the device's friendly name.
     * @param firmwareVersion the device's firmware version.
     * @param serialNumber    the device's serial number.
     * @return the ThisDevice object.
     */
    public ThisDeviceType createThisDevice(List<LocalizedStringType> friendlyName,
                                           @Nullable String firmwareVersion,
                                           @Nullable String serialNumber) {
        ThisDeviceType devType = dpwsFactory.createThisDeviceType();
        devType.setFriendlyName(friendlyName);
        Optional.ofNullable(firmwareVersion).ifPresent(devType::setFirmwareVersion);
        Optional.ofNullable(serialNumber).ifPresent(devType::setSerialNumber);
        return devType;
    }

    /**
     * Creates a builder to set ThisDevice data by means of a fluent interface.
     *
     * @return fluent builder interface.
     */
    public ThisDeviceBuilder createDeviceBuilder() {
        return new ThisDeviceBuilder();
    }

    /**
     * Creates a builder to set ThisDevice data by means of a fluent interface.
     *
     * @param friendlyName a list of friendly names initially set.
     * @return fluent builder interface.
     */
    public ThisDeviceBuilder createDeviceBuilder(List<LocalizedStringType> friendlyName) {
        return new ThisDeviceBuilder(friendlyName);
    }

    /**
     * Creates a builder to set ThisModel data by means of a fluent interface.
     *
     * @return fluent builder interface.
     */
    public ThisModelBuilder createModelBuilder() {
        return new ThisModelBuilder();
    }

    /**
     * Creates a builder to set ThisDevice data by means of a fluent interface.
     *
     * @param manufacturer a list of manufacturer names initially set.
     * @param modelName    a list of model names initially set.
     * @return fluent builder interface.
     */
    public ThisModelBuilder createModelBuilder(List<LocalizedStringType> manufacturer,
                                               List<LocalizedStringType> modelName) {
        return new ThisModelBuilder(manufacturer, modelName);
    }

    /**
     * Creates a {@link ThisModelType} from given parameters.
     *
     * @param manufacturer    the manufacturer name.
     * @param manufacturerUrl the manufacturer URL.
     * @param modelName       the model name.
     * @param modelNumber     the model number.
     * @param modelUrl        the model URL.
     * @param presentationUrl the presentation URL.
     * @return the ThisModel object.
     */
    public ThisModelType createThisModel(List<LocalizedStringType> manufacturer,
                                         @Nullable String manufacturerUrl,
                                         List<LocalizedStringType> modelName,
                                         @Nullable String modelNumber,
                                         @Nullable String modelUrl,
                                         @Nullable String presentationUrl) {
        ThisModelType modType = dpwsFactory.createThisModelType();
        modType.setManufacturer(manufacturer);
        Optional.ofNullable(manufacturerUrl).ifPresent(modType::setManufacturerUrl);
        modType.setModelName(modelName);
        Optional.ofNullable(modelNumber).ifPresent(modType::setModelNumber);
        Optional.ofNullable(modelUrl).ifPresent(modType::setModelUrl);
        Optional.ofNullable(presentationUrl).ifPresent(modType::setPresentationUrl);
        return modType;
    }

    /**
     * Creates a builder to create localized texts.
     *
     * @param lang a language tag initially set.
     * @param text a text that matches the given language.
     * @return the localized text builder fluent interface.
     */
    public LocalizedStringsBuilder createLocalizedStrings(String lang, String text) {
        return new LocalizedStringsBuilder(lang, text);
    }

    /**
     * Creates a builder to create localized texts.
     *
     * @param text a text that matches the default language.
     * @return the localized text builder fluent interface.
     */
    public LocalizedStringsBuilder createLocalizedStrings(String text) {
        return new LocalizedStringsBuilder(text);
    }

    /**
     * Creates an empty builder with no predefined texts.
     *
     * @return the localized text builder fluent interface.
     */
    public LocalizedStringsBuilder createLocalizedStrings() {
        return new LocalizedStringsBuilder();
    }

}
