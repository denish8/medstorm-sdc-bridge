# SDC Reference Implementation

## Summary

SDCri is a set of Java libraries that implements a network communication framework conforming with the IEEE 11073 SDC specifications

- IEEE 11073-20702: Point-of-care medical device communication Part 20702: Medical Devices Communication Profile for Web Services
- IEEE 11073-10207: Point-of-care medical device communication Part 10207: Domain Information and Service Model for Service-Oriented Point-of-Care Medical Device Communication
- IEEE 11073-20701: Point-of-care medical device communication Part 20701: Service-Oriented Medical Device Exchange Architecture and Protocol Binding

The SDC standard meets increasing demand for medical device interoperability in clinical environments like operating theatres or intensive care units.

The long-term goal of SDCri is to deliver a reference implementation in order to facilitate conformance testing against other SDC implementations.
It is not intended to be used in clinical trials, clinical studies, or in clinical routine.

## Releases

You find a list of all releases [here](https://gitlab.com/sdc-suite/sdc-ri/-/releases).

## How to get started

The easiest way to get a copy of SDCri is to visit Maven Central at https://mvnrepository.com/artifact/org.somda.sdc/sdc-ri.
There you can seek the latest version of the library and either download the JAR files or - preferably - include it as a dependency in your project.

Watch out for the [Glue Examples](https://mvnrepository.com/artifact/org.somda.sdc/glue-examples) sub-module in order to learn setting up providers and consumers.
Alternatively, you can directly navigate to the code examples within this Git repository:

- Starting point for SDC Service Provider: [com.example.provider1](glue-examples%2Fsrc%2Fmain%2Fjava%2Fcom%2Fexample%2Fprovider1)
- Starting point for SDC Service Consumer: [com.example.consumer1](glue-examples%2Fsrc%2Fmain%2Fjava%2Fcom%2Fexample%2Fconsumer1)

## License

SDCri is licensed under the MIT license, see [LICENSE](LICENSE) file.

## Changelog

The changelog is available [here](CHANGELOG.md).

## Contact

Contact information can be found in the `pom.xml`.