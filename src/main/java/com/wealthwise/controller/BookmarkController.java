package com.wealthwise.controller;

import com.wealthwise.dto.response.BookmarkResponse;
import com.wealthwise.dto.response.SuccessResponse;
import com.wealthwise.service.FundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final FundService fundService;

    @GetMapping
    public ResponseEntity<BookmarkResponse> getBookmarks(Authentication authentication) {
        UUID userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(fundService.getBookmarks(userId));
    }

    @PostMapping("/{fundId}")
    public ResponseEntity<SuccessResponse> addBookmark(
        @PathVariable UUID fundId,
        Authentication authentication
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(fundService.addBookmark(userId, fundId));
    }

    @DeleteMapping("/{fundId}")
    public ResponseEntity<SuccessResponse> removeBookmark(
        @PathVariable UUID fundId,
        Authentication authentication
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(fundService.removeBookmark(userId, fundId));
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
