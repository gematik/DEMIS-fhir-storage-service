INSERT INTO bundles (id, last_updated, responsible_department, tags, source_id, content, profile, notification_bundle_id)
VALUES ('00000000-0000-0000-0000-000000000001', '2025-01-17 14:59:27.111000 +01:00', '1.01.0.53.',
        '[
          {
            "code": "1.01.0.53.",
            "system": "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment"
          },
          {
            "code": "1.11.0.11.01.",
            "system": "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartmentNotifier"
          },
          {
            "code": "1a3a16aa-64e0-5eb1-8601-018fc3794b6e",
            "system": "https://demis.rki.de/fhir/CodeSystem/RelatedNotificationBundle",
            "display": "Relates to message with identifier: f47ac10b-58cc-4372-a567-0e02b2c3d479e"
          }
        ]',
        '061f30ab559170b6c4db82ca25ef6daa',
        E'\\x1f8b08000000000000ffab562a4a2dce2f2d4a4e0da92c4855b252722acd4bc94955d2512a81f053f2934b7353f34a946ab9005874be072c000000',
        'https://demis.rki.de/fhir/StructureDefinition/NotificationBundleLaboratory',
        null),
       ('00000000-0000-0000-0000-000000000002', '2025-01-17 15:02:27.222000 +01:00', '1.20.4.',
        '[
          {
            "code": "1.20.4.",
            "system": "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment"
          },
          {
            "code": "1.11.0.11.01.",
            "system": "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartmentNotifier"
          },
          {
            "code": "aaaaaaaa-0000-0000-0000-000000000001",
            "system": "https://demis.rki.de/fhir/CodeSystem/RelatedNotificationBundle",
            "display": "Relates to message with identifier: aaaaaaaa-0000-0000-0000-000000000001"
          }
        ]',
        '061f30ab559170b6c4db82ca25ef6daa',
        E'\\x1f8b08000000000000ffab562a4a2dce2f2d4a4e0da92c4855b252722acd4bc94955d2512a81f053f2934b7353f34a946ab9005874be072c000000'
           ,
        'https://demis.rki.de/fhir/StructureDefinition/NotificationBundleLaboratory',
        '1111'),
       ('00000000-0000-0000-0000-000000000003', '2025-01-17 14:55:16.333000 +01:00', '1.01.0.53.',
        '[
          {
            "code": "1.01.0.53.",
            "system": "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment"
          },
          {
            "code": "1.11.0.11.01.",
            "system": "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartmentNotifier"
          },
          {
            "code": "1a3a16aa-64e0-5eb1-8601-018fc3794b6e",
            "system": "https://demis.rki.de/fhir/CodeSystem/RelatedNotificationBundle",
            "display": "Relates to message with identifier: f47ac10b-58cc-4372-a567-0e02b2c3d479e"
          }
        ]',
        '061f30ab559170b6c4db82ca25ef6daa',
        E'\\x1f8b08000000000000ffab562a4a2dce2f2d4a4e0da92c4855b252722acd4bc94955d2512a81f053f2934b7353f34a946ab9005874be072c000000',
        'https://demis.rki.de/fhir/StructureDefinition/NotificationBundleDisease',
        '9999')
;

INSERT INTO binaries (id, last_updated, responsible_department, tags, source_id, content_type, data)
VALUES ('00000000-0000-0000-0001-000000000001',
        '2025-01-12 12:19:29.114000 +00:00',
        '1.01.0.53.',
        '[
          {
            "code": "1.01.0.53.",
            "system": "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment",
            "display": null
          },
          {
            "code": "2.22.0.22.02.",
            "system": "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartmentNotifier",
            "display": null
          }
        ]',
        '061f30ab559170b6c4db82ca25ef6daa',
        'application/cms',
        E'\\x1F8B08000000000000FF3B2E53745AE7F3F9D8ED0039DE624109000000'),
       ('00000000-0000-0000-0001-000000000002',
        '2025-01-10 10:19:29.114000 +00:00',
        '1.01.0.53.',
        '[
          {
            "code": "1.01.0.53.",
            "system": "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment",
            "display": null
          },
          {
            "code": "1.11.0.11.01.",
            "system": "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartmentNotifier",
            "display": null
          }
        ]',
        '061f30ab559170b6c4db82ca25ef6daa',
        'application/cms',
        E'\\x1F8B08000000000000FF3B2E53745AE7F3F9D8ED0039DE624109000000'),
       ('00000000-0000-0000-0001-000000000003',
        '2024-01-02 13:19:29.114000 +00:00',
        '2.01.5.99.',
        '[
          {
            "code": "2.01.5.99",
            "system": "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment",
            "display": null
          },
          {
            "code": "1.11.0.11.01.",
            "system": "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartmentNotifier",
            "display": null
          }
        ]',
        '061f30ab559170b6c4db82ca25ef6daa',
        'application/cms',
        E'\\x1F8B08000000000000FF3B2E53745AE7F3F9D8ED0039DE624109000000')
;
