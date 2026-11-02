package com.gharnata.service;

import com.gharnata.entity.Image;
import com.gharnata.entity.Produit;
import com.gharnata.repository.RepImage;
import com.gharnata.storage.StorageException;
import com.gharnata.storage.StorageService;

import org.imgscalr.Scalr;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ServImage {

    private static final int MAX_WIDTH = 1200;
    private static final int MAX_HEIGHT = 1200;

    private static final float JPEG_QUALITY = 0.85f;

    private static final String DOSSIER = "produits";
    private static final String TYPE_MIME = "image/jpeg";

    private static final String IMAGE_DEFAUT =
            "static/images/defaut.JPEG";

    private final RepImage repImage;
    private final StorageService storageService;

    public ServImage(
            RepImage repImage,
            StorageService storageService) {

        this.repImage = repImage;
        this.storageService = storageService;
    }

    /**
     * Enregistre une nouvelle image de produit.
     * Si aucune image n'est fournie, utilise l'image par défaut.
     */
    @Transactional
    public Image treatPic(MultipartFile fichier) {

        if (fichier == null || fichier.isEmpty()) {

            Image image = new Image();

            image.setCheminFichier("produits/defaut.jpg");
            image.setTypeMime("image/jpeg");
            image.setNomOriginal("defaut.jpg");

            return repImage.save(image);
        }

        BufferedImage original;

        try (InputStream input = fichier.getInputStream()) {
            original = ImageIO.read(input);

        } catch (IOException e) {
            throw new StorageException(
                    "Impossible de lire l'image du produit.",
                    e
            );
        }

        if (original == null) {
            throw new StorageException(
                    "Le fichier fourni n'est pas une image valide."
            );
        }

        String nomOriginal = nettoyerNomOriginal(
                fichier.getOriginalFilename()
        );

        return enregistrerImage(original, nomOriginal, null);
    }

    /**
     * Remplace l'image d'un produit existant.
     * L'ancienne image physique est supprimée uniquement
     * après validation de la transaction.
     */
    @Transactional
    public void modifyPic(MultipartFile fichier, long id) {

        if (fichier == null || fichier.isEmpty()) {
            return;
        }

        Image image = repImage.findById(id)
                .orElseThrow(() -> new StorageException(
                        "Image du produit introuvable."
                ));

        BufferedImage original;

        try (InputStream input = fichier.getInputStream()) {
            original = ImageIO.read(input);

        } catch (IOException e) {
            throw new StorageException(
                    "Impossible de lire la nouvelle image.",
                    e
            );
        }

        if (original == null) {
            throw new StorageException(
                    "Le fichier fourni n'est pas une image valide."
            );
        }

        String ancienChemin = image.getCheminFichier();

        enregistrerImage(
                original,
                nettoyerNomOriginal(fichier.getOriginalFilename()),
                image
        );

        enregistrerNettoyageTransactionnel(
                image.getCheminFichier(),
                ancienChemin
        );

        repImage.save(image);
    }

    /**
     * Redimensionne, compresse et enregistre l'image.
     */
    private Image enregistrerImage(
            BufferedImage original,
            String nomOriginal,
            Image imageExistante) {

        exigerTransactionActive();

        BufferedImage redimensionnee = redimensionner(original);

        AtomicLong taille = new AtomicLong();

        String chemin = storageService.store(
                DOSSIER,
                TYPE_MIME,
                output -> {

                    CountingOutputStream countingOutput =
                            new CountingOutputStream(output);

                    compresserJPEG(
                            redimensionnee,
                            countingOutput
                    );

                    countingOutput.flush();

                    taille.set(countingOutput.getCount());
                }
        );

        // En cas de rollback, le nouveau fichier sera supprimé.
        enregistrerNettoyageTransactionnel(chemin, null);

        Image image = imageExistante != null
                ? imageExistante
                : new Image();

        image.setCheminFichier(chemin);
        image.setTypeMime(TYPE_MIME);
        image.setNomOriginal(nomOriginal);
        image.setTaille(taille.get());

        if (imageExistante == null) {
            return repImage.save(image);
        }

        return image;
    }

    /**
     * Réduit les dimensions uniquement si elles dépassent 1200 px.
     * Les proportions sont conservées.
     */
    private BufferedImage redimensionner(BufferedImage original) {

        if (original.getWidth() <= MAX_WIDTH
                && original.getHeight() <= MAX_HEIGHT) {

            return original;
        }

        return Scalr.resize(
                original,
                Scalr.Method.QUALITY,
                Scalr.Mode.AUTOMATIC,
                MAX_WIDTH,
                MAX_HEIGHT
        );
    }

    /**
     * Convertit l'image en JPEG avec une qualité de 85 %.
     * Les zones transparentes sont remplacées par un fond blanc.
     */
    private void compresserJPEG(
            BufferedImage original,
            OutputStream output) throws IOException {

        BufferedImage rgbImage = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = rgbImage.createGraphics();

        try {
            graphics.setColor(Color.WHITE);

            graphics.fillRect(
                    0,
                    0,
                    original.getWidth(),
                    original.getHeight()
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );

            graphics.drawImage(original, 0, 0, null);

        } finally {
            graphics.dispose();
        }

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByFormatName("jpg");

        if (!writers.hasNext()) {
            throw new IOException(
                    "Aucun encodeur JPEG disponible."
            );
        }

        ImageWriter writer = writers.next();

        try {
            ImageWriteParam param =
                    writer.getDefaultWriteParam();

            param.setCompressionMode(
                    ImageWriteParam.MODE_EXPLICIT
            );

            param.setCompressionQuality(JPEG_QUALITY);

            ImageOutputStream imageOutput =
                    ImageIO.createImageOutputStream(output);

            if (imageOutput == null) {
                throw new IOException(
                        "Impossible de créer le flux JPEG."
                );
            }

            try (imageOutput) {

                writer.setOutput(imageOutput);

                writer.write(
                        null,
                        new IIOImage(rgbImage, null, null),
                        param
                );

                imageOutput.flush();
            }

        } finally {
            writer.dispose();
        }
    }

    /**
     * Vérifie que la méthode est exécutée dans une transaction.
     */
    private void exigerTransactionActive() {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException(
                    "L'enregistrement de l'image nécessite une transaction."
            );
        }
    }

    /**
     * Nettoyage des fichiers selon le résultat de la transaction.
     *
     * Rollback : suppression du nouveau fichier.
     * Commit : suppression éventuelle de l'ancien fichier.
     */
    private void enregistrerNettoyageTransactionnel(
            String nouveauChemin,
            String ancienChemin) {

        exigerTransactionActive();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status) {

                        if (status == STATUS_ROLLED_BACK) {

                            supprimerSilencieusement(nouveauChemin);

                        } else if (status == STATUS_COMMITTED
                                && ancienChemin != null
                                && !ancienChemin.isBlank()) {

                            supprimerSilencieusement(ancienChemin);
                        }
                    }
                }
        );
    }

    private void supprimerSilencieusement(String chemin) {

        if (chemin == null
                || chemin.isBlank()
                || chemin.equals("produits/defaut.jpg")) {
            return;
        }

        try {
            storageService.delete(chemin);

        } catch (RuntimeException e) {
            System.err.println(
                    "Impossible de supprimer le fichier : "
                            + chemin + " - " + e.getMessage()
            );
        }
    }

    /**
     * Nettoie le nom original pour éviter de conserver un chemin.
     */
    private String nettoyerNomOriginal(String nom) {

        if (nom == null || nom.isBlank()) {
            return "image";
        }

        nom = nom.replace("\\", "/");

        nom = nom.substring(
                nom.lastIndexOf('/') + 1
        ).trim();

        if (nom.length() > 255) {
            nom = nom.substring(0, 255);
        }

        return nom.isBlank() ? "image" : nom;
    }

    /**
     * Compte les octets écrits sans conserver le contenu en mémoire.
     */
    private static class CountingOutputStream
            extends FilterOutputStream {

        private long count = 0;

        public CountingOutputStream(OutputStream output) {
            super(output);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            count++;
        }

        @Override
        public void write(
                byte[] bytes,
                int offset,
                int length) throws IOException {

            out.write(bytes, offset, length);
            count += length;
        }

        public long getCount() {
            return count;
        }
    }
}