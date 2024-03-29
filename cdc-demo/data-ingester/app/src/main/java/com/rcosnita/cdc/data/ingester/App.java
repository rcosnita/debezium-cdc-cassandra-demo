/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.rcosnita.cdc.data.ingester;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint;
import com.rcosnita.cdc.data.ingester.db.InfiniteDataGenerator;
import com.rcosnita.cdc.data.ingester.db.models.Person;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class App {
    private static final long PERSON_CONTENT_SIZE_BYTES = 4096;
    private static final int MAX_NUM_PERSONS = Integer.parseInt(System.getenv().getOrDefault("MAX_NUM_PERSONS", "5000"));
    private static final ExecutorService EXECUTOR_SVC = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws Exception {
        final var iterator = new InfiniteDataGenerator(PERSON_CONTENT_SIZE_BYTES).iterator();
        long numOfPersons = MAX_NUM_PERSONS;
        List<Future<Person>> pendingQueries = new ArrayList<>();

        try (var session = CqlSession.builder()
                .addContactEndPoint(new DefaultEndPoint(InetSocketAddress.createUnresolved("cassandra-node1-cdc1", 9042)))
                .withKeyspace("cdc_experiment")
                .withLocalDatacenter("dc2")
                .build()) {
            while (iterator.hasNext() && --numOfPersons >= 0) {
                pendingQueries.add(EXECUTOR_SVC.submit(() -> {
                    var p = iterator.next();
                    var stmt = session.prepare("INSERT INTO persons(id, content, creation_time) VALUES(?,?,?)");
                    session.execute(stmt.bind(p.getId(), p.getContent(), p.getCreatedTime()));
                    return p;
                }));
            }

            pendingQueries.forEach((f) -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }

        EXECUTOR_SVC.shutdown();
    }
}
