package com.elton_luck.url_shortener.controller;

import com.elton_luck.url_shortener.service.LinkService;
import com.elton_luck.url_shortener.entity.Link;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private final LinkService links;

    public LinkController(LinkService links) {
        this.links = links;
    }

    public record CreateLinkRequest(String url) {}
    public record CreateLinkResponse(String shortCode, String shortUrl, String originalUrl) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateLinkResponse create(@RequestBody CreateLinkRequest request) {
        Link link = links.create(request.url());
        return new CreateLinkResponse(
                link.getShortCode(),
                "http://localhost:8080/" + link.getShortCode(),
                link.getOriginalUrl());
    }
}
