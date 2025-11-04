# SDCri coding conventions

This document describes coding conventions when writing Java code for the SDCri software beyond code style formatting.

## Naming

- Naming of objects, methods and variables follows the common Java coding conventions of setters/getters, camel case notation etc. (e.g., see this [geeksforgeeks article](https://www.geeksforgeeks.org/java-naming-conventions))
- Any object, method or variable **shall** have speaking names.
- If the scope of a variable spans less than 5 lines, names **may** be narrowed down to 3 characters.
- Loop variables **may** be expressed with 1 character (e.g., the glorious i and j loop counters).

## Immutability

Immutability allows stricter control over data, hence immutable objects **should** be used where applicable. 

## Error handling

- Errors **shall** be expressed using checked exceptions.
- Errors **should not** be expressed by return values.
- The format of exception messages **shall** be in compliance with the format for log messages (see below).

## Logging

### Log levels

- The DEBUG log level **shall** be used generously in order to ease debugging.
- The INFO log level **shall** be used sparingly for important log information that is not repeated at high frequency. INFO **may** also express uncritical failures.
- The WARN log level **shall** be used only in case of a failure that hinders the application from continuing its processing for a specific function call. A WARN is always a sign of malfunction and should be investigated when observed.
- The ERROR log level **shall** be used only in case the application detects a systematic or incurable failure.

### Format

For any line in the log output,

- any variable portion **should** be placed at the end if reasonable,
- the line **should not** comprise more than three sentences, preferably one,
- a sentence stop **shall not** be placed if the line comprises one sentence only,
- sentence stops **shall** be placed if the line comprises more than one sentence. 

### Format of toString() methods

A `toString()` method **should** be formatted in accordance with the following style:

- Simple class name prefix followed by essential attributes in parenthesis
- Each attribute is separated by using a semicolon
- Each attribute key is separated from the attribute value by an equal character

Example: `MdibVersion(sequenceId=<VALUE>;instanceId=<VALUE>;version=<VALUE>)` for the `MdibVersion` class with attributes `instanceId`, `sequenceId` and `version`.

> See `org.somda.sdc.common.util.ObjectStringifier` to generate `toString()` output that is in accordance with the aforementioned rules.

## Dependency injection

SDCri utilizes Google Guice for dependency injection.

- In general, every class that exhibits logic **shall** be injected by using Google Guice.
- Simple data containers or local helper classes **may** be used without dependency injection.
- Assisted parameters **shall** appear before any other parameters.
- Field injection **shall** not be used.

### Class bindings with default modules

For each module there **shall** be a package `guice` that includes a default Guice module and other Guice related classes and annotations.

Example: the dpws module encloses a package named `org.somda.sdc.dpws.guice` with the default module named `DefaultDpwsModule`.

## Module configuration

### Static configuration

The configuration of a module **shall** be facilitated by a specific Guice configuration module located under the `guice` package that includes named bindings for all module related static configuration values (values that are applied once during runtime and never change). Consider the configuration to be loadable from an IO resource on application start.

Example: the dpws module encloses the file `DefaultDpwsConfigModule` derived from `org.somda.sdc.common.guice.AbstractConfigurationModule`, which defines default values for all DPWS related static configuration values. The keys for those values are part of different classes ending with `Config`, e.g., `DpwsConfig`, `SoapConfig`, etc. Exemplary excerpt from `SoapConfig`:

```
/**  
 * Configuration of SOAP package.
 */
public class SoapConfig {    
    /**  
     * Sets the context path for JAXB marshalling and unmarshalling.
     * <p>
     * Internal context path elements will be added automatically.
     * This configuration item uses the same String format as used in
     * {@link JAXBContext#newInstance(String)}.
     * <p><ul>
     * <li>Data type: {@linkplain String}  
     * <li>Use: optional
     * </ul>
     */
    public static final String JAXB_CONTEXT_PATH = "SoapConfig.ContextPaths";  
}
```

### Dynamic configuration

Sometimes it’s helpful to inject dynamic configuration parameters to certain components of a module during runtime (which may either be generated or change during runtime). In SDCri that type of configuration is called *Settings*. Settings are typically interfaces that are supposed to be implemented by a module's user.

Any dynamic configuration class/interface **shall** be appended with `Settings`.

Example: the dpws module defines a dynamic device settings interface called `DeviceSettings` to provide endpoint reference and HTTP server bindings:

```
/**  
 * Settings used in the setup process of a device.
 */
public interface DeviceSettings {  
    /**  
     * The unique and persisted endpoint reference (EPR) of the device.
     * 
     * @return the EPR of the device.
     */
    EndpointReferenceType getEndpointReference();  
  
    /**  
     * Bindings that are used to make the device accessible from network.
     * 
     * @return a list of bindings, e.g., for HTTP and HTTPS bindings. 
     */
    List<URI> getHostingServiceBindings();  
}
```

## Directory/package structure

- Every module *shall* include a package named `guice` (see section _Class bindings with default modules_)
- Factories are stored in each package as a sub-package named `factory`
- Helper classes go into a sub-package of each package named `helper`. Helper classes are not intended to be used publicly.
- Event messages *shall* go into `event` packages (see section _Observer pattern_) 

## Observer pattern

SDCri uses Google Guava's EventBus to enable the observer pattern. The EventBus is a lightweight class that is capable of invoking arbitrary, annotated functions on registered observers. In order to distinguish between different function calls that accept the same payload, every event **shall** be conveyed using a strong-typed wrapper class called *message*, ending with `Message` and stored in a separate sub-package called `event`.

Example:
```
public class DeviceEnteredMessage extends AbstractEventMessage<DiscoveredDevice> {  
    public DeviceEnteredMessage(DiscoveredDevice payload) {  
        super(payload);  
    }  
}
```

### Empty observer interfaces

For every observer there **shall** be an empty interface that describes the accepted messages.

Example:

```
/**  
 * Indicates class as a discovery observer.
 * <p>
 * Annotate method with {@link com.google.common.eventbus.Subscribe} to
 * <p><ul>  
 * <li>{@link DeviceEnteredMessage}  
 * <li>{@link DeviceLeftMessage}  
 * <li>{@link ProbedDeviceFoundMessage}  
 * <li>{@link DeviceProbeTimeoutMessage}
 * </ul>
 */
public interface DiscoveryObserver {  
}
```

## Test code conventions

Rule of thumb: treat your test code as you treat productive code.

### Test classes

By following the naming rules defined in this section, Maven will detect unit and integration tests automatically. Unit tests are executed during the *test* phase whereas integration tests are executed during the *verify* phase.

#### Unit test classes

Unit test class names **shall** end with a *Test* and go to the same package as the tested classes go.

#### Integration test classes

Integration test class names **shall** end with an IT and go to a package that complies with the top-level package named *it* followed by the module package name plus a suitable sub-package tree.

Example: `it.org.somda.sdc.dpws. soap` where `it` is the integration test prefix package, `org.somda.sdc.dpws` is the modules base package and `soap` is a meaningful sub-package name.
 
#### Unit test scaffold

Any test **should** outline the following points:

- Given: the input to the test
- When: the tested condition
- Then: the expected result

To ease reading a test, it is helpful to annotate a test with comments marking the given-when-then sections.

Example:

```
// Given an object Foo
Foo foo = new Foo();

// When foo invokes bar()
try {
    foo.bar();
    // Then expect an exception to be thrown
    fail();
} catch (Exception e) {
}
```
