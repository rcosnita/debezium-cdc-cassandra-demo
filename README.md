# Getting started

```bash
docker-compose up
```

## Create required kafka topics

```bash
docker-compose exec kafka2 bash
kafka-topics --bootstrap-server 192.168.68.52:9092 --create --topic my_cluster.cdc_experiment.persons --replication-factor 1 --partitions 10
kafka-topics --list --bootstrap-server 192.168.68.52:9092
kafka-console-consumer --topic my_cluster.cdc_experiment.persons --bootstrap-server 192.168.68.52:9092
```

## Create the cassandra required keyspace

```bash
docker-compose exec cassandra-node1-cdc1 bash
cqlsh

CREATE KEYSPACE IF NOT EXISTS cdc_experiment
    WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': 1 }
    AND DURABLE_WRITES = 'true';

DROP TABLE IF EXISTS cdc_experiment.persons;

CREATE TABLE IF NOT EXISTS cdc_experiment.persons(
    id UUID,
    content BLOB,
    creation_time TIMESTAMP,
    PRIMARY KEY (id)
) WITH cdc=true;

truncate table cdc_experiment.persons;
SELECT COUNT(1) FROM cdc_experiment.persons;

exit
watch du -h /var/lib/cassandra/cdc_raw/
```

## Run debezium connector.

Follow the steps from [Debezium connector config-only](https://groups.google.com/g/debezium/c/uvN2WzR15qE).

```bash
docker-compose exec cassandra-node1-cdc1 bash
java -Djdk.attach.allowAttachSelf=true \
  --add-exports java.base/jdk.internal.misc=ALL-UNNAMED \
  --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
  --add-exports java.base/sun.nio.ch=ALL-UNNAMED \
  --add-exports java.management.rmi/com.sun.jmx.remote.internal.rmi=ALL-UNNAMED \
  --add-exports java.rmi/sun.rmi.registry=ALL-UNNAMED \
  --add-exports java.rmi/sun.rmi.server=ALL-UNNAMED \
  --add-exports java.sql/java.sql=ALL-UNNAMED \
  --add-opens java.base/java.lang.module=ALL-UNNAMED \
  --add-opens java.base/jdk.internal.loader=ALL-UNNAMED \
  --add-opens java.base/jdk.internal.ref=ALL-UNNAMED \
  --add-opens java.base/jdk.internal.reflect=ALL-UNNAMED \
  --add-opens java.base/jdk.internal.math=ALL-UNNAMED \
  --add-opens java.base/jdk.internal.module=ALL-UNNAMED \
  --add-opens java.base/jdk.internal.util.jar=ALL-UNNAMED \
  --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED \
  -jar ./debezium-connector-cassandra-4-2.5.0.Final-jar-with-dependencies.jar /etc/debezium/cdc.properties > debezium.stdout.log 2> debezium.stderr.log
```