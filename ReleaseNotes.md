<img align="right" width="250" height="47" src="media/Gematik_Logo_Flag.png" alt="gematik GmbH Logo"/> <br/> 
 
# Release notes
## Release 1.4.0 
- updated base-image and updated from java 21 to java 25
- Removed istio helm chart
- decreased MaxRAMPercentage from 80% to 65%

## Release 1.3.2
- updated spring-parent to 2.14.20
- removed FEATURE_FLAG_NEW_API_ENDPOINTS
- removed all legacy NCAPI references
- updated base-image

## Release 1.3.1
- purger: add configmap checksum as annotation to force pod restart on configmap change
- updated dependencies
- error id in operation-outcome moved from location to diagnostics (FEATURE_FLAG_MOVE_ERROR_ID_TO_DIAGNOSTICS)
- fixed purger pod annotations in helmchart

## Release 1.3.0
- Updated base image to gematik1/osadl-alpine-openjdk21-jre:1.0.3
- add support for new API Endpoints
 
## Release 1.2.2
- Updated base image to gematik1/osadl-alpine-openjdk21-jre:1.0.2
- Updated dependencies including HAPI FHIR 8.2.0
- Updated Roles for supporting §7.3/§7.4 Notifications in Storage Reader

## Release 1.2.1
- Optimized purger SQL statements
- Liquibase has to be explicitly enabled on deployment

## Release 1.2.0
- Removed feature: sync to HAPI FHIR server
- Updated ospo-resources for adding additional notes and disclaimer
- setting new resources in helm chart
- setting new timeouts and retries in helm chart
- change base chart to istio hostnames
- updating dependencies

## Release 1.1.0
- First official GitHub-Release
- Update Base-Image to OSADL
- Dependency-Updates (CVEs et al.)

## Release 1.0.0
### added
- Initial release