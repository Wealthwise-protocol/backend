package com.wealthwise.controller;

import com.wealthwise.dto.request.CreateSipRequest;
import com.wealthwise.dto.request.UpdateSipRequest;
import com.wealthwise.dto.response.SipResponse;
import com.wealthwise.service.SipService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/sips")
@RequiredArgsConstructor
public class SipController {

    private final SipService sipService;

    @GetMapping
    public ResponseEntity<Map<String, List<SipResponse>>> getSips(
        @AuthenticationPrincipal String userId
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        return ResponseEntity.ok(Map.of("sips", sipService.getUserSips(userUuid)));
    }

    @PostMapping
    public ResponseEntity<Map<String, SipResponse>> createSip(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody CreateSipRequest request
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        SipResponse response = sipService.createSip(userUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("sip", response));
    }

    @PatchMapping
    public ResponseEntity<Map<String, SipResponse>> updateSip(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody UpdateSipRequest request
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        SipResponse response = sipService.updateSip(userUuid, request);
        return ResponseEntity.ok(Map.of("sip", response));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, SipResponse>> deleteSip(
        @AuthenticationPrincipal String userId,
        @RequestParam("id") UUID sipId
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        SipResponse response = sipService.deleteSip(userUuid, sipId);
        return ResponseEntity.ok(Map.of("sip", response));
    }

    private UUID getAuthenticatedUserId(String userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
