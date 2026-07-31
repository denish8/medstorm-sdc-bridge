# sdc-core

An SDC (IEEE 11073-20701 / DPWS) **provider runtime** built on
[SDC-ri](https://gitlab.com/sdc-suite/sdc-ri) 6.0.0.

SDC-ri is a capable and correct reference implementation. Getting a provider
that a real consumer — a Philips or Dräger monitor — will actually discover,
subscribe to and read from is a different problem, and it is where teams lose
months. This module is that problem, already solved.

**No Spring. No device-specific logic.** The build fails if either creeps in.

---

## What it actually fixes

Each of these was a discrete failure that had to be diagnosed against live
consumer software. They are the reason this module exists.

| Problem | What goes wrong without it | Where |
|---|---|---|
| **NIC selection** | SDC-ri reads the network interface from system properties at static-init time, under ~30 differently-spelled keys across versions. Miss one and WS-Discovery silently binds the wrong adapter — or none. | `DpwsBootstrap`, `MedstormNicResolver` |
| **HTTP port `:0`** | SDC-ri internally requests `http://<ip>:0`. Jetty binds an ephemeral port, the EPR advertises `:0`, and every consumer fails to connect. | `MedstormHttpServerRegistryOverride` |
| **Jetty version** | SDC-ri 6.0.0's DPWS layer does not work on Jetty 12. Any transitive upgrade breaks the stack at runtime, not at compile time. | pinned in `build.gradle` |
| **Null CommunicationLog** | The Jetty handler factory can be handed a null log/context and NPEs during request handling. | `MedstormJettyFactoryOverride` |
| **JAXB context path** | The SOAP marshaller needs an exact colon-separated package list. Wrong or incomplete, and marshalling fails at first message. | `MedstormSdcriConfigModule` |
| **Consumer graph** | Instantiating `DefaultGlueModule` pulls in the consumer side, whose bindings collide in a provider-only deployment. | `MedstormProviderOnlyGlueModule` |
| **WS-Eventing `callerId`** | A real consumer can subscribe without a caller identity; the stock subscription manager does not tolerate null. | `MedstormEventingPatchModule`, `PatchedSourceSubscriptionManagerImpl` |
| **JAXB 2 ↔ 4 split** | SDC-ri's generated model references `org.jvnet.jaxb.*` types that only exist under `org.jvnet.jaxb2_commons.*`. Class-load failure on a Jakarta classpath. | `sdc-jaxb-compat` |

Validated against the Philips consumer simulator and the Dräger simulator:
WS-Discovery Hello, WS-Transfer Get, Subscribe, GetMdib, GetStatus heartbeat,
Unsubscribe and re-subscribe.

## Relationship to SDC-ri

`sdc-core` is a **compatibility and composition layer on top of unmodified,
upstream SDC-ri 6.0.0**, resolved from Maven Central. SDC-ri is not forked and
not vendored; it is a normal dependency:

```
org.somda.sdc:common:6.0.0
org.somda.sdc:dpws:6.0.0
org.somda.sdc:biceps:6.0.0
org.somda.sdc:glue:6.0.0
```

SDC-ri is MIT licensed. Upgrading it is a version bump plus a re-test of the
patches in this module — not a merge.

One deliberate exception: `PatchedSourceSubscriptionManagerImpl` lives in the
`org.somda.sdc.dpws.soap.wseventing` package because it must reach a
package-private constructor. It is a patch, not a feature, and it is the one
thing that ties this module to a specific SDC-ri version.

## Build

```bash
./gradlew :sdc-core:build
```

`verifyNoSpring` runs as part of `check` and fails the build if any
`org.springframework.*` artifact reaches the runtime classpath.

## Status

Extracted and building standalone; the public API is still the Guice injector.
A fluent `SdcProvider.builder()` that hides Guice, Jetty and system properties
entirely is the next step — see the repository issue tracker.

Consumers should treat everything outside the (forthcoming) `api` package as
internal and subject to change until 1.0.

## Licence

Third-party: SDC-ri (MIT), Guice (Apache-2.0), Jetty (Apache-2.0 / EPL-2.0),
JAXB (EDL-1.0). See the generated dependency report for the full set.
