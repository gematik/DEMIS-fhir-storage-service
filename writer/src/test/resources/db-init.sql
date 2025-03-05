CREATE SCHEMA demis_fhir;

CREATE USER "fhir-storage-writer-ddl" WITH PASSWORD 'fhir-storage-writer-ddl';
REVOKE ALL PRIVILEGES ON SCHEMA PUBLIC FROM "fhir-storage-writer-ddl";
GRANT CONNECT ON DATABASE "fhir-storage" TO "fhir-storage-writer-ddl";
ALTER ROLE "fhir-storage-writer-ddl" SET search_path TO demis_fhir;
GRANT ALL PRIVILEGES ON SCHEMA demis_fhir TO "fhir-storage-writer-ddl";

CREATE USER "fhir-storage-reader" WITH PASSWORD 'fhir-storage-reader';
REVOKE ALL PRIVILEGES ON SCHEMA PUBLIC FROM "fhir-storage-reader";
GRANT CONNECT ON DATABASE "fhir-storage" TO "fhir-storage-reader";
GRANT USAGE ON SCHEMA demis_fhir TO "fhir-storage-reader";
ALTER ROLE "fhir-storage-reader" SET search_path TO demis_fhir;

CREATE USER "fhir-storage-writer" WITH PASSWORD 'fhir-storage-writer';
REVOKE ALL PRIVILEGES ON SCHEMA PUBLIC FROM "fhir-storage-writer";
GRANT CONNECT ON DATABASE "fhir-storage" TO "fhir-storage-writer";
GRANT USAGE ON SCHEMA demis_fhir TO "fhir-storage-writer";
ALTER ROLE "fhir-storage-writer" SET search_path TO demis_fhir;

CREATE USER "fhir-storage-purger" WITH PASSWORD 'fhir-storage-purger';
REVOKE ALL PRIVILEGES ON SCHEMA PUBLIC FROM "fhir-storage-purger";
GRANT CONNECT ON DATABASE "fhir-storage" TO "fhir-storage-purger";
GRANT USAGE ON SCHEMA demis_fhir TO "fhir-storage-purger";
ALTER ROLE "fhir-storage-purger" SET search_path TO demis_fhir;
