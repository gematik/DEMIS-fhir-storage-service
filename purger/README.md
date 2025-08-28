
# FHIR-Storage-Purger

THe FHIR-Storage-Purger deletes records from FHIR-Storage.
It's a K8s CronJob that runs periodically and deletes records based on a set of rules.

<!-- TOC -->
* [FHIR-Storage-Purger](#fhir-storage-purger)
  * [Configuration](#configuration)
    * [Selective deletion periods](#selective-deletion-periods)
      * [Rules](#rules)
  * [Main process](#main-process)
    * [BINARY](#binary)
    * [BUNDLE](#bundle)
      * [Choosing records to delete](#choosing-records-to-delete)
  * [Scaling](#scaling)
    * [Horizontal Scaling](#horizontal-scaling)
    * [Vertical Scaling](#vertical-scaling)
<!-- TOC -->

### Selective deletion periods

Configuration parameters to define specific periods in which records should be deleted.
This will be a K8s ConfigMap provided as YAML.

The config for features:
- `default`: The default period for records that do not match any other rule.
- `bundle-profiles`: A list of rules that define a period for records that match a specific bundle profile.
- `responsible-departments`: A list of rules that define a period for records that have a specific responsible department.

```yaml
periods:
  default-period: 30d
  bundle-profiles:
    - uri: https://demis.rki.de/fhir/StructureDefinition/NotificationBundleDisease
      period: 20d
    - uri: https://demis.rki.de/fhir/StructureDefinition/NotificationBundlePathogen
      period: 20d
  responsible-departments:
    - department: 1.01.0.53.
      period: 60d
```

This is our vision for further features:
- `composition-profiles`: A list of rules that define a period for records that match a specific composition profile.
- `tags`: A list of rules that define a period for records that have a specific tag.

```yaml
periods:
  default-period: 30d
  bundle-profiles:
    - uri: https://demis.rki.de/fhir/StructureDefinition/NotificationBundleDisease
      resource-types:
        - Bundle
        - Binary
      period: 20d
  composition-profiles:
    - uri: https://demis.rki.de/fhir/StructureDefinition/NotificationDiseaseCVDD
      period: 60d
  responsible-departments:
    - department: 1.01.0.53.
      period: 60d
  tags:
    - system: https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartmentNotifier
      code: 1.01.0.53.
      period: 60d
```

#### Rules
This is how we apply the configured deletion periods:
1. If responsible-department and bundle-profile exist, choose the greater value as period.
2. If responsible-department or bundle-profile exists, choose that period.
3. If no responsible-department or bundle-profile exists, choose default-period as period.

## Main process

- Every resource type has a specific purging implementation
- Batch-based purging is used for all resource types
- All purge implementations are executed in parallel, currently: `binaries`, `bundles`

## Scaling

The purger is designed to be run in a Kubernetes cluster and can be scaled horizontally and vertically to increase the deletion speed.

### Horizontal Scaling

When multiple instances of the purger are running, they will synchronize with each other through a database lock and process different batches of records.

### Vertical Scaling

The purger uses a thread pool to parallelize the deletion of records. The size of the thread pool is dynamically adjusted based on the number of CPU cores available.
