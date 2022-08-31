package persistence;

import model.Data;
import model.Symbol;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class DBWriter {

    public static void writeSymbol(SessionFactory sessionFactory,Symbol symbol) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        session.save(symbol);
        transaction.commit();
        session.close();
    }

    public static void writeData(SessionFactory sessionFactory, Data d) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        DbReader dbReader = new DbReader(sessionFactory);
        List<Symbol> symbolList = dbReader.getSymbolObjFromDb(d.getSymbol().getSymbolName());
        if(symbolList.size() == 1){
            Symbol persistentSymbol = session.get(Symbol.class,symbolList.get(0).getId());
            d.setSymbol(persistentSymbol);
        }
        session.save(d);
        transaction.commit();
        session.close();
    }

    public static void writeDatainBatch(SessionFactory sessionFactory, List<Data> data) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        DbReader dbReader = new DbReader(sessionFactory);
        List<Symbol> symbolList = dbReader.getSymbolObjFromDb(data.get(0).getSymbol().getSymbolName());

        long index = dbReader.getLatestIndex();
        index++;
        Symbol persistentSymbol = session.get(Symbol.class,symbolList.get(0).getId());

        for (Data d : data) {
            d.setSymbol(persistentSymbol);
            d.setId(index);
            session.save(d);
            index++;
        }

        transaction.commit();
        session.close();

        Logger logger = LoggerFactory.getLogger(DBWriter.class);
        logger.info("Saved to database: " + data.size() + " records for: " + data.get(0).getSymbol().getSymbolName() + " ticker");
    }
}
