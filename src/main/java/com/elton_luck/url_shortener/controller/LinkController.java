package com.elton_luck.url_shortener.controller;

import com.elton_luck.url_shortener.service.LinkService;
import com.elton_luck.url_shortener.entity.Link;
import com.elton_luck.url_shortener.service.RateLimitService;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private final LinkService links;
    private final RateLimitService rateLimits;

    public LinkController(LinkService links, RateLimitService rateLimits) {
        this.links = links;
        this.rateLimits = rateLimits;
    }

    public record CreateLinkRequest(String url) {}
    public record CreateLinkResponse(String shortCode, String shortUrl, String originalUrl) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateLinkResponse create(@RequestBody CreateLinkRequest request, HttpServletRequest httpRequest) {

        ConsumptionProbe probe = rateLimits.tryCreate(httpRequest.getRemoteAddr());
        if (!probe.isConsumed()) {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "rate limit exceeded — try again in " + waitSeconds + " seconds");
        }

        Link link = links.create(request.url());
        return new CreateLinkResponse(
                link.getShortCode(),
                "http://localhost:8080/" + link.getShortCode(),
                link.getOriginalUrl());
    }
}
