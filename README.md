<img align="right" width="250" height="47" src="media/Gematik_Logo_Flag.png" alt="gematik GmbH Logo"/> <br/> 

# DEMIS FHIR Storage Service (FSS)

[![Quality Gate Status](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Afhir-storage&metric=alert_status&token=sqb_805bc73ae9665d4c07078315f21febb24c807aea)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Afhir-storage)
[![Vulnerabilities](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Afhir-storage&metric=vulnerabilities&token=sqb_805bc73ae9665d4c07078315f21febb24c807aea)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Afhir-storage)
[![Bugs](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Afhir-storage&metric=bugs&token=sqb_805bc73ae9665d4c07078315f21febb24c807aea)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Afhir-storage)
[![Code Smells](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Afhir-storage&metric=code_smells&token=sqb_805bc73ae9665d4c07078315f21febb24c807aea)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Afhir-storage)
[![Lines of Code](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Afhir-storage&metric=ncloc&token=sqb_805bc73ae9665d4c07078315f21febb24c807aea)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Afhir-storage)
[![Coverage](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Afhir-storage&metric=coverage&token=sqb_805bc73ae9665d4c07078315f21febb24c807aea)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Afhir-storage)

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#release-notes">Release Notes</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#security-policy">Security Policy</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>

## About The Project

This project is part of the DEMIS project and is the successor of the Notification-Clearing-API Service, as part of the
modernisation of the DEMIS project itself. It consists of 3 services:

1. [fhir-storage-writer](reader)

   This service is responsible for retrieving and writing FHIR Bundle Resources in a Database.
2. [fhir-storage-reader](writer)

   This one is responsible for retrieving them.
3. [fhir-storage-purger](purger)

   This service deletes old FHIR resources from the database.

In the [`common`](common) module are defined the common classes and interfaces used by both services.

### Release Notes

See [ReleaseNotes.md](./ReleaseNotes.md) for all information regarding the (newest) releases.

## Getting Started

### Prerequisites

The Project requires Java 21 and Maven 3.8+.

### Installation

The Project can be built with the following command:

```sh
mvn clean install
```

The Docker Image associated to the service can be built with the extra profile `docker`:

```sh
mvn clean install -Pdocker
```

## Usage

The applications can be executed from a JAR file or a Docker Image (example for the reader):

```sh
# As JAR Application
cd fhir-storage-reader
java -jar target/fhir-storage-reader.jar
# As Docker Image
docker run --rm -it -p 8080:8080 fhir-storage-reader:latest
```

It can also be deployed on Kubernetes by using the Helm Chart defined in the
folder `deployment/helm/fhir-storage-reader`:

```ssh
helm upgrade --install fhir-storage-reader ./deployment/helm/fhir-storage-reader
```

### Endpoints

The two services provide the following endpoints:

#### [fhir-storage-reader](reader)

| Endpoint          | Method | Input                                      | Output                                                              |
|-------------------|--------|--------------------------------------------|---------------------------------------------------------------------|
| /fhir/Binary      | GET    | request parameters defining the search     | A list of binary FHIR resources matching the given search parameter |
| /fhir/Binary/{id} | GET    | ID of a binary resource as query parameter | The binary resource with the given ID                               |
| /fhir/Bundle      | GET    | request parameters defining the search     | A list of FHIR resource bundles matching the given search parameter |
| /fhir/Bundle/{id} | GET    | ID of a bundle as query parameter          | The bundle with the given ID                                        |

The search parameters are designed to work like the [HL7 FHIR search parameters](http://hl7.org/fhir/search.html)
Currently possible search parameters are:

| request parameter | description                                                                                                                                                        |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| _lastUpdated      | find resources by their [lastUpdated](http://hl7.org/fhir/search.html#_lastUpdated) field. The search value is a [FHIR date](http://hl7.org/fhir/search.html#date) |
| _tag              | find resources by their [tags](http://hl7.org/fhir/search.html#_tag)                                                                                               |
| _source           | find resources based by their [source](http://hl7.org/fhir/search.html#_source) information                                                                        |
| _profile          | direct filter for the profile                                                                                                                                      |
| _sort             | define a field for which the result set is sorted, defaults to ascending                                                                                           |
| _sort:asc         | like _sort                                                                                                                                                         |
| _sort:desc        | like _sort but with descending sort order                                                                                                                          |
| _count            | number of resources returned on the result page: [limiting page size](http://hl7.org/fhir/search.html#_count)                                                      |
| _offset           | skip these number of results before returning the results                                                                                                          |
| _format           | specify the mime-type in which the result shall be returned [_format](http://hl7.org/fhir/http.html#parameters)                                                    |

#### [fhir-storage-writer](writer)

| Endpoint | Input                           | Output                              |
|----------|---------------------------------|-------------------------------------|
| /fhir    | FHIR bundle or resource as json | A list of created FHIR resource IDs |

#### [fhir-storage-purger](purger)

TODO

## Security Policy
If you want to see the security policy, please check our [SECURITY.md](.github/SECURITY.md).

## Contributing
If you want to contribute, please check our [CONTRIBUTING.md](.github/CONTRIBUTING.md).

## License
EUROPEAN UNION PUBLIC LICENCE v. 1.2

EUPL © the European Union 2007, 2016

Copyright (c) 2023 gematik GmbH

See [LICENSE](LICENSE.md).

## Contact
E-Mail to [DEMIS Entwicklung](mailto:demis-entwicklung@gematik.de?subject=[GitHub]%20FHIR-Storage-Service)