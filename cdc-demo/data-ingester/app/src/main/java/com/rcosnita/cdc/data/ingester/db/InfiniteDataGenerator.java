package com.rcosnita.cdc.data.ingester.db;

import com.rcosnita.cdc.data.ingester.db.models.Person;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

/**
 * Provides the implementation of an infinite stream of persons.
 */
public class InfiniteDataGenerator implements DataGenerator {
    /**
     * Provides the business logic for generating arbitrary persons content.
     */
    private static class PersonsIterator implements Iterator<Person> {
        PersonsIterator(long contentSizeBytes) {
            this.contentSizeBytes = contentSizeBytes;
            this.random = new Random();
            this.random.setSeed(Calendar.getInstance().getTimeInMillis());
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Person next() {
            var newId = UUID.randomUUID();
            var content = generatedArbitraryContent();
            var creationTime = Calendar.getInstance().toInstant();

            return new Person(newId, content, creationTime);
        }

        private ByteBuffer generatedArbitraryContent() {
            var remainingSize = this.contentSizeBytes;
            StringBuilder content = new StringBuilder();

            while (remainingSize > 0) {
                int c = this.random.nextInt();
                remainingSize -= 4;
                content.append(c);
            }

            return ByteBuffer.wrap(content.toString().getBytes(StandardCharsets.UTF_8));
        }

        private final long contentSizeBytes;
        private final Random random;
    }

    public InfiniteDataGenerator(long contentSizeBytes) {
        this.contentSizeBytes = contentSizeBytes;
    }

    @Override
    public Iterator<Person> iterator() {
        return new PersonsIterator(contentSizeBytes);
    }

    private final long contentSizeBytes;
}
