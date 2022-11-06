package persistence;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaInitializer {

    private final Session session;
    private final Logger logger;

    public SchemaInitializer(Session session) {
        this.session = session;
        this.logger = LoggerFactory.getLogger(SchemaInitializer.class);
    }

    public void initializeSchemasOrDoNothing() {


        final var tables = session.createSQLQuery("Show tables")
                .getResultList();

        if (tables.isEmpty()) {
            logger.debug("No tables in database, need to create one");
            Transaction transaction = session.beginTransaction();
            String binanceData = "create table binance_data(id bigint ,symbol_id int,open_time datetime(6),open double,high double,low double,close double,volume double,key(id));";
            session.createSQLQuery(binanceData).executeUpdate();
            String symbol = "create table symbols(id bigint AUTO_INCREMENT,symbol char(15),key(id));";
            session.createSQLQuery(symbol).executeUpdate();
            transaction.commit();
            logger.debug("tables created");

        }
    }
}
