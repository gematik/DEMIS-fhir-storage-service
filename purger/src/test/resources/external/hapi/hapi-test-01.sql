INSERT INTO bundles (id, last_updated, responsible_department, tags, source_id, content, profile,
                     notification_bundle_id)
VALUES ('00000000-0000-0000-0000-000000000001',
        CURRENT_TIMESTAMP - INTERVAL '4 days',
        '1.01.0.53.',
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
       ('00000000-0000-0000-0000-000000000002',
        CURRENT_TIMESTAMP - INTERVAL '3 days',
        '1.20.4.',
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
        E'\\x1f8b08000000000000ffab562a4a2dce2f2d4a4e0da92c4855b252722acd4bc94955d2512a81f053f2934b7353f34a946ab9005874be072c000000',
        'https://demis.rki.de/fhir/StructureDefinition/NotificationBundleLaboratory',
        '1111'),
       ('00000000-0000-0000-0000-000000000003',
        CURRENT_TIMESTAMP - INTERVAL '2 days',
        '1.01.0.53.',
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
INSERT INTO hapi_bundles (bundle_id, status, response_code, hapi_id, error, purger_id, purger_started, purger_attempt,
                          dtype)
VALUES
    -- 1: deletion blocked by active bundle
    ('00000000-0000-0000-0000-000000000001',
     1,
     201,
     '10001',
     null,
     null,
     null,
     null,
     'purger_hapi_bundle'),
    -- 2: deletion blocked by active bundle
    ('00000000-0000-0000-0000-000000000002',
     1,
     201,
     '10002',
     null,
     null,
     null,
     null,
     'purger_hapi_bundle'),
    -- 3: deletion blocked by active bundle
    ('00000000-0000-0000-0000-000000000003',
     0,
     201,
     null,
     null,
     null,
     null,
     null,
     'purger_hapi_bundle'),
    -- 4: deletion blocked by active purger-ID
    ('00000000-0000-0000-0000-000000000004',
     1,
     201,
     '10004',
     null,
     'purger_id_blocks_deletion',
     null,
     null,
     'purger_hapi_bundle'),
    -- 5: deletion blocked by attempt limit
    ('00000000-0000-0000-0000-000000000005',
     1,
     201,
     '10004',
     null,
     null,
     null,
     3,
     'purger_hapi_bundle'),
    -- 6: deletion blocked by bad response of HAPI FHIR server at runtime triggers FATAL logging
    ('00000000-0000-0000-0000-000000000006',
     1,
     201,
     '10006',
     null,
     null,
     CURRENT_TIMESTAMP - INTERVAL '1 day',
     2,
     'purger_hapi_bundle'),
    -- 7: deleted as outdated bundle
    ('00000000-0000-0000-0000-000000000007',
     0,
     null,
     '10007',
     null,
     null,
     null,
     null,
     'purger_hapi_bundle')
;