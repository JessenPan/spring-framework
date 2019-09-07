package org.springframework.jdbc.datasource.embedded;

import org.junit.Test;
import org.springframework.jdbc.datasource.init.DatabasePopulator;

import java.sql.Connection;

import static org.junit.Assert.assertTrue;

public class EmbeddedDatabaseFactoryTests {

    private EmbeddedDatabaseFactory factory = new EmbeddedDatabaseFactory();

    @Test
    public void testGetDataSource() {
        StubDatabasePopulator populator = new StubDatabasePopulator();
        factory.setDatabasePopulator(populator);
        EmbeddedDatabase db = factory.getDatabase();
        assertTrue(populator.populateCalled);
        db.shutdown();
    }

    private static class StubDatabasePopulator implements DatabasePopulator {

        private boolean populateCalled;

        @Override
        public void populate(Connection connection) {
            this.populateCalled = true;
        }

    }
}
