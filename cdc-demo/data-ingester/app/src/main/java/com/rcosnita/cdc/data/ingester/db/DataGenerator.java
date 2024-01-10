package com.rcosnita.cdc.data.ingester.db;

import com.rcosnita.cdc.data.ingester.db.models.Person;

import java.util.Iterator;

/**
 * Provides the contract used for running the data generator.
 */
public interface DataGenerator extends Iterable<Person> {
    /**
     * Provides the contract for generating persons.
     */
    @Override
    Iterator<Person> iterator();
}
