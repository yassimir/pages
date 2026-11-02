package com.gharnata.repository;

import com.gharnata.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepImage extends JpaRepository<Image,Long > {
}
