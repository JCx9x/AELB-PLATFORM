package es.aelb.backendweb.infrastructure.web.dto;

import es.aelb.backendweb.domain.team.Team;
import es.aelb.backendweb.infrastructure.web.dto.response.PublicTeamResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PublicTeamResponseTest {

    @Test
    void publicResponseContainsNoResponsibleContactFields() {
        Team team = Team.create("AELB Madrid", "Ana", "Responsable", "+34600000000",
                "Madrid", "Madrid", null, null);

        PublicTeamResponse response = PublicTeamResponse.from(team);

        assertEquals(team.getId().value(), response.id());
        assertEquals("AELB Madrid", response.name());
        assertEquals("Madrid", response.province());
        assertEquals("Madrid", response.locality());
        assertFalse(PublicTeamResponse.class.getRecordComponents() == null);
        assertArrayEquals(new String[]{"id", "name", "province", "locality", "hasLogo", "logoType"},
                java.util.Arrays.stream(PublicTeamResponse.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName).toArray(String[]::new));
    }
}
