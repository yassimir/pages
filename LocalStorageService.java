package com.gharnata.storage;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final Set<String> DOSSIERS_AUTORISES =
            Set.of("produits", "paiements");

    private final Path rootLocation;

    public LocalStorageService(StorageProperties properties) {

        this.rootLocation = Paths.get(properties.getLocation())
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException(
                    "Impossible d'initialiser le stockage", e);
        }
    }

    @Override
    public String store(MultipartFile file, String dossier, String typeMime) {

        if (file == null || file.isEmpty()) {
            throw new StorageException("Le fichier est vide.");
        }

        if (!DOSSIERS_AUTORISES.contains(dossier)) {
            throw new StorageException("Dossier de stockage non autorisé.");
        }

        String nomFichier = UUID.randomUUID() + obtenirExtension(typeMime);

        Path destination = rootLocation
                .resolve(dossier)
                .normalize();

        if (!destination.startsWith(rootLocation)) {
            throw new StorageException("Chemin de stockage invalide.");
        }

        try {
            Files.createDirectories(destination);

            Path fichier = destination
                    .resolve(nomFichier)
                    .normalize();

            if (!fichier.startsWith(destination)) {
                throw new StorageException("Chemin de fichier invalide.");
            }

            Files.copy(file.getInputStream(), fichier);

            return rootLocation.relativize(fichier)
                    .toString()
                    .replace("\\", "/");

        } catch (IOException e) {
            throw new StorageException(
                    "Erreur lors de l'enregistrement du fichier.", e);
        }
    }

    @Override
    public Resource load(String chemin) {

        try {
            Path fichier = rootLocation
                    .resolve(chemin)
                    .normalize();

            if (!fichier.startsWith(rootLocation)) {
                throw new StorageException("Chemin de fichier invalide.");
            }

            if (!Files.isRegularFile(fichier)) {
                throw new StorageException("Fichier introuvable.");
            }

            Resource resource = new UrlResource(fichier.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new StorageException("Fichier inaccessible.");
            }

            return resource;

        } catch (MalformedURLException e) {
            throw new StorageException("Impossible de lire le fichier.", e);
        }
    }

    @Override
    public void delete(String chemin) {

        try {
            Path fichier = rootLocation
                    .resolve(chemin)
                    .normalize();

            if (!fichier.startsWith(rootLocation)) {
                throw new StorageException("Chemin de fichier invalide.");
            }

            Files.deleteIfExists(fichier);

        } catch (IOException e) {
            throw new StorageException(
                    "Impossible de supprimer le fichier.", e);
        }
    }

    private String obtenirExtension(String typeMime) {

        return switch (typeMime) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new StorageException(
                    "Type de fichier non autorisé."
            );
        };
    }
}
