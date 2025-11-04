package org.somda.sdc.dpws;

import org.somda.sdc.dpws.model.LocalizedStringType;
import org.somda.sdc.dpws.model.ObjectFactory;
import org.somda.sdc.dpws.model.ThisDeviceType;

import java.util.List;

/**
 * Convenient class to build ThisDevice.
 */
public class ThisDeviceBuilder {
    private final ThisDeviceType thisDevice;

    /**
     * Default constructor.
     */
    public ThisDeviceBuilder() {
        thisDevice = (new ObjectFactory()).createThisDeviceType();
    }

    /**
     * Constructor with predefined friendly name.
     *
     * @param friendlyName the device friendly name.
     */
    public ThisDeviceBuilder(List<LocalizedStringType> friendlyName) {
        this();
        thisDevice.setFriendlyName(friendlyName);
    }

    /**
     * Sets the friendly names.
     *
     * @param friendlyName names to set
     * @return this builder
     */
    public ThisDeviceBuilder setFriendlyName(List<LocalizedStringType> friendlyName) {
        thisDevice.setFriendlyName(friendlyName);
        return this;
    }

    /**
     * Sets the firmware version.
     *
     * @param firmwareVersion to set
     * @return this builder
     */
    public ThisDeviceBuilder setFirmwareVersion(String firmwareVersion) {
        thisDevice.setFirmwareVersion(firmwareVersion);
        return this;
    }

    /**
     * Sets the serial number.
     *
     * @param serialNumber to set
     * @return this builder
     */
    public ThisDeviceBuilder setSerialNumber(String serialNumber) {
        thisDevice.setSerialNumber(serialNumber);
        return this;
    }

    /**
     * Gets the actual device type.
     *
     * @return the internally stored device type. Caution: changes afterwards in the fluent interface will affect
     * this returned value.
     */
    public ThisDeviceType get() {
        return thisDevice;
    }
}
