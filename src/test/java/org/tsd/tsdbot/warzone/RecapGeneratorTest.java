package org.tsd.tsdbot.warzone;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.haloapi.model.stats.TeamStat;
import org.tsd.tsdbot.haloapi.model.stats.warzone.WarzoneMatch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(JukitoRunner.class)
public class RecapGeneratorTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.setVisibility(mapper.getVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }

    @Test
    public void testIdentifyTeamByRegulars(RecapGenerator recapGenerator) throws Exception {
        WarzoneMatch match = mapper.readValue(
                getClass().getResourceAsStream("/haloapi/warzone-assault-carnage.json"), WarzoneMatch.class
        );

        Set<String> regulars = new HashSet<>(Arrays.asList("Schooly D"));
        TeamStat team = recapGenerator.identifyTeamByRegulars(match, regulars);
        assertEquals(0, team.getTeamId());

        regulars = new HashSet<>(Arrays.asList("Schooly D", "tarehart", "Panda Owning Em"));
        team = recapGenerator.identifyTeamByRegulars(match, regulars);
        assertEquals(0, team.getTeamId());

        regulars = new HashSet<>(Arrays.asList("imperatorpat", "Wameedooo", "Im RubberDuck"));
        team = recapGenerator.identifyTeamByRegulars(match, regulars);
        assertEquals(1, team.getTeamId());
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {

        }
    }
}
