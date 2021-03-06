package racejournal.service;

import racejournal.data.RaceDao;
import racejournal.data.RaceJdbcDao;
import racejournal.domain.RaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import racejournal.domain.Race;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by alaplante on 2/2/16.
 */
public class RaceService {
    private final static Logger logger = LoggerFactory.getLogger(RaceService.class);

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private RaceDao raceDao;

    private String bootstrapFile;
    private String protoBufFile;

    public void setBootstrapFile(String bootstrapFile) {
        this.bootstrapFile = bootstrapFile;
    }

//    public void setProtoBufFile(String protoBufFile) {
//        this.protoBufFile = protoBufFile;
//    }

//    public List<Race> getRacesByType(String type) {
//        return raceDao.fetchRacesByType(type.toUpperCase());
//    }

    public void bootstrap() {
        List<Race> races = raceDao.fetchRaces();
        if(races.isEmpty()) { // Bootstrap
            logger.info("DB empty thus bootstrap");
            try {
                races = pullRemoteAndParse();
//                races = parseCsv(bootstrapFile); // dont spam coloradocycling.org while testing
            } catch(Exception e) {
                logger.error("Error pulling or parsing remote file", e);
                races = parseCsv(bootstrapFile);
            }
            // Save to DB
            logger.info("Save {} races to DB", races.size());
            raceDao.saveRaces(races);
        }
    }

    public List<Race> fetchRaces() {
        return raceDao.fetchRaces();
    }

    public Race fetchRace(Long id) {
        return raceDao.fetchRace(id);
    }

    public void deleteRace(Long id) {
        raceDao.deleteRace(id);
    }

    public void saveRaces(List<Race> races) {
        raceDao.saveRaces(races);
    }

    public void updateRaces(List<Race> races) {
        raceDao.updateRaces(races);
    }

    public List<Race> pullRemoteAndParse() throws Exception {
        List<Race> races = new ArrayList<Race>();
        try {
            URL url = new URL("http://www.coloradocycling.org/calendar/download");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                Race race = parseCsvLine(line);
                if(race!= null) races.add(race);
            }
            reader.close();
        } catch(Exception e) {
            logger.error("Error pulling file");
            throw e;
        }
        return races;
    }

//    public List<Race> loadRaceData() {
//        List<Race> races = null;
//
//        // First try proto
//        long start = System.currentTimeMillis();
//        races = readProtoBuf(protoBufFile);
//        if(races.size() > 0) {
//            logger.info("Deserialized proto buff data to races in {} ms", System.currentTimeMillis() - start);
//            return races;
//        }
//
//        // Otherwise bootstrap
//        start = System.currentTimeMillis();
//        races = parseCsv(bootstrapFile);
//        logger.info("Parsed CSV in {} ms", System.currentTimeMillis() - start);
//
//        // Persist as proto buf for next time
//        start = System.currentTimeMillis();
//        saveProtoBuf(races);
//        logger.info("Serialized races to proto buff data in {} ms", System.currentTimeMillis() - start);
//
//        // Ensure all good
//        start = System.currentTimeMillis();
//        races = readProtoBuf(protoBufFile);
//        logger.info("Deserialized proto buff data to races in {} ms", System.currentTimeMillis() - start);
//
//        return races;
//    }

//    private List<Race> readProtoBuf(String file) {
//        logger.info("Load race data from protocol buffer file");
//        List<Race> races = new ArrayList<Race>();
//        RaceDirectoryProto.RaceDirectory.Builder raceDirectoryBuilder = RaceDirectoryProto.RaceDirectory.newBuilder();
//
//        try {
//            raceDirectoryBuilder.mergeFrom(new FileInputStream(protoBufFile));
//        } catch(IOException e) {
//            logger.error("Error reading proto buf file", e);
//        }
//
//        RaceDirectoryProto.RaceDirectory raceDirectory = raceDirectoryBuilder.build();
//        for(RaceDirectoryProto.Race race : raceDirectory.getRaceList()) {
//            races.add(RaceProtoMapper.mapFrom(race));
//        }
//        logger.info("Loaded race data from protocol buffer file");
//        return races;
//    }

