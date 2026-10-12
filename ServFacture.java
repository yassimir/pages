package com.gharnata.service;

import com.gharnata.compenents.ResourceUtils;
import com.gharnata.entity.*;
import com.gharnata.repository.RepFacture;
import com.gharnata.repository.RepLigneCommand;
import com.gharnata.repository.RepLigneFac;
import com.gharnata.repository.RepProduit;
import com.gharnata.service.footer.PdfFooterProdsEvent;
import com.itextpdf.text.*;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import com.gharnata.service.footer.BonFooter;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServFacture {

    private static final int MAX_CENTIMES = 50_000_000;
    private static final int MAX_ETATS_ECHEC = 2_000_000;

    private final RepFacture repFacture;
    private final RepLigneFac repLigneFac;
    private final NumeroDocumentService numeroDocumentService;
    private final ServPaiement servPaiement;
    private final RepProduit repProduit;
    private final RepLigneCommand repLigneCommand;

    public ServFacture(
            RepFacture repFacture,
            RepLigneFac repLigneFac,
            NumeroDocumentService numeroDocumentService,
            ServPaiement servPaiement,
            RepProduit repProduit,
            RepLigneCommand repLigneCommand) {

        this.repFacture = repFacture;
        this.repLigneFac = repLigneFac;
        this.numeroDocumentService = numeroDocumentService;
        this.servPaiement = servPaiement;
        this.repProduit = repProduit;
        this.repLigneCommand = repLigneCommand;
    }

    private long enCentimes(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2).longValueExact();
    }

    private record Calcul(boolean[] atteint, int[] item) {}

    /** Quelles sommes (en centimes) sont atteignables avec prix[i] disponible qte[i] fois ? */
    private Calcul sommesAtteignables(long[] prix, int[] qte, int cible) {
        boolean[] atteint = new boolean[cible + 1];
        int[] item = new int[cible + 1];
        int[] used = new int[cible + 1];
        atteint[0] = true;
        for (int i = 0; i < prix.length; i++) {
            if (prix[i] <= 0 || prix[i] > cible || qte[i] <= 0) continue;
            int p = (int) prix[i];
            Arrays.fill(used, 0);
            for (int s = p; s <= cible; s++) {
                if (!atteint[s] && atteint[s - p] && used[s - p] < qte[i]) {
                    atteint[s] = true;
                    item[s] = i;
                    used[s] = used[s - p] + 1;
                }
            }
        }
        return new Calcul(atteint, item);
    }

    /** Remonte le chemin pour savoir combien d'unités de chaque item composent la somme. */
    private Map<Integer, Integer> reconstruire(Calcul c, long[] prix, int somme) {
        Map<Integer, Integer> q = new HashMap<>();
        for (int s = somme; s > 0; ) {
            int i = c.item()[s];
            q.merge(i, 1, Integer::sum);
            s -= (int) prix[i];
        }
        return q;
    }

    private List<LigneFacture> calculerLignes(long montantCible, Commande commande) {
        if (montantCible <= 0 || montantCible > MAX_CENTIMES) {
            throw new IllegalArgumentException("Montant HT invalide ou trop élevé.");
        }
        int cible = (int) montantCible;

        List<LigneCommande> lc = repLigneCommand.findAllByCommande(commande);
        if (lc == null || lc.isEmpty()) {
            throw new IllegalStateException("La commande ne contient aucune ligne.");
        }

        // ---- Étape 1 : produits de la commande ----
        long[] prix = new long[lc.size()];
        int[] qte = new int[lc.size()];
        for (int i = 0; i < lc.size(); i++) {
            LigneCommande l = lc.get(i);
            if (l.getQuantity() > 0 && Double.isFinite(l.getPrixVente()) && l.getPrixVente() > 0) {
                prix[i] = enCentimes(l.getPrixVente());
                qte[i] = l.getQuantity();
            }
        }
        Calcul c1 = sommesAtteignables(prix, qte, cible);
        int montantCommande = 0;
        for (int s = cible; s > 0; s--) {
            if (c1.atteint()[s]) { montantCommande = s; break; }
        }
        if (montantCommande == 0) {
            throw new IllegalStateException("Aucun produit de la commande ne peut être retenu pour cette facture.");
        }

        List<LigneFacture> lignes = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : reconstruire(c1, prix, montantCommande).entrySet()) {
            LigneCommande src = lc.get(e.getKey());
            LigneFacture lf = new LigneFacture();
            lf.setProductId(src.getProductId());
            lf.setRef(src.getRef());
            lf.setProductName(src.getProductName());
            lf.setPrixVente(src.getPrixVente());
            lf.setQuantity(e.getValue());
            lf.setMontant(BigDecimal.valueOf(prix[e.getKey()] * e.getValue(), 2).doubleValue());
            lignes.add(lf);
        }

        // ---- Étape 2 : compléter avec le catalogue ----
        int reste = cible - montantCommande;
        if (reste > 0) {
            List<Produit> produits = repProduit.findByPrixVenteLessThanEqual(reste / 100.0);
            long[] prix2 = new long[produits.size()];
            int[] qte2 = new int[produits.size()];
            for (int i = 0; i < produits.size(); i++) {
                Produit p = produits.get(i);
                if (p.getQuantite() > 0 && Double.isFinite(p.getPrixVente()) && p.getPrixVente() > 0) {
                    prix2[i] = enCentimes(p.getPrixVente());
                    qte2[i] = p.getQuantite();
                }
            }
            Map<Integer, Integer> complement = completerParPrixDecroissant(prix2, qte2, reste);
            if (complement == null) {
                throw new IllegalStateException("Impossible de compléter exactement le montant. Reste à compléter : "
                        + BigDecimal.valueOf(reste, 2) + " DH.");
            }
            for (Map.Entry<Integer, Integer> e : complement.entrySet()) {
                Produit src = produits.get(e.getKey());
                LigneFacture lf = new LigneFacture();
                lf.setProductId(src.getId());
                lf.setRef(src.getRef());
                lf.setProductName(src.getName());
                lf.setPrixVente(src.getPrixVente());
                lf.setQuantity(e.getValue());
                lf.setMontant(BigDecimal.valueOf(prix2[e.getKey()] * e.getValue(), 2).doubleValue());
                lignes.add(lf);
            }
        }

        // ---- Vérification du total ----
        long total = 0;
        for (LigneFacture l : lignes) total += enCentimes(l.getMontant());
        if (total != montantCible) {
            throw new IllegalStateException("Le total des lignes ne correspond pas au montant HT.");
        }
        return lignes;
    }

    private Map<Integer, Integer> completerParPrixDecroissant(long[] prix, int[] qte, int reste) {
        // indices valides triés par prix décroissant
        List<Integer> ordre = new ArrayList<>();
        for (int i = 0; i < prix.length; i++) {
            if (prix[i] > 0 && prix[i] <= reste && qte[i] > 0) ordre.add(i);
        }
        ordre.sort((a, b) -> Long.compare(prix[b], prix[a]));

        int[] choix = new int[ordre.size()];
        Set<Long> echecs = new HashSet<>();

        if (!chercher(0, reste, ordre, prix, qte, choix, echecs)) {
            return null;
        }

        Map<Integer, Integer> resultat = new LinkedHashMap<>();
        for (int k = 0; k < ordre.size(); k++) {
            if (choix[k] > 0) resultat.put(ordre.get(k), choix[k]);
        }
        return resultat;
    }

    private boolean chercher(int pos, int reste, List<Integer> ordre, long[] prix, int[] qte,
                             int[] choix, Set<Long> echecs) {
        if (reste == 0) return true;
        if (pos == ordre.size()) return false;

        long cle = (long) pos * 1_000_000_007L + reste;
        if (echecs.contains(cle)) return false;

        int i = ordre.get(pos);
        int p = (int) prix[i];
        int max = Math.min(qte[i], reste / p);

        for (int k = max; k >= 0; k--) {
            choix[pos] = k;
            if (chercher(pos + 1, reste - k * p, ordre, prix, qte, choix, echecs)) {
                return true;
            }
        }
        choix[pos] = 0;

        if (echecs.size() > MAX_ETATS_ECHEC) {
            throw new IllegalStateException("La recherche des produits complémentaires est trop complexe.");
        }
        echecs.add(cle);
        return false;
    }



    @Transactional
    public Facture creerFactureDepuisCommande(Paiement paiement, Commande commande) {
        // ... tes validations actuelles inchangées ...

        // 1) Calcul pur : aucun verrou, aucune écriture
        long cible = enCentimes(paiement.getMontant() / 1.20);
        List<LigneFacture> lignes = calculerLignes(cible, commande);

        // 2) Seulement maintenant : numéro (verrou) + sauvegarde
        Facture facture = creeFacture(paiement);
        facture.setMtl(convertirMontantEnLettres(facture.getMontantTTC()));
        saveFac(facture);

        for (LigneFacture l : lignes) {
            l.setFacture(facture);
            saveLF(l);
        }
        return facture;
    }



    //------------------------------------------------------



    public Facture genererFacture(Facture facture, String tmp, HttpServletResponse response) throws Exception{
        if (facture == null) {
            throw new IllegalArgumentException("La facture est obligatoire.");
        }

        if (facture.getPaiement() == null) {
            throw new IllegalStateException(
                    "La facture n'est associée à aucun paiement."
            );
        }

        Paiement paiement = facture.getPaiement();

        if (paiement.getClient() == null) {
            throw new IllegalStateException(
                    "Le paiement n'est associé à aucun client."
            );
        }

        Client client = paiement.getClient();

        List<LigneFacture> lLF = repLigneFac.findAllByFacture(facture);
        Document document = new Document();
        try {
            // Créer une police en gras
            Font policeGras = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            //Font pg = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            String nomF= "FACTURE"+facture.getId()+"_"+client.getSte()+".pdf";
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(nomF));
            PdfFooterEvent event = new PdfFooterEvent();
            // Ajouter un événement pour gérer le pied de page
            writer.setPageEvent(event);
            document.open();
            //Image logo = Image.getInstance("classes/static/images/lg.png");
            //Image logo = Image.getInstance("src/main/resources/static/images/lg.png");
            Image logo = Image.getInstance(ResourceUtils.load("static/images/lg.png"));

            logo.scaleToFit(500, 150); // Ajuster la taille du logo selon vos besoins
            logo.setAlignment(Element.ALIGN_LEFT);
            document.add(logo);
            //informations de l'entreprise
            String cel = "STE "+ client.getSte()+"\n\n";
            if (client.getIce() != null && !client.getIce().trim().isEmpty() && !client.getIce().toLowerCase().startsWith("cin") && !client.getIce().toLowerCase().startsWith("ice")) {
                cel += "I.C.E : " + client.getIce();
            }
            cel+= "\n\n";
            if(!Objects.equals(client.getAdresse(), "")){
                cel+= client.getAdresse();
            }
            PdfPCell cellule = new PdfPCell(new Phrase(cel ,policeGras));
            //PdfPCell cellule = new PdfPCell(new Phrase("STE "+ facture.getClient().getSte()+"\n\nI.C.E : " + facture.getClient().getIce()+"\n\n"+facture.getClient().getAdresse(),policeGras));
            cellule.setBorder(Rectangle.BOX);
            cellule.setBorderColor(BaseColor.BLACK);
            cellule.setBorderWidth(1f);
            cellule.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellule.setPaddingTop(7f);
            cellule.setPaddingBottom(10f);
            PdfPTable tt = new PdfPTable(1);
            tt.setWidthPercentage(50);
            tt.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tt.addCell(cellule);
            document.add(tt);
            //informations de la facture
            PdfPTable tab = new PdfPTable(3); // 5 colonnes
            tab.setWidthPercentage(100);
            tab.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            tab.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            tab.setSpacingBefore(20f);// Ajouter un espacement avant la table
            tab.setSpacingAfter(20f);
            tab.addCell(new Phrase("Facture nº : "+ facture.getNumero(),policeGras));
            tab.addCell(new Phrase("Date : "+ facture.getDate(),policeGras));
            tab.addCell(new Phrase("Code Client : "+ client.getId(), policeGras));
            document.add(tab);
            // Ajouter le tableau des détails de la facture
            PdfPTable table = new PdfPTable(5); // 5 colonnes
            table.setWidthPercentage(100);
            table.getDefaultCell().setPaddingTop(2f);
            table.getDefaultCell().setPaddingBottom(5f);
            table.setWidths(new float[]{20,45,10, 10, 15});
            table.setHorizontalAlignment(Element.ALIGN_LEFT);

            // Ajouter une cellule pour chaque détail de la facture
            table.addCell(createHeaderCell("Code Article", policeGras));
            table.addCell(createHeaderCell("Désignation", policeGras));
            table.addCell(createHeaderCell("Qté", policeGras));
            table.addCell(createHeaderCell("P.U", policeGras));
            table.addCell(createHeaderCell("P.T", policeGras));

            // Ajouter des lignes de données (à remplacer par vos propres données)
            for (LigneFacture lf : lLF){
                table.addCell(lf.getRef());
                table.addCell(lf.getProductName());
                table.addCell(String.valueOf(lf.getQuantity()));
                table.addCell(String.format("%.2f",lf.getPrixVente()));
                table.addCell(String.format("%.2f", lf.getMontant()));
            }
            // Ajouter le tableau au document
            document.add(table);
            //les montants
            PdfPTable tm = new PdfPTable(3); // 5 colonnes
            tm.setWidthPercentage(100);
            tm.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            tm.getDefaultCell().setPaddingTop(5f);
            tm.getDefaultCell().setPaddingBottom(5f);
            tm.setSpacingBefore(20f);
            tm.setSpacingAfter(20f);
            //header
            tm.addCell(createHeaderCell("Montant total Hors taxes (HT)", policeGras));
            tm.addCell(createHeaderCell("Montant TVA (20%)", policeGras));
            tm.addCell(createHeaderCell("Montant TTC", policeGras));
            //body
            tm.addCell(String.format("%.2f",facture.getMontantHT())+" DH");
            tm.addCell(String.format("%.2f",facture.getmTVA())+" DH");
            tm.addCell(new Phrase(String.format("%.2f",facture.getMontantTTC())+" DH", policeGras));
            document.add(tm);
            //mode de paie
            PdfPTable mp = new PdfPTable(2);
            mp.setHorizontalAlignment(Element.ALIGN_LEFT);
            mp.setWidthPercentage(100);
            mp.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            mp.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            mp.setSpacingAfter(10f);
            mp.setWidths(new float[]{22,78});
            //heaed
            mp.addCell(new Phrase("Mode de Paiment : ", policeGras));
            mp.addCell(new Phrase(facture.getPaiement().getModPai()));
            document.add(mp);
            //mtl
            PdfPTable mtl = new PdfPTable(2);
            mtl.setHorizontalAlignment(Element.ALIGN_LEFT);
            mtl.setWidthPercentage(100);
            mtl.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            mtl.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            mtl.setSpacingAfter(20f);
            mtl.setWidths(new float[]{46,54});
            //head
            mtl.addCell(new Phrase("Arrêté la présente facture à la somme de: ", policeGras));
            mtl.addCell(new Phrase(facture.getMtl()));
            document.add(mtl);
            if ("on".equals(tmp)) {
                try {
                    //Image cachet = Image.getInstance("classes/static/images/cachet.png");
                    //Image cachet = Image.getInstance("src/main/resources/static/images/cachet.png");
                    Image cachet = Image.getInstance(ResourceUtils.load("static/images/cachet.png"));
                    cachet.scaleToFit(150, 150); // Redimensionner selon besoin
                    cachet.setAbsolutePosition(420f, 50f); // Position sur la page

                    // Créer un état graphique avec opacité
                    PdfGState gstate = new PdfGState();
                    gstate.setFillOpacity(0.9f); // 0.0 = transparent, 1.0 = opaque

                    PdfContentByte canvas = writer.getDirectContent(); // au-dessus du contenu
                    canvas.saveState();
                    canvas.setGState(gstate);
                    canvas.addImage(cachet);
                    canvas.restoreState(); // très important pour ne pas affecter les autres éléments
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            document.close();
            // Définir les entêtes pour le téléchargement du fichier
            response.setContentType("application/pdf");
            String headerKey ="Content-Disposition";
            String headerValue = "attachment; filename="+ nomF;
            response.setHeader(headerKey, headerValue);

            // Copier le contenu du fichier PDF dans la réponse HTTP
            response.getOutputStream().write(org.apache.commons.io.IOUtils.toByteArray(new java.io.FileInputStream(nomF)));
            response.getOutputStream().flush();
            response.getOutputStream().close();

            // Supprimer le fichier local après l'exportation
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(nomF));
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void genererBon(Commande commande, List<LigneCommande> lLC, HttpServletResponse response, String rap) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Document document = new Document();
        try {
            // Créer une police en gras
            Font policeGras = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font pg = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            String nomF= "BON"+commande.getId()+"_"+commande.getClient().getSte()+".pdf";
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(nomF));
            BonFooter event = new BonFooter();
            // Ajouter un événement pour gérer le pied de page
            writer.setPageEvent(event);
            document.open();
            //Image logo = Image.getInstance("classes/static/assets/images/logo/lgb.png");
            //Image logo = Image.getInstance("src/main/resources/static/images/lgb.png");
            Image logo = Image.getInstance(ResourceUtils.load("static/images/lgb.png"));
            logo.scaleToFit(200, 150); // Ajuster la taille du logo selon vos besoins
            logo.setAlignment(Element.ALIGN_LEFT);
            document.add(logo);
            //informations de l'entreprise
            float distanceHautBas = 10f;
            PdfPCell cellule = new PdfPCell(new Phrase(commande.getClient().getSte()+"\n\n"+commande.getClient().getAdresse(),policeGras));
            cellule.setBorder(Rectangle.BOX);
            cellule.setBorderColor(BaseColor.BLACK);
            cellule.setBorderWidth(1f);
            cellule.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellule.setPaddingTop(distanceHautBas);
            cellule.setPaddingBottom(distanceHautBas);
            PdfPTable tt = new PdfPTable(1);
            tt.setWidthPercentage(50);
            tt.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tt.addCell(cellule);
            document.add(tt);
            //informations de la facture
            //produits
            PdfPTable tab = new PdfPTable(3); // 5 colonnes
            tab.setWidthPercentage(100);
            tab.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            tab.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            tab.setSpacingBefore(20f);// Ajouter un espacement avant la table
            tab.setSpacingAfter(20f);
            tab.addCell(new Phrase("Bon de livraison nº : "+ commande.getId(),policeGras));
            tab.addCell(new Phrase("Date : "+ sdf.format(new Date()),policeGras));
            tab.addCell(new Phrase("Code Client : "+ commande.getClient().getId(), policeGras));
            document.add(tab);
            // Ajouter le tableau des détails de la facture
            PdfPTable table = new PdfPTable(5); // 5 colonnes
            PdfPCell cc = new PdfPCell(new Phrase("Quantité", pg));
            table.setWidthPercentage(100);
            table.setWidths(new float[]{20,45,10, 10, 15});
            table.setHorizontalAlignment(Element.ALIGN_LEFT);

            // Ajouter une cellule pour chaque détail de la facture
            table.addCell(new Phrase("Référence", pg));
            table.addCell(new Phrase("Désignation", pg));
            table.addCell(cc);
            table.addCell(new Phrase("Prix unitaire", pg));
            table.addCell(new Phrase("Montant HT",pg));

            // Ajouter des lignes de données (à remplacer par vos propres données)
            for (LigneCommande lc : lLC){
                table.addCell(lc.getRef());
                table.addCell(lc.getProductName());
                table.addCell(String.valueOf(lc.getQuantity()));
                table.addCell(String.format("%.2f",lc.getPrixVente()));
                table.addCell(String.format("%.2f", lc.getMontant()));
            }
            // Ajouter le tableau au document
            document.add(table);
            //les montants
            PdfPTable tm = new PdfPTable(1); // 5 colonnes
            tm.setWidthPercentage(25);
            tm.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tm.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            tm.getDefaultCell().setPaddingTop(5f);
            tm.getDefaultCell().setPaddingBottom(5f);
            tm.setSpacingBefore(20f);
            tm.setSpacingAfter(20f);
            //header
            tm.addCell(new Phrase("Montant HT", policeGras));
            //body
            tm.addCell(new Phrase(String.format("%.2f",commande.getMontantHT())+" DH", policeGras));
            document.add(tm);
            Font pgg = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            if (Objects.equals(rap, "on")) {

                double credit = servPaiement.calculerCreditClient(
                        commande.getClient()
                );

                if (credit > 0) {

                    PdfPTable crd = new PdfPTable(2);

                    crd.setHorizontalAlignment(Element.ALIGN_LEFT);
                    crd.setWidthPercentage(100);
                    crd.getDefaultCell()
                            .setBorder(Rectangle.NO_BORDER);
                    crd.getDefaultCell()
                            .setHorizontalAlignment(Element.ALIGN_LEFT);

                    crd.setSpacingAfter(10f);
                    crd.setWidths(new float[]{12, 88});

                    crd.addCell(
                            new Phrase("RAPPEL : ", pgg)
                    );

                    crd.addCell(
                            new Phrase(
                                    String.format("%.2f", credit)
                                            + " DHS RESTE À REGLER",
                                    pgg
                            )
                    );

                    document.add(crd);
                }
            }
            document.close();


            // Définir les entêtes pour le téléchargement du fichier
            response.setContentType("application/pdf");
            String headerKey ="Content-Disposition";
            String headerValue = "attachment; filename="+ nomF;
            response.setHeader(headerKey, headerValue);

            // Copier le contenu du fichier PDF dans la réponse HTTP
            response.getOutputStream().write(org.apache.commons.io.IOUtils.toByteArray(new java.io.FileInputStream(nomF)));
            response.getOutputStream().flush();
            response.getOutputStream().close();

            // Supprimer le fichier local après l'exportation
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(nomF));
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void simulerFacture(SimFacture simFacture, List<LigneSFacture> lSF, HttpServletResponse response){
        Document document = new Document();
        try {
            // Créer une police en gras
            Font policeGras = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font pg = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            String nomF= "FACTURE"+simFacture.getId()+"_"+simFacture.getClient().getSte()+".pdf";
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(nomF));
            PdfFooterEvent event = new PdfFooterEvent();
            // Ajouter un événement pour gérer le pied de page
            writer.setPageEvent(event);
            document.open();
            //Image logo = Image.getInstance("classes/static/images/lg.png");
            //Image logo = Image.getInstance("src/main/resources/static/images/lg.png");
            Image logo = Image.getInstance(ResourceUtils.load("static/images/lg.png"));
            logo.scaleToFit(500, 150); // Ajuster la taille du logo selon vos besoins
            logo.setAlignment(Element.ALIGN_LEFT);
            document.add(logo);
            //informations de l'entreprise
            float distanceHautBas = 10f;
            PdfPCell cellule = new PdfPCell(new Phrase("STE "+ simFacture.getClient().getSte()+"\n\nI.C.E : " + simFacture.getClient().getIce()+"\n\n"+simFacture.getClient().getAdresse(),policeGras));
            cellule.setBorder(Rectangle.BOX);
            cellule.setBorderColor(BaseColor.BLACK);
            cellule.setBorderWidth(1f);
            cellule.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellule.setPaddingTop(distanceHautBas);
            cellule.setPaddingBottom(distanceHautBas);
            PdfPTable tt = new PdfPTable(1);
            tt.setWidthPercentage(50);
            tt.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tt.addCell(cellule);
            document.add(tt);
            //informations de la facture
            //produits
            PdfPTable tab = new PdfPTable(3); // 5 colonnes
            tab.setWidthPercentage(100);
            tab.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            tab.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            tab.setSpacingBefore(20f);// Ajouter un espacement avant la table
            tab.setSpacingAfter(20f);
            tab.addCell(new Phrase("Facture nº : "+ simFacture.getId(),policeGras));
            tab.addCell(new Phrase("Date : "+ simFacture.getDate(),policeGras));
            tab.addCell(new Phrase("Code Client : "+ simFacture.getClient().getId(), policeGras));
            document.add(tab);
            // Ajouter le tableau des détails de la facture
            PdfPTable table = new PdfPTable(5); // 5 colonnes
            PdfPCell cc = new PdfPCell(new Phrase("Quantité", pg));
            table.setWidthPercentage(100);
            table.setWidths(new float[]{20,45,10, 10, 15});
            table.setHorizontalAlignment(Element.ALIGN_LEFT);

            // Ajouter une cellule pour chaque détail de la facture
            table.addCell(new Phrase("Référence", pg));
            table.addCell(new Phrase("Désignation", pg));
            table.addCell(cc);
            table.addCell(new Phrase("Prix unitaire", pg));
            table.addCell(new Phrase("Montant HT",pg));

            // Ajouter des lignes de données (à remplacer par vos propres données)
            for (LigneSFacture lc : lSF){
                table.addCell(lc.getRef());
                table.addCell(lc.getProductName());
                table.addCell(String.valueOf(lc.getQuantity()));
                table.addCell(String.format("%.2f",lc.getPrixVente()));
                table.addCell(String.format("%.2f", lc.getMontant()));
            }
            // Ajouter le tableau au document
            document.add(table);
            //les montants
            PdfPTable tm = new PdfPTable(3); // 5 colonnes
            tm.setWidthPercentage(100);
            tm.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            tm.getDefaultCell().setPaddingTop(5f);
            tm.getDefaultCell().setPaddingBottom(5f);
            tm.setSpacingBefore(20f);
            tm.setSpacingAfter(20f);
            //header
            tm.addCell(new Phrase("Montant total Hors taxes (HT)", policeGras));
            tm.addCell(new Phrase("Montant TVA (20%)", policeGras));
            tm.addCell(new Phrase("Montant TTC", policeGras));
            //body
            tm.addCell(String.format("%.2f",simFacture.getMontantHT())+" DH");
            tm.addCell(String.format("%.2f",simFacture.getmTVA())+" DH");
            tm.addCell(new Phrase(String.format("%.2f",simFacture.getMontantTTC())+" DH", policeGras));
            document.add(tm);
            //mode de paie
            PdfPTable mp = new PdfPTable(2);
            mp.setHorizontalAlignment(Element.ALIGN_LEFT);
            mp.setWidthPercentage(100);
            mp.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            mp.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            mp.setSpacingAfter(10f);
            mp.setWidths(new float[]{22,78});
            //heaed
            mp.addCell(new Phrase("Mode de Paiment : ", policeGras));
            mp.addCell(new Phrase(simFacture.getModPai()));
            document.add(mp);
            //mtl
            PdfPTable mtl = new PdfPTable(2);
            mtl.setHorizontalAlignment(Element.ALIGN_LEFT);
            mtl.setWidthPercentage(100);
            mtl.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            mtl.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            mtl.setSpacingAfter(20f);
            mtl.setWidths(new float[]{45,55});
            //head
            mtl.addCell(new Phrase("Arrêté la présente facture à la somme de : ", policeGras));
            mtl.addCell(new Phrase(simFacture.getMtl()));
            document.add(mtl);
            document.close();


            // Définir les entêtes pour le téléchargement du fichier
            response.setContentType("application/pdf");
            String headerKey ="Content-Disposition";
            String headerValue = "attachment; filename="+ nomF;
            response.setHeader(headerKey, headerValue);

            // Copier le contenu du fichier PDF dans la réponse HTTP
            response.getOutputStream().write(org.apache.commons.io.IOUtils.toByteArray(new java.io.FileInputStream(nomF)));
            response.getOutputStream().flush();
            response.getOutputStream().close();

            // Supprimer le fichier local après l'exportation
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(nomF));
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void loadProds(List<Produit> lP, HttpServletResponse response, String qt, String pr){
        Document document = new Document();
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        try {
            // Créer une police en gras
            Font pg = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            Font pd = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            String nomF= "ListeProduits_"+sdf.format(new Date())+".pdf";
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(nomF));
            PdfFooterProdsEvent event = new PdfFooterProdsEvent();
            writer.setPageEvent(event);
            document.open();
            Paragraph ph = new Paragraph("Liste des Produits", pd);
            ph.setAlignment(Element.ALIGN_CENTER);
            document.add(ph);
            int nbC=6;
            if ((pr != null && qt == null) || (pr == null && qt != null)){
                nbC=5;
            }else if(pr == null && qt == null){
                nbC=4;
            }
            // Ajouter le tableau des détails de la facture
            PdfPTable table = new PdfPTable(nbC); //colonnes tt
            table.setSpacingBefore(20f);
            PdfPCell cc = new PdfPCell(new Phrase("Qte", pg));//tt
            table.setWidthPercentage(100);
            if (nbC==5){
                table.setWidths(new float[]{20,15,35,10, 20});//tt
            } else if(nbC==4){
                table.setWidths(new float[]{20,20,35, 25});//tt
            }
            table.setHorizontalAlignment(Element.ALIGN_LEFT);

            // Ajouter une cellule pour chaque détail de la facture
            table.addCell(new Phrase("Image", pg));//tt
            table.addCell(new Phrase("Référence", pg));
            table.addCell(new Phrase("Désignation", pg));
            if (nbC != 4 && qt != null) {
                table.addCell(cc);
            }
            if (nbC != 4 && pr != null) {
                table.addCell(new Phrase("Prix unitaire", pg));
            }
            table.addCell(new Phrase("Catégorie", pg));

            // Ajouter des lignes de données (à remplacer par vos propres données)
            for (Produit p : lP){
                Image image = Image.getInstance(p.getPic().getData());
                image.scaleToFit(20, 20);
                table.addCell(image);
                table.addCell(p.getRef());
                table.addCell(p.getName());
                if (nbC != 4 && qt != null) {
                    table.addCell(String.valueOf(p.getQuantite()));
                }
                if (nbC != 4 && pr != null) {
                    table.addCell(String.format("%.2f",p.getPrixVente()));
                }
                table.addCell(p.getCategorie().getLib());
            }
            // Ajouter le tableau au document
            document.add(table);
            //les montants
            document.close();


            // Définir les entêtes pour le téléchargement du fichier
            response.setContentType("application/pdf");
            String headerKey ="Content-Disposition";
            String headerValue = "attachment; filename="+ nomF;
            response.setHeader(headerKey, headerValue);

            // Copier le contenu du fichier PDF dans la réponse HTTP
            response.getOutputStream().write(org.apache.commons.io.IOUtils.toByteArray(new java.io.FileInputStream(nomF)));
            response.getOutputStream().flush();
            response.getOutputStream().close();

            // Supprimer le fichier local après l'exportation
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(nomF));
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
    }

    public Facture creeFacture(Paiement paiement) {
        if (paiement == null) {
            throw new IllegalArgumentException("Le paiement est obligatoire.");
        }
        if (paiement.getFacture() != null) {
            throw new IllegalStateException("Une facture existe déjà pour ce paiement.");
        }
        if (paiement.getMontant() <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit être supérieur à zéro.");
        }
        Facture facture = new Facture();
        facture.setNumero(numeroDocumentService.genererNumero("FACT"));

        facture.setDate(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

        /*Le montant du paiement est considéré comme TTC.*/
        double montantTTC = paiement.getMontant();

        double montantHT = montantTTC / 1.20;
        double montantTVA = montantTTC - montantHT;

        facture.setMontantTTC(montantTTC);
        facture.setMontantHT(montantHT);
        facture.setmTVA(montantTVA);

        facture.setMtl(NombreEnLettres.convertir((int) montantTTC) + " dirhams");
        /*Facture = côté propriétaire de la relation.*/
        facture.setPaiement(paiement);

        /*Synchronisation du côté inverse de l'entité.*/
        paiement.setFacture(facture);

        return repFacture.save(facture);
    }

    public void saveLF(LigneFacture lf) {
        this.repLigneFac.save(lf);
    }

    public Facture saveFac(Facture facture){
        return this.repFacture.save(facture);
    }

    public Facture getFactureByPaiement(Paiement paiement) {
        if (paiement == null) {
            return null;
        }

        return repFacture.findByPaiement(paiement).orElse(null);
    }

    private PdfPCell createHeaderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY); // couleur fond gris
        cell.setHorizontalAlignment(Element.ALIGN_CENTER); // centrer le texte
        cell.setPadding(5); // marges internes
        return cell;
    }

    @Transactional
    public List<LigneFacture> persFactItems(Facture facture, Commande commande) {

        if (facture == null || commande == null) {
            throw new IllegalArgumentException("La facture et la commande sont obligatoires.");
        }
        if (!Double.isFinite(facture.getMontantHT()) || facture.getMontantHT() <= 0) {
            throw new IllegalArgumentException("Le montant HT doit être positif et valide.");
        }
        long montantCible = BigDecimal
                .valueOf(facture.getMontantHT())
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();

        final int MAX_ETATS = 200_000;

        List<LigneCommande> lignesCommande = repLigneCommand.findAllByCommande(commande);

        if (lignesCommande == null || lignesCommande.isEmpty()) {
            throw new IllegalStateException("La commande ne contient aucune ligne.");
        }

        /*
         * ETAPE 1 :
         * Chercher le montant maximal atteignable avec
         * les produits et quantités de la commande,
         * sans dépasser le montant cible.
         */
        Map<Long, SelectionFacture> etatsCommande = new HashMap<>();
        etatsCommande.put(0L, null);

        for (int i = 0; i < lignesCommande.size(); i++) {
            LigneCommande ligne = lignesCommande.get(i);
            if (ligne.getQuantity() <= 0 || !Double.isFinite(ligne.getPrixVente()) || ligne.getPrixVente() <= 0) {
                continue;
            }

            long prix = BigDecimal
                    .valueOf(ligne.getPrixVente())
                    .setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .longValueExact();

            if (prix <= 0 || prix > montantCible) {
                continue;
            }

            for (int unite = 0; unite < ligne.getQuantity(); unite++) {

                List<Map.Entry<Long, SelectionFacture>> existants = new ArrayList<>(etatsCommande.entrySet());

                for (Map.Entry<Long, SelectionFacture> etat : existants) {

                    long nouveauMontant = etat.getKey() + prix;

                    if (nouveauMontant > montantCible) {
                        continue;
                    }

                    if (!etatsCommande.containsKey(nouveauMontant)) {
                        etatsCommande.put(
                                nouveauMontant,
                                new SelectionFacture(
                                        etat.getValue(), i
                                )
                        );
                    }
                }

                if (etatsCommande.size() > MAX_ETATS) {
                    throw new IllegalStateException(
                            "La recherche des produits de la commande "
                                    + "est trop complexe."
                    );
                }
            }
        }

        /*
         * Retenir le montant le plus élevé possible.
         */
        long montantCommande = etatsCommande.keySet()
                .stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        if (montantCommande <= 0) {
            throw new IllegalStateException(
                    "Aucun produit de la commande ne peut être "
                            + "retenu pour cette facture."
            );
        }

        Map<Integer, Integer> quantitesRetenues =
                new HashMap<>();

        SelectionFacture selection =
                etatsCommande.get(montantCommande);

        while (selection != null) {
            quantitesRetenues.merge(
                    selection.indexLigne,
                    1,
                    Integer::sum
            );

            selection = selection.precedent;
        }

        /*
         * ETAPE 2 :
         * Calculer le reste à compléter.
         */
        long reste = montantCible - montantCommande;

        Map<Integer, Integer> quantitesComplementaires =
                new HashMap<>();

        List<Produit> produitsComplementaires =
                new ArrayList<>();

        if (reste > 0) {

            /*
             * Le repository filtre par prix.
             * Le stock et le prix exact en centimes sont
             * également vérifiés ci-dessous.
             */
            produitsComplementaires =
                    repProduit.findByPrixVenteLessThanEqual(
                            reste / 100.0
                    );

            Map<Long, SelectionFacture> etatsComplement =
                    new HashMap<>();

            etatsComplement.put(0L, null);

            for (int i = 0;
                 i < produitsComplementaires.size();
                 i++) {

                Produit produit = produitsComplementaires.get(i);

                if (produit.getQuantite() <= 0
                        || !Double.isFinite(produit.getPrixVente())
                        || produit.getPrixVente() <= 0) {
                    continue;
                }

                long prix = BigDecimal
                        .valueOf(produit.getPrixVente())
                        .setScale(2, RoundingMode.HALF_UP)
                        .movePointRight(2)
                        .longValueExact();

                if (prix <= 0 || prix > reste) {
                    continue;
                }

                /*
                 * Chaque unité disponible est utilisable
                 * au maximum une fois dans cette recherche.
                 * Aucun stock n'est modifié ici.
                 */
                for (int unite = 0;
                     unite < produit.getQuantite();
                     unite++) {

                    List<Map.Entry<Long, SelectionFacture>> existants =
                            new ArrayList<>(etatsComplement.entrySet());

                    for (Map.Entry<Long, SelectionFacture> etat
                            : existants) {

                        long nouveauMontant =
                                etat.getKey() + prix;

                        if (nouveauMontant > reste) {
                            continue;
                        }

                        if (!etatsComplement.containsKey(nouveauMontant)) {
                            etatsComplement.put(
                                    nouveauMontant,
                                    new SelectionFacture(
                                            etat.getValue(), i
                                    )
                            );
                        }
                    }

                    if (etatsComplement.size() > MAX_ETATS) {
                        throw new IllegalStateException(
                                "La recherche des produits "
                                        + "complémentaires est trop complexe."
                        );
                    }

                    if (etatsComplement.containsKey(reste)) {
                        break;
                    }
                }

                if (etatsComplement.containsKey(reste)) {
                    break;
                }
            }

            /*
             * Le reste doit être couvert exactement.
             */
            if (!etatsComplement.containsKey(reste)) {
                throw new IllegalStateException(
                        "Impossible de compléter exactement le montant "
                                + "de la facture avec les produits "
                                + "complémentaires disponibles. "
                                + "Reste à compléter : "
                                + BigDecimal.valueOf(reste, 2)
                                + " DH."
                );
            }

            selection = etatsComplement.get(reste);

            while (selection != null) {
                quantitesComplementaires.merge(
                        selection.indexLigne,
                        1,
                        Integer::sum
                );

                selection = selection.precedent;
            }
        }

        /*
         * ETAPE 3 :
         * Construire les lignes issues de la commande.
         */
        List<LigneFacture> lignesFacture =
                new ArrayList<>();

        for (Map.Entry<Integer, Integer> entree
                : quantitesRetenues.entrySet()) {

            LigneCommande source =
                    lignesCommande.get(entree.getKey());

            int quantite = entree.getValue();

            LigneFacture ligne = new LigneFacture();

            ligne.setFacture(facture);
            ligne.setProductId(source.getProductId());
            ligne.setRef(source.getRef());
            ligne.setProductName(source.getProductName());
            ligne.setPrixVente(source.getPrixVente());
            ligne.setQuantity(quantite);

            long prixCentimes = BigDecimal
                    .valueOf(source.getPrixVente())
                    .setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .longValueExact();

            ligne.setMontant(
                    BigDecimal.valueOf(prixCentimes * quantite, 2)
                            .doubleValue()
            );

            lignesFacture.add(ligne);
        }

        /*
         * ETAPE 4 :
         * Construire les lignes complémentaires.
         *
         * Elles figurent uniquement sur la facture.
         * Aucun changement n'est effectué sur Commande,
         * LigneCommande ou Produit.quantite.
         */
        for (Map.Entry<Integer, Integer> entree
                : quantitesComplementaires.entrySet()) {

            Produit source =
                    produitsComplementaires.get(entree.getKey());

            int quantite = entree.getValue();

            LigneFacture ligne = new LigneFacture();

            ligne.setFacture(facture);
            ligne.setProductId(source.getId());
            ligne.setRef(source.getRef());
            ligne.setProductName(source.getName());
            ligne.setPrixVente(source.getPrixVente());
            ligne.setQuantity(quantite);

            long prixCentimes = BigDecimal
                    .valueOf(source.getPrixVente())
                    .setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .longValueExact();

            ligne.setMontant(
                    BigDecimal.valueOf(prixCentimes * quantite, 2)
                            .doubleValue()
            );

            lignesFacture.add(ligne);
        }

        /*
         * ETAPE 5 :
         * Vérifier le total avant toute persistance.
         */
        long totalVerifie = 0L;

        for (LigneFacture ligne : lignesFacture) {

            long montantLigne = BigDecimal
                    .valueOf(ligne.getMontant())
                    .setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .longValueExact();

            totalVerifie = Math.addExact(
                    totalVerifie, montantLigne
            );
        }

        if (totalVerifie != montantCible) {
            throw new IllegalStateException(
                    "Le total des lignes ne correspond pas "
                            + "au montant HT de la facture."
            );
        }

        facture.setMtl(
                convertirMontantEnLettres(
                        facture.getMontantTTC()
                )
        );

        saveFac(facture);

        for (LigneFacture ligne : lignesFacture) {
            saveLF(ligne);
        }

        return lignesFacture;
    }

    private static class SelectionFacture {

        private final SelectionFacture precedent;
        private final int indexLigne;

        private SelectionFacture(
                SelectionFacture precedent,
                int indexLigne) {

            this.precedent = precedent;
            this.indexLigne = indexLigne;
        }
    }

    private double ajouterDepuisCommande(List<LigneCommande> lignesCommande, List<LigneFacture> lignesFacture, Facture facture, double montantHT) {

        double montantCompose = 0;

        int iteration = 0;

        while (montantCompose < montantHT && iteration < 500) {

            for (LigneCommande ligneCommande : lignesCommande) {

                if (montantCompose >= montantHT) {
                    break;
                }

                double prix = ligneCommande.getPrixVente();

                if (prix <= 0) {
                    continue;
                }

                if (montantCompose + prix <= montantHT) {

                    ajouterOuIncrementerLigneFacture(
                            lignesFacture,
                            facture,
                            ligneCommande.getProductId(),
                            ligneCommande.getRef(),
                            ligneCommande.getProductName(),
                            ligneCommande.getPrixVente()
                    );

                    montantCompose += prix;
                }
            }

            iteration++;
        }

        return montantCompose;
    }

    private double ajouterProduitsComplementaires(
            List<Produit> produits,
            List<LigneFacture> lignesFacture,
            Facture facture,
            double montantCompose,
            double montantHT) {

        int iteration = 0;

        while (montantCompose < montantHT && iteration < 1000) {

            for (Produit produit : produits) {

                if (montantCompose >= montantHT) {
                    break;
                }

                double prix = produit.getPrixVente();

                if (prix <= 0) {
                    continue;
                }

                if (montantCompose + prix <= montantHT) {

                    ajouterOuIncrementerLigneFacture(
                            lignesFacture,
                            facture,
                            produit.getId(),
                            produit.getRef(),
                            produit.getName(),
                            prix
                    );

                    montantCompose += prix;
                }
            }

            iteration++;
        }

        return montantCompose;
    }

    private void ajouterOuIncrementerLigneFacture(
            List<LigneFacture> lignesFacture,
            Facture facture,
            long productId,
            String ref,
            String productName,
            double prixVente) {

        for (LigneFacture ligne : lignesFacture) {

            if (ligne.getProductId() == productId) {

                ligne.setQuantity(ligne.getQuantity() + 1);

                ligne.setMontant(
                        ligne.getPrixVente()
                                * ligne.getQuantity()
                );

                return;
            }
        }

        LigneFacture nouvelleLigne = new LigneFacture();

        nouvelleLigne.setFacture(facture);
        nouvelleLigne.setProductId(productId);
        nouvelleLigne.setRef(ref);
        nouvelleLigne.setProductName(productName);
        nouvelleLigne.setPrixVente(prixVente);
        nouvelleLigne.setQuantity(1);
        nouvelleLigne.setMontant(prixVente);

        lignesFacture.add(nouvelleLigne);
    }

    private String convertirMontantEnLettres(double montant) {

        int dirhams = (int) montant;

        int centimes = (int) Math.round(
                (montant - dirhams) * 100
        );

        String resultat =
                NombreEnLettres.convertir(dirhams) + " dirhams";

        if (centimes > 0) {
            resultat += " et "
                    + NombreEnLettres.convertir(centimes)
                    + " centimes";
        }

        return resultat;
    }
}
