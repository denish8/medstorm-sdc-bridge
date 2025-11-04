package org.somda.sdc.dpws.device.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.dpws.device.DeviceSettings;

/**
 * Factory to create {@linkplain Device} instances.
 */
public interface DeviceFactory {
    /**
     * Creates a new {@linkplain Device} instance.
     *
     * @param deviceSettings the settings to be applied on the device.
     * @return a new {@linkplain Device} instance, not running. Use {@link Device#startAsync()} to start the device.
     */
    Device createDevice(@Assisted DeviceSettings deviceSettings);
}
