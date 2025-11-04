package org.somda.sdc.dpws.device;

import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.service.HostedService;

/**
 * Interface to access the hosting service provided by a {@link Device}.
 * <p>
 * <i>Important note: there is no support to remove or alter hosted services that have been added before.</i>
 */
public interface HostingServiceAccess {
    /**
     * Adds the ThisModel definition.
     * <p>
     * According to the DPWS specification, certain information is limited in the number of characters:
     * <ul>
     * <li>{@link ThisDeviceType#getFriendlyName()} shall have fewer than
     * {@link org.somda.sdc.dpws.DpwsConstants#MAX_FIELD_SIZE} octets
     * <li>{@link ThisDeviceType#getFirmwareVersion()} shall have fewer than
     * {@link org.somda.sdc.dpws.DpwsConstants#MAX_FIELD_SIZE} octets
     * <li>{@link ThisDeviceType#getSerialNumber()} ()} shall have fewer than
     * {@link org.somda.sdc.dpws.DpwsConstants#MAX_FIELD_SIZE} octets
     * </ul>
     * <p>
     * <em>Attention: If those limits are exceeded, the underlying implementation may cut off
     * overflowing characters.</em>
     *
     * @param thisDevice the ThisModel information to set.
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672093"
     * >4.1 Characteristics</a>
     */
    void setThisDevice(ThisDeviceType thisDevice);

    /**
     * Updates the ThisModel definition.
     * <p>
     * According to the DPWS specification, certain information is limited in the number of characters:
     * <ul>
     * <li>{@link ThisModelType#getManufacturer()} ()} shall have fewer than
     * {@link org.somda.sdc.dpws.DpwsConstants#MAX_FIELD_SIZE} octets
     * <li>{@link ThisModelType#getModelName()} ()} shall have fewer than
     * {@link org.somda.sdc.dpws.DpwsConstants#MAX_FIELD_SIZE} octets
     * <li>{@link ThisModelType#getModelNumber()} ()} ()} shall have fewer than
     * {@link org.somda.sdc.dpws.DpwsConstants#MAX_FIELD_SIZE} octets
     * <li>{@link ThisModelType#getManufacturerUrl()} shall have fewer than
     * {@link org.somda.sdc.dpws.DpwsConstants#MAX_URI_SIZE} octets
     * <li>{@link ThisModelType#getModelUrl()} ()} shall have fewer than
     * {@link org.somda.sdc.dpws.DpwsConstants#MAX_URI_SIZE} octets
     * </ul>
     * <p>
     * <em>Attention: If those limits are exceeded, the underlying implementation may cut off
     * overflowing characters.</em>
     *
     * @param thisModel the ThisModel information to set.
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672093"
     * >4.1 Characteristics</a>
     */
    void setThisModel(ThisModelType thisModel);

    /**
     * Adds a hosted service definition.
     * <p>
     * <i>Attention: there is currently no mechanism that verifies if the hosted service was added before!</i>
     *
     * @param hostedService the hosted service definition to add.
     */
    void addHostedService(HostedService hostedService);
}
