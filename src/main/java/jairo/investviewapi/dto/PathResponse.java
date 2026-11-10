package jairo.investviewapi.dto;

import java.util.List;

public record PathResponse(
        boolean found,
        List<String> path,
        int length
) {
}
