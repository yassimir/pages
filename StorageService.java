package com.gharnata.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Enregistre un fichier dans le dossier spécifié.
     *
     * @return le chemin relatif du fichier enregistré
     */
    String store(MultipartFile file, String dossier, String typeMime);

    /**
     * Récupère un fichier à partir de son chemin relatif.
     */
    Resource load(String chemin);

    /**
     * Supprime un fichier à partir de son chemin relatif.
     */
    void delete(String chemin);
}
