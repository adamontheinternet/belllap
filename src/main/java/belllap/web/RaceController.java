package belllap.web;

import belllap.data.RaceRepository;
import belllap.domain.Race;
import belllap.service.RaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by alaplante on 2/3/16.
 */
@RestController
public class RaceController {

    @Autowired
    RaceService raceService;

//    @Autowired
//    RaceRepository raceRepository;
//
//    @RequestMapping(value = "/email", method = RequestMethod.GET)
//    public String email(@RequestParam String name) {
//        return raceRepository.getEmailByName(name);
//    }

    @RequestMapping(value = "/racesByType", method = RequestMethod.GET, produces = "application/json")
    public List<Race> races(@RequestParam("type") String type) {
        return raceService.getRacesByType(type.trim());
    }

    @RequestMapping(value = "/races", method = RequestMethod.GET, produces = "application/json")
    public List<Race> races() {
        List<Race> races = raceService.loadRaceData();
        return races;
    }
}
