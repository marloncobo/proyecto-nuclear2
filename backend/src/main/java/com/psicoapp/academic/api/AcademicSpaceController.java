package com.psicoapp.academic.api;

import com.psicoapp.academic.application.AcademicAdministrationFacade;
import com.psicoapp.academic.application.AcademicAdministrationFacade.Space;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academic/spaces")
public class AcademicSpaceController {
    public record SpaceRequest(@NotBlank @Size(max = 150) String name, @NotBlank @Size(max = 30) String period) { }

    private final AcademicAdministrationFacade academic;

    public AcademicSpaceController(AcademicAdministrationFacade academic) {
        this.academic = academic;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Space create(Principal principal, @Valid @RequestBody SpaceRequest request) {
        return academic.create(UUID.fromString(principal.getName()), request.name(), request.period());
    }

    @GetMapping("/{id}")
    public Space get(Principal principal, @PathVariable UUID id) {
        return academic.get(UUID.fromString(principal.getName()), id);
    }

    @GetMapping
    public List<Space> list(Principal principal) {
        return academic.list(UUID.fromString(principal.getName()));
    }
}
