import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.client.impl.spot.Market;
import downloads.BinanceDownloader;
import model.Data;
import model.Symbol;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import persistence.DBWriter;
import persistence.DbReader;
import persistence.MySQLUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BinanceRunner {

    final private BinanceDownloader binance;
    final private SessionFactory sessionFactory;
    private final Logger logger;

    public BinanceRunner() {
        binance = configureDownloader();
        MySQLUtil mySQLUtil = new MySQLUtil();
        sessionFactory = mySQLUtil.getSessionFactory();
        logger = LoggerFactory.getLogger(BinanceRunner.class);
    }

    public void run() {

//        List<String> filteredSymbolList = getListOfSymbolsUSDT(binance, "USDT");
        List<String> filteredSymbolList = List.of("SOLUSDT");
        logger.info("downloaded tickers: " + filteredSymbolList.size());

        //pobierz dane odnośnie symbolów z bazy danych i pobierz ostatni czas z bazy danych
        DbReader dbReader = new DbReader(sessionFactory);
        List<Symbol> symbolObj = dbReader.getSymbolObjListFromDb();

        HashMap<String, LocalDateTime> symbolTimeFromDb = new HashMap<>();

        for (Symbol symbol : symbolObj) {
            LocalDateTime lastDate = dbReader.readLastDate(symbol);
            symbolTimeFromDb.put(symbol.getSymbolName(), lastDate);
        }

        logger.info("symbol Time from database: ");
        logger.info(symbolTimeFromDb.toString());

        final List<LinkedHashMap<String, Object>> params = prepareParams(filteredSymbolList, symbolTimeFromDb);

        for (LinkedHashMap<String, Object> map : params) {
            List<Data> data = binance.downloadKlines(map);

//            data.forEach(d -> DBWriter.writeData(sessionFactory, d));

            int dataSize = data.size();

            while (dataSize == 1000) {

                LocalDateTime nextDate = data.get(data.size() - 1).getOpenTime().plusMinutes(1);
                Instant instant = nextDate.toInstant(ZoneOffset.UTC);
                Long date = instant.toEpochMilli();

                map.replace("startTime", date);
                List<Data> downloadedData = binance.downloadKlines(map);

                data.addAll(downloadedData);
                dataSize = downloadedData.size();

//                Symbol symbol = new Symbol();
//                symbol.setSymbolName(String.valueOf(map.get("symbol")));
//
//                final LocalDateTime lastDate = dbReader.readLastDate(symbol);
//                final HashMap<String,LocalDateTime> updateDateHashMap = new HashMap<>();
//                updateDateHashMap.put(symbol.getSymbolName(),lastDate);
//
//                final List<LinkedHashMap<String, Object>> moreParams = prepareParams(filteredSymbolList, updateDateHashMap);
//
//                for(LinkedHashMap<String,Object> moreMap : moreParams) {
//                    final List<Data> moreData = binance.downloadKlines(moreMap); //inside hard coded binance1d
//                    moreData.forEach(d -> DBWriter.writeData(sessionFactory, d));
//                    dataSize = moreData.size();
//                }
            }

            DBWriter.writeDatainBatch(sessionFactory,data);

        }
    }


    private List<LinkedHashMap<String, Object>> prepareParams(List<String> symbolsFromBinance, HashMap<String, LocalDateTime> symbolTimeFromDb) {
        List<LinkedHashMap<String, Object>> preparedParamList = new ArrayList<>();
        for (String symbol : symbolsFromBinance) {

            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("symbol", symbol);
            params.put("interval", "1m"); //  for daily timeframe use 1d
            params.put("limit", 1000);    //default 500 max 1000

            System.out.println("check if " + symbol + " is in database " + symbolTimeFromDb.containsKey(symbol));

            if (!symbolTimeFromDb.containsKey(symbol)) {
                LocalDateTime newStartDate = LocalDateTime.of(2010, 1, 1, 0, 0, 0);
                Instant instant = newStartDate.toInstant(ZoneOffset.UTC);
                long convertedTime = instant.toEpochMilli();
                params.put("startTime", convertedTime);

                preparedParamList.add(params);
            } else {
                System.out.println("symbol " + symbol + " , found in database");

                LocalDateTime dateInDb = symbolTimeFromDb.get(symbol);
                LocalDateTime newStartDate = dateInDb.plusMinutes(1); // TODO change this when downloading daily

                System.out.println("found latest date: " + dateInDb + " new calculated date: " + newStartDate);
                Instant inst = newStartDate.toInstant(ZoneOffset.UTC);
                long convertedTime = inst.toEpochMilli();

                params.put("startTime", convertedTime);
                preparedParamList.add(params);
            }
        }
        return preparedParamList;
    }

    @NotNull
    private BinanceDownloader configureDownloader() {
        SpotClientImpl client = new SpotClientImpl();
        client.setShowLimitUsage(true); //important option to enable
        Market market = client.createMarket();
        return new BinanceDownloader(market);
    }

    private List<String> getListOfSymbolsUSDT(BinanceDownloader binance, String currency) {
        List<String> symbolList = binance.getTickers();
        return symbolList.stream().filter(s -> s.endsWith(currency)).collect(Collectors.toList());
    }

}
