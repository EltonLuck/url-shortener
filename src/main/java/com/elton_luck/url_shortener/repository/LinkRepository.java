package com.elton_luck.url_shortener.repository;

import com.elton_luck.url_shortener.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, Long> {
}
