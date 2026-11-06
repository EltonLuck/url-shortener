package com.elton_luck.url_shortener.service;

import java.net.URI;
import java.time.LocalDateTime;

import com.elton_luck.url_shortener.entity.Link;
import com.elton_luck.url_shortener.repository.LinkRepository;
import com.elton_luck.url_shortener.util.Base62;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LinkService {

    private final LinkRepository links;

    public LinkService(LinkRepository links) {
        this.links = links;
    }

    @Transactional
    public Link create(String originalUrl) {
        String url = validate(originalUrl);
        Link link = new Link();
        link.setOriginalUrl(url);
        link.setCreatedAt(LocalDateTime.now());
        link = links.save(link);              // Postgres assigns the ID here
        link.setShortCode(Base62.encode(link.getId()));
        return links.save(link);              // second save stores the code
    }

    private String validate(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "url is required");
        }
        String url = originalUrl.trim();
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if ((!"http".equals(scheme) && !"https".equals(scheme))
                    || uri.getHost() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "url must start with http:// or https://");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "url is not a valid URL");
        }
        return url;
    }
}