//    private void saveProtoBuf(List<Race> races) {
//        logger.info("Save race data to protocol buffer file");
//        RaceDirectoryProto.RaceDirectory.Builder raceDirectoryBuilder = RaceDirectoryProto.RaceDirectory.newBuilder();
//        for(Race race : races) {
//            raceDirectoryBuilder.addRace(RaceProtoMapper.mapTo(race));
//        }
//        try {
//            FileOutputStream fos = new FileOutputStream(protoBufFile);
//            raceDirectoryBuilder.build().writeTo(fos);
//            fos.close();
//            logger.info("Race data saved to protocol buffer file");
//        } catch(IOException e) {
//            logger.error("Error writing proto buf file", e);
//        }
//
//    }

    private List<Race> parseCsv(String file) {
        logger.info("Load and parse file {}", file);
        List<Race> races = new ArrayList<Race>();
        try {
            Resource resource = resourceLoader.getResource(String.format("classpath:%s", file));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String line = null;
            while((line = bufferedReader.readLine()) != null) {
                Race race = parseCsvLine(line);
                if(race!= null) races.add(race);
            }
            logger.info("File {} successfully parsed", file);
        } catch(IOException e) {
            // log error
            logger.error("Error reading file", e);
        }
        return races;
    }

    //   0            1                                          2      3         4       5
    // 1853,"Lucky Pie Criterium -  CO Master Crit Championships",,08/21/2016,Louisville,CO,,,,Y,
    private Race parseCsvLine(String line) {
        logger.info("Parse line {}", line);
        if(line.contains("\"event number\"")) return null; // skip header
        if(line.contains("GC")) return null;
        if(line.contains("(SMP 1-2, SM 3, SW P-1-2)")) {
            line = line.replaceAll("SMP 1-2, SM 3, SW P-1-2", "SMP 1-2 SM 3 SW P-1-2");
        }

        String[] tokens = line.split(",");
        Race race = new Race();
//        race.setId(atomicLong.incrementAndGet());
        race.setName(tokens[1].replaceAll("\"","").trim().replaceAll("'", "").trim());
        String[] dateTokens = tokens[3].split("/");
        race.setDate(LocalDate.of(Integer.parseInt(dateTokens[2]), Integer.parseInt(dateTokens[0]), Integer.parseInt(dateTokens[1])));
        race.setCity(tokens[4].replaceAll("\"","").trim().replaceAll("'", "").trim());
        race.setState(tokens[5].replaceAll("\"","").trim().replaceAll("'", "").trim());
        race.setRaceType(categorizeRaceType(race.getName()));

        return race;
    }

    private RaceType categorizeRaceType(String name) {
//        name = name.toLowerCase();
        if(name.contains("Hill") || name.contains("HC")) {
            return RaceType.HILL_CLIMB;
        } else if(name.contains("TT") || name.contains("Time")) {
            return RaceType.TIME_TRIAL;
        } else if(name.contains("Crit")) {
            return RaceType.CRITERIUM;
        } else if(name.contains("Road") || name.contains("RR") || name.contains("Circuit")) {
            return RaceType.ROAD_RACE;
        }  else {
            logger.info("Could not categorize race type for {}", name);
            return RaceType.OTHER;
        }
    }

//    public Map<RaceType, List<Race>> partitionRacesByRaceType(List<Race> races) {
//        Map<RaceType, List<Race>> map = new HashMap<RaceType, List<Race>>();
//        for(Race race : races) {
//            if(map.get(race.getRaceType()) == null) {
//                map.put(race.getRaceType(), new ArrayList<Race>());
//            }
//            map.get(race.getRaceType()).add(race);
//        }
//        return map;
//    }

    public void test() {
        raceDao.testCreate();
        raceDao.testGet();
    }
}

