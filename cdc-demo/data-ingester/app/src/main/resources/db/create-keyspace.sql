CREATE KEYSPACE IF NOT EXISTS cdc_experiment
    WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': 1 }
    AND DURABLE_WRITES = 'true';

DROP TABLE IF EXISTS persons;

CREATE TABLE IF NOT EXISTS cdc_experiment.persons(
  id UUID,
  content BLOB,
  creation_time TIMESTAMP,
  PRIMARY KEY (id)
) WITH cdc=true;

truncate table cdc_experiment.persons;
SELECT COUNT(1) FROM cdc_experiment.persons;