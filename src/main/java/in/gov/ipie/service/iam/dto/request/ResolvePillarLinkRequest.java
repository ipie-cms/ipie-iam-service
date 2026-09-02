package in.gov.ipie.service.iam.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResolvePillarLinkRequest(

        @NotBlank
        String pillarType,

        @NotBlank
        String externalPillarId) {
}
