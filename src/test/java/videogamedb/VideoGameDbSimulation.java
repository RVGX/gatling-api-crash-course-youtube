package videogamedb;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class VideoGameDbSimulation extends Simulation {

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://videogamedb.uk:443/api")
            .acceptHeader("application/json");

    private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "5"));
    private static final int RAMP_DURATION = Integer.parseInt(System.getProperty("RAMP_DURATION", "10"));

    private static FeederBuilder<Object> jsonFeeder = jsonFile("data/gameJsonFile.json").random();

    @Override
    public void before() {
        System.out.println("Running test with " + USER_COUNT + " users");
        System.out.println("Ramping over " + RAMP_DURATION + " seconds");
    }
    
    private static ChainBuilder getAllGames = exec(
            http("Get All Video Games")
                    .get("/videogame")
                    .check(status().is(200)));

    private static ChainBuilder authenticate = exec(
            http("Authenticate")
                    .post("/authenticate")
                    .body(ElFileBody("bodies/authenticate.json")).asJson()
                    .check(jmesPath("token").saveAs("jwtToken"))
                    .check(status().is(200)));

    private static ChainBuilder createNewGame = feed(jsonFeeder)
            .exec(http("Create New Game - #{name}")
                    .post("/videogame")
                    .header("Authorization", "Bearer #{jwtToken}")
                    .body(ElFileBody("bodies/NewGameTemplate.json")).asJson()
                    // .check(jmesPath("id").ofInt().saveAs("id"))
                    .check(status().is(200)));

    private static ChainBuilder getLastPostedGame = exec(
            http("Get Last Posted Game - #{name}")
                    .get("/videogame/#{id}")
                    .check(status().is(200))
                    .check(jmesPath("name").isEL("#{name}")));

    private static ChainBuilder deleteLastPostedGame = exec(
            http("Delete Last Posted Game - #{name}")
                    .delete("/videogame/#{id}")
                    .header("Authorization", "Bearer #{jwtToken}")
                    .check(status().is(200))
                    .check(bodyString().is("Video game deleted")));

    ScenarioBuilder scn = scenario("Video Game DB - Stress Test")
            .exec(getAllGames).pause(2)
            .exec(authenticate).pause(2)
            .exec(createNewGame).pause(2)
            .exec(getLastPostedGame).pause(2)
            .exec(deleteLastPostedGame);

    {
        setUp(scn.injectOpen(nothingFor(5),rampUsers(USER_COUNT).during(RAMP_DURATION))
            .protocols(httpProtocol));
    }
}