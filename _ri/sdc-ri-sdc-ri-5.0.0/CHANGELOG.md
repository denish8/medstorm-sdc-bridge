# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [5.0.0] - 2022-09-05

### Added

- `org.somda.sdc.dpws.device.WebService.getEventSources()`. (#268)
- `org.somda.sdc.dpws.service.EventSink.subscribe()` overload to allow for event sinks to generically subscribe to an event source (non-action based). (#268)
- `org.somda.sdc.dpws.soap.wseventing.SubscriptionManager.getFilters()` and `org.somda.sdc.dpws.soap.wseventing.SubscriptionManager.getFilterDialect()`. (#268)
- `org.somda.sdc.dpws.soap.wseventing.EventSourceInterceptorDispatcher` to dispatch WS-Eventing requests to dedicated filter dialect handlers. (#268)
- `SubscriptionRegistryFactory`, `EventSourceInterceptorDispatcherFactory`, and `GenericEventSourceInterceptorFactory` to `org.somda.sdc.dpws.soap.wseventing.factory`. (#268)
- `org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager.offerEndTo(WsEventingStatus)`. (#268)
- `org.somda.sdc.dpws.soap.wseventing.Subscriptions` for read access to subscription managers. (#268)
- Parameter for `org.somda.sdc.glue.provider.factory.SdcDeviceFactory.createSdcDevice()` to add a list of custom hosted services, which are started in addition to the SDC services. (#268)
- `org.somda.sdc.dpws.DpwsConfig#HTTP_SERVER_THREAD_POOL_SIZE`: configuration parameter to set the max size of the http server thread pool. (#274)
- `org.somda.sdc.dpws.DpwsConfig#HTTP_CLIENT_POOL_SIZE`: configuration parameter to set the max size of the http client pool. (#274)
- `org.somda.sdc.dpws.DpwsConfig#HTTP_CLIENT_RETRY_POST`: configuration parameter to enable potentially dangerous transmission retries of non-idempotent http requests. (#274)
- `org.somda.sdc.glue.provider.sco.ScoController.processIncomingSetOperation`: optional callerId parameter to uniquely identify the calling consumer. (#277) 
- `org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager`: optional callerId parameter to uniquely identify the subscribing consumer. (#277)

### Changed

- `org.somda.sdc.dpws.device.WebService.setEventSource()` to `org.somda.sdc.dpws.device.WebService.registerEventSource()`, which allows for registration of multiple event sources (one per filter dialect). (#268)
- Renamed `org.somda.sdc.dpws.soap.wseventing.EventSourceInterceptor` to `org.somda.sdc.dpws.soap.wseventing.ActionBasedEventSource`. (#263)
- Renamed `org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager.sendToEndTo` to `org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager.offerEndTo`. (#263)
- `org.somda.sdc.dpws.soap.wseventing.SubscriptionManager.getFilters()` returns a list instead of a collection. (#263)
- `org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry` implements `org.somda.sdc.dpws.soap.wseventing.Subscriptions`. (#268)
- `org.somda.sdc.dpws.device.WebService` implementations do not provide a default `EventSource` implementation. (#268)
- Subscription expiration detection is done by using `Instant` instead of `LocalDateTime`. (#268)
- Use extracted sdc-ri-model. (#279) 
- `ScoTransaction` returns `MdibVersion` associated with received `OperationInvokedReport.ReportPart`s. (#282)
- `org.somda.sdc.dpws.soap.CommunicationContext` contains `org.somda.sdc.dpws.CommunicationLogContext` instances when possible. (#271)
- Constructors and methods that previously passed through an optional custom `org.somda.sdc.dpws.CommunicationLog` instance now accept an optional `org.somda.sdc.dpws.CommunicationLogContext`. (#271)

### Removed

- `org.somda.sdc.dpws.soap.wseventing.SubscriptionManager.getActions()`. (#268)
- biceps-model, dpws-model, mdpws-model, mdpws and parts of the common-model modules. (#279)
- `org.somda.sdc.dpws.factory.CommunicationLogFactory#createCommunicationLog(CommunicationLogContext)` in favor of passing it through `org.somda.sdc.dpws.soap.CommunicationContext`. (#271)

### Fixed

- Removal of subscriptions did not unregister HTTP handler. (#268)
- `org.somda.sdc.dpws.DpwsConfig#HTTP_SERVER_COMMLOG_IN_HANDLER` to use the communication log in jetty server handlers instead of the entire jetty server.
- `org.somda.sdc.glue.provider.services.HighPriorityServices` did not set InvocationSource correctly. (#277)
- `org.somda.sdc.glue.consumer.SdcRemoteDeviceWatchdog` shut down executor of AsyncEventQueue. (#278)
- `org.somda.sdc.dpws.udp.UdpBindingServiceImpl` defaults to IPv6 multicast when using Java 15+ causing an Exception on network interfaces without IPv6 on Windows. (#266)
- Consumers creating two separate HTTP Servers for each subscription. (#271)
- Consumers creating separate HTTP Clients for each hosted service and an additional client for other requests. (#271)

## [4.0.0] - 2023-03-10

### Added

- Support for MDIBs with multiple Mds. (#35)
- `org.somda.sdc.dpws.crypto.CachingCryptoSettings` to allow caching of SSLContext instances for CryptoSettings. (#239)
- `org.somda.sdc.dpws.DpwsConfig#HTTP_CONTENT_TYPE_CHARSET`: configuration parameter to set the charset in the http content-type header. (#256)
- Support for MDS filtering in GetContextStates. (#139)
- `org.somda.sdc.biceps.common.FindExtensions` to conveniently retrieve extensions from BICEPS model elements. (#262)

### Changed

- `org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient` and `org.somda.sdc.dpws.client.DiscoveryFilter` to support manual configuration of MatchBy. (#263)

### Removed

- `setMatchBy` and `getMatchBy` from `org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService`. (#263)
- Setting MatchBy for Hello and Bye messages. (#263)

### Fixed

- `org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryUtil.isScopesMatching()` did not allow for empty segments in the path. (#258)
- Jetty servers without TLS could provide a hostname instead of an ip. (#261)
- `org.somda.sdc.dpws.soap.wseventing.EventSinkImpl` did not unregister http handlers after ending subscriptions. (#257)

## [3.0.0] - 2022-12-06

### Added

- `org.somda.sdc.dpws.DpwsConfig#MULTICAST_TTL`: configuration parameter to set the time-to-live for outgoing multicast packets. (#240)
- `org.somda.sdc.glue.provider.localization` and `org.somda.sdc.glue.consumer.localization` packages to support Localization service. (#141)
- Java 17 support. (#233)
- Added `org.somda.sdc.biceps.common.CodedValueUtil` which enables comparisons of CodedValues according to BICEPS.
- Support for `org.somda.sdc.dpws.CommunicationLogContext` to provide additional context information for communication logs on consumer side. (#221)

### Changed

- Replace `org.somda.sdc.common.util.ObjectUtil` using clone and copy provided by the `jaxb2-rich-contract-plugin`. (#224)
- Reduced log level of network interface information on DPWS framework startup in `org.somda.sdc.dpws.DpwsFrameworkImpl`. (#245)
- Avoid use of reflection in `org.somda.sdc.biceps.common.MdibTypeValidator` to improve performance. (#248)
- `org.somda.sdc.dpws.soap.NotificationSink` throws `SoapFaultException`. (#252)

### Removed

- `org.somda.sdc.glue.common.helper.UrlUtf8` `.decode` and `.encode` methods (#247)

### Fixed

- `org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryClientInterceptor` correctly handles multiple concurrent Probe or Resolve operations. (#243)
- Removed useless host name resolution in `org.somda.sdc.dpws.udp.UdpBindingServiceImpl` causing delay on every SDCri stack start. (#246)
- `org.somda.sdc.glue.common.helper.UrlUtf8` encoding did not follow RFC2398 or RFC3986 rules, queries could contain & which must be escaped. (#247)
- `org.somda.sdc.glue.consumer.SdcRemoteDeviceWatchdog` EventBus could be interrupted by shutdown of watchdog. (#253)
- `org.somda.sdc.glue.consumer.sco.helper.OperationInvocationDispatcher` concurrent modification in `sanitizeAwaitingTransactions` (#254)
- Cancel futures if they raise TimeoutException. (#255)

## [2.0.0] - 2022-03-17

### Added

- `org.somda.sdc.common.util.AnyDateTime` and `org.somda.sdc.common.util.AnyDateTimeAdapter` to fully support XML Schema DateTime. (#151)
- `org.somda.sdc.common.logging.InstanceLogger` to provide an instance identifier in all log messages. (#156)
- `org.somda.sdc.common.CommonConfig` to configure the instance identifier for the logger. (#156)
- `org.somda.sdc.dpws.wsdl.WsdlMarshalling` to marshal and unmarshal WSDL documents. (#161)
- `DeviceConfig.WSDL_PROVISIONING_MODE` to allow device-side configuration of different WSDL provisioning modes in accordance with WS-MetadataExchange. (#161)
- `org.somda.sdc.dpws.http.HttpClient` interface for generic http requests. (#165)
- `org.somda.sdc.dpws.wsdl.WsdlRetriever` to retrieve WSDLs from services using multiple methods. (#165)
- `org.somda.sdc.dpws.http.apache.ClientTransportBinding` and `org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler` added chunked flag to enforce chunked outgoing requests and chunked outgoing responses. Only one big chunk is used instead of splitting up, since it is currently only needed for testing purposes. (#173)
- `org.somda.sdc.biceps.common.storage.MdibStorageImpl` can be configured not to remove not associated context states from storage. (#183)
- `org.somda.sdc.biceps.common.storage.MdibStorageImpl` can be configured not to create descriptors for state updates missing descriptors. (#183)
- `org.somda.sdc.glue.consumer.report.ReportProcessor` can be configured to apply reports which have the same MDIB version as the current MDIB. (#183)
- `org.somda.sdc.biceps.common.access.MdibAccess` method added to retrieve a list of states of a specific type. (#195)
- `org.somda.sdc.biceps.common.storage.MdibStorage` method added to retrieve a list of states of a specific type. (#195)
- `org.somda.sdc.biceps.consumer.preprocessing.DuplicateContextStateHandleHandler` to detect duplicate context state handles in MdibStateModifications. (#196)
- `org.somda.sdc.biceps.consumer.access.RemoteMdibAccessImpl` and `org.somda.sdc.biceps.provider.access.LocalMdibAccessImpl` can be configured to specify which DescriptionPreprocessingSegments and StatePreprocessingSegments should be used for consumer or provider. (#196)
- `org.somda.sdc.dpws.DpwsConfig#COMMUNICATION_LOG_WITH_HTTP_REQUEST_RESPONSE_ID`: configuration parameter to enable/disable generating HTTP request response transaction identifiers in communication logs. (#203)
- `org.somda.sdc.dpws.DpwsConfig#COMMUNICATION_LOG_PRETTY_PRINT_XML`: configuration parameter to enable/disable pretty printing in communication logs. (#203)
- `org.somda.sdc.dpws.soap.TransportInfo.getRemoteNodeInfo()`  to retrieve a remote node's requested scheme, address 
  and port. (#208)
- `org.somda.sdc.dpws.http.helper.HttpServerClientSelfTest` to perform HTTP server & client connection self-test 
   and print certificate information during `DpwsFramework` startup. (#113)
- `org.somda.sdc.common.event.EventBus` as a replacement for `com.google.common.eventbus.EventBus` to support unregistering all observers at once. (#229)
- `org.somda.sdc.glue.consumer.SdcRemoteDevicesConnector` method added to pass an `MdibAccessObserver` when connecting,
   to enable observing/reacting to the initial MDIB being fetched from the device. (#227)
  
### Changed

- Use of `io.github.threetenjaxb.core.LocalDateTimeXmlAdapter` to `org.somda.sdc.common.util.AnyDateTimeAdapter` for any XML Schema DateTime in module `biceps-model`. (#151)
- Use log4j2-api instead of slf4j for logging. (#156)
- Communication log file names to include SOAP action information and XML to be pretty-printed. (#153)
- `GetContainmentTree` handling changed in order to allow traversal of the MDIB. (#150)
- Change names in `org.somda.sdc.dpws.soap.wseventing.WsEventingConstants` from `WSE_ACTION[...]` to `WSA_ACTION[...]`. (#157)
- `org.somda.sdc.common.util.ExecutorWrapperService` are bound as guice `Provider`. (#156)
- `org.somda.sdc.glue.consumer.helper.LogPrepender` replaced by `HostingServiceLogger`. (#156)
- Evaluate HTTP Content-Type header element to determine the appropriate charset for received messages. (#170)
- `org.somda.sdc.common.guice.DefaultHelperModule` renamed to `DefaultCommonModule`. (#160)
- `org.somda.sdc.dpws.soap.exception.SoapFaultException` is now compliant with DPWS R0040. (#181)
- `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingHeader` changed relatesTo element to RelatesToType instead of an AttributedURIType. (#184)
- `org.somda.sdc.dpws.CommunicationLog` MessageType enum added, to mark messages as i.e. request, response. (#188)
- `org.somda.sdc.dpws.soap.HttpApplicationInfo` additional transactionId added, to associate request response messages. (#188)
- `org.somda.sdc.dpws.soap.HttpApplicationInfo` additional requestUri added, to determine the used POST address. (#190)
- `org.somda.sdc.glue.GlueConstants` moved URI related regex constants to `org.somda.sdc.dpws.DpwsContants`. (#190)
- `org.somda.sdc.glue.provider.sco.ScoController` to process lists independent of a specific list type as otherwise 
  activate operations do not integrate well. (#207) 
- `org.somda.sdc.dpws.soap.interception.RequestResponseObject` return non-optional `CommunicationContext` instances. (#208)
- `org.somda.sdc.glue.consumer.SdcRemoteDevicesConnectorImpl.disconnect()` now sends `RemoteDeviceDisconnectedMessage` message 
  if device lost connection or was disconnected. (#216)
- `org.somda.sdc.biceps.common.storage.MdibStorageImpl.deleteEntity()` updates parent entity and returns it in the updated entity list 
  if child descriptor is deleted (#211)
- `org.somda.sdc.common.util.AnyDateTime` methods `equals()`, `hashCode()`, `toString()` implemented (#201)
- `org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryUtil.isScopesMatching()` extends scope matcher to be compatible with RFC3986 URIs 
  and follow WS-Discovery rules (#212)
- `org.somda.sdc.dpws.http.apache.CommunicationLogHttpRequestInterceptor` adds local certificates to TransportInfo and `org.somda.sdc.dpws.http.apache.CommunicationLogHttpResponseInterceptor` adds peer certificates to TransportInfo. (#204)
- `org.somda.sdc.dpws.client.helper.HostingServiceResolver` detects mismatches between the EPR from WS-Discovery and 
  the EPR from the WS-Transfer Get responses and subsequently throws an `EprAddressMismatchException`. (#230)

### Removed

- `org.somda.sdc.dpws.CommunicationLogSink.getTargetStream()`; use `org.somda.sdc.dpws.CommunicationLogSink.createTargetStream()` instead. (#153)
- `org.somda.sdc.common.util.ExecutorWrapperUtil`; `org.somda.sdc.common.util.ExecutorWrapperServices` are bound as guice `Provider`. (#156)
- `org.somda.sdc.dpws.service.HostedService.getWsdlLocations()` as data is exclusively accessible through `getWsdlDocument()`. (#161)
- `org.somda.sdc.dpws.crypto.CryptoSettings.getKeyStoreFile()`; use `org.somda.sdc.dpws.crypto.CryptoSettings.getKeyStoreStream()` instead. (#206)
- `org.somda.sdc.dpws.crypto.CryptoSettings.getTrustStoreFile()`; use `org.somda.sdc.dpws.crypto.CryptoSettings.getTrustStoreStream()` instead. (#206)
- remove deprecated code (#137)

### Fixed

- added `ActionConstants.ACTION_SYSTEM_ERROR_REPORT` to the list `ConnectConfiguration.EPISODIC_REPORTS` as SystemErrorReports are EpisodicReports. (#222)
- `org.somda.sdc.dpws.soap.wseventing.EventSourceInterceptor` no longer tries to send SubscriptionEnd messages to stale subscriptions on shutdown. (#164)
- `IEEE11073-20701-LowPriority-Services.wsdl` specified the wrong input and output messages for `GetStatesFromArchive` operation. (#167)
- Namespace prefix mappings which were missing for SDC Glue-related XML fragments. (#169)
- `org.somda.sdc.dpws.http.jetty.CommunicationLogHandlerWrapper` determined TLS usage by whether CryptoSettings were present, not based on request. (#171)
- `org.somda.sdc.dpws.http.jetty.JettyHttpServerRegistry` is now compliant with RFC 2616 instead of RFC 7230. (#172)
- `org.somda.sdc.biceps.consumer.preprocessing.VersionDuplicateHandler` can now handle implied state versions. (#182)
- Fix swallowed state updates during description modification. (#179) 
- Prevent suspension of notification source and duplication of notification messages. (#179) 
- `org.somda.sdc.biceps.common.storage.MdibStorageImpl` correctly updates the list of children when removing an entity with a parent. (#186)
- `org.somda.sdc.glue.consumer.SdcRemoteDevicesConnectorImpl` no longer calls GetMdib before subscribing to reports. (#189)
- `org.somda.sdc.glue.consumer.SdcRemoteDeviceImpl` no longer ignores registering and unregistering watchdog observers. (#192)
- `org.somda.sdc.dpws.soap.wseventing.EventSinkImpl` getStatus, renew and unsubscribe messages are send to the epr of the SubscriptionManager. (#190)
- `org.somda.sdc.glue.consumer.report.helper.ReportWriter` no longer ignores states without descriptors in DescriptionModificationReports. (#193)
- `org.somda.sdc.glue.consumer.report.ReportProcessor` correctly handles implied value when comparing instanceIds. (#194)
- `org.somda.sdc.glue.provider.sco.ScoController` to invoke list callbacks correctly (handle-based and catch-all) 
  which have formerly not been invoked by the controller. (#207)
- `org.somda.sdc.glue.consumer.report.helper.ReportWriter` no longer fails on descriptors without states in description modification report parts with modification type del. (#210)
- Disabled external XML entity processing to prevent XXE attacks. (#218)
- `org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryUtil` correctly compares scope sets using STRCMP0 rules. (#217)

## [1.1.0] - 2020-04-18

### Added

- _Releases_ and _How to get started_ sections to the project readme. (#125)
- Feature from BICEPS to send and receive periodic reports. (#51)
- Jetty server supports http and https connections on the same port. (#107)
- Additional utility method in `org.somda.sdc.dpws.soap.SoapUtil` to create new SoapMessage with reference parameters. (#140)
- Additional utility methods `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil#createAttributedQNameType()` and `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil#getAddressUriString()`.
- WS Addressing constant `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants#QNAME_ACTION` that describes the fully qualified name for the action type.
- `org.somda.sdc.dpws.http.HttpException` in order to transport HTTP status codes on SOAP faults. (#143)
- `org.somda.sdc.dpws.soap.SoapFaultHttpStatusCodeMapping` to map from SOAP faults to HTTP status codes. (#143)
- SOAP constants for VersionMismatch, MustUnderstand and DataEncodingUnknown SOAP Fault codes.
- `org.somda.sdc.dpws.soap.exception.SoapFaultException`: constructor that accepts a throwable cause. (#143)

### Changed

- Report processing on consumer side, which now compares MDIB sequence IDs by using URI compare instead of string compare.
- Extracted namespace prefixes in `biceps` and `glue` package `CommonConstants`.
- Enable generating equals and hashcode for all models. (#140)
- `org.somda.sdc.dpws.soap.TransportInfo` provides a  `List` of certificates instead of a `Collection`. (#147)

### Deprecated

- `org.somda.sdc.glue.provider.SdcDevice#getDiscoveryAccess()` and `#getHostingServiceAccess()`; see `SdcDevice` class comment for alternative access.
- `org.somda.sdc.dpws.CommunicationLogSink.getTargetStream()`; see method comment for alternative.
- `org.somda.sdc.glue.consumer.factory.SdcRemoteDeviceFactory#createSdcRemoteDevice()` without watchdog argument.
- `org.somda.sdc.dpws.soap.MarshallingService#handleRequestResponse()` as this function was only used by tests.
- `org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler#getX509Certificates()` as it is supposed to be an internal function only.
- `org.somda.sdc.dpws.soap.TransportInfo` constructor using a collection. (#147)
- Jetty server supports http and https connections on the same port (#107)
- Additional utility method in `org.somda.sdc.dpws.soap.SoapUtil` to create new SoapMessage with reference parameters (#140)
- `org.somda.sdc.dpws.soap.HttpApplicationInfo(Map<String, String>)` and `org.somda.sdc.dpws.soap.HttpApplicationInfo#getHttpHeaders()`; use Multimap versions instead. (#147)
- `org.somda.sdc.biceps.common.preprocessing.DescriptorChildRemover#removeChildren(MdsDescriptor)` as this was not intended to be public. (#149)

### Fixed

- `org.somda.sdc.dpws.soap.SoapMessage#getEnvelopeWithMappedHeaders()` did not retain additional header set in the original envelope. (#131)
- `SdcRemoteDevicesConnectorImpl` did not register disconnecting providers.
- Services did not shut down in an orderly manner, causing issues when shutting down a consumer. (#134)
- Jetty server could select incorrect adapter when running HTTPS. (#135)
- `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingMapper#mapToJaxbSoapHeader()` could cause duplicate header entries, e.g. Action elements. (#140)
- SOAP engine did not respond with appropriate faults in case the action header was missing or the action was unknown. (#143)
- `SdcRemoteDevicesConnectorImpl` did not register disconnecting providers
- Services did not shut down in an orderly manner, causing issues when shutting down a consumer (#134)
- Jetty server could select incorrect adapter when running https (#135)
- `org.somda.sdc.dpws.soap.wsaddressing.WsAddressingMapper#mapToJaxbSoapHeader()` could cause duplicate header entries, e.g. Action elements (#140)
- Http headers which occurred multiple times would only return the last value. (#146)
- `org.somda.sdc.dpws.client.helper.HostingServiceResolver#resolveHostingService()` could cause deadlock due to 
  chained tasks execution inside the same thread pool. (#225)

## [1.0.1] - 2020-03-11

### Fixed

- `org.somda.sdc.dpws.CommunicationLogImpl.logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext, InputStream message)` did not close OutputStream and was logging trailing empty bytes (#126)
- `org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants.NAMESPACE` contained an extra trailing slash, not matching the actual WS-Discovery 1.1 namespace (#130)
