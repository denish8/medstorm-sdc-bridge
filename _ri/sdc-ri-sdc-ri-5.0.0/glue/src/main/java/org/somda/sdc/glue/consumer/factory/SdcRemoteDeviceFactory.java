package org.somda.sdc.glue.consumer.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import org.somda.sdc.glue.consumer.SdcRemoteDeviceWatchdog;
import org.somda.sdc.glue.consumer.localization.LocalizationServiceProxy;
import org.somda.sdc.glue.consumer.report.ReportProcessor;
import org.somda.sdc.glue.consumer.sco.ScoController;

import javax.annotation.Nullable;

public interface SdcRemoteDeviceFactory {
    SdcRemoteDevice createSdcRemoteDevice(@Assisted HostingServiceProxy hostingServiceProxy,
                                          @Assisted RemoteMdibAccess remoteMdibAccess,
                                          @Assisted ReportProcessor reportProcessor,
                                          @Assisted @Nullable ScoController scoController,
                                          @Assisted SdcRemoteDeviceWatchdog watchdog,
                                          @Assisted @Nullable LocalizationServiceProxy localizationServiceProxy);
}
