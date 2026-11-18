package com.gharnata.controller;

import com.gharnata.entity.Categorie;
import com.gharnata.entity.Produit;
import com.gharnata.entity.dto.ProductDTO;
import com.gharnata.repository.RepImage;
import com.gharnata.service.*;
import com.gharnata.storage.StorageService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.gharnata.entity.Image;
import com.gharnata.storage.StorageException;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/products")
public class ProduitController {

    private final ServProduit servProduit;
    private final ServImage servImage;
    private final ServNotification servNotification;
    private final ServPanier servPanier;
    private final RepImage repImage;
    private final StorageService storageService;
    private final ServParametre servParametre;

    public ProduitController(ServProduit servProduit, ServImage servImage, ServNotification servNotification,
                             ServPanier servPanier, RepImage repImage, StorageService storageService,
                             ServParametre servParametre) {
        this.servProduit = servProduit;
        this.servImage = servImage;
        this.servNotification = servNotification;
        this.servPanier = servPanier;
        this.repImage = repImage;
        this.storageService = storageService;
        this.servParametre = servParametre;
    }

    @GetMapping("/images/{id}")
    @ResponseBody
    public ResponseEntity<Resource> afficherImage(@PathVariable Long id) {

        Image image = repImage.findById(id).orElse(null);

        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource;

            // Nouvelle méthode : stockage physique
            if (image.getCheminFichier() != null
                    && !image.getCheminFichier().isBlank()) {

                resource = storageService.load(
                        image.getCheminFichier()
                );

            } else {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = MediaType.parseMediaType(
                    image.getTypeMime() != null
                            ? image.getTypeMime()
                            : MediaType.APPLICATION_OCTET_STREAM_VALUE
            );

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.noCache().cachePrivate())
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"image\""
                    )
                    .body(resource);

        } catch (StorageException | IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/add")
    public String addproduit(Model model){
        model.addAttribute("cats", this.servProduit.getAllCats());
        model.addAttribute("ancP", new Produit());
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Produits et Stock",
                "Ajouter un produit"
        ));
        return "gest/addProduct";
    }
    @GetMapping("/check-ref")
    public String checkRef(@RequestParam String ref, @RequestParam(required = false) Long excludeId, Model model) {
        Produit p = servProduit.getProductByRef(ref);
        boolean existe = (p != null) && (excludeId == null || !p.getId().equals(excludeId));
        model.addAttribute("existe", existe);
        model.addAttribute("refVide", ref == null || ref.isBlank());
        model.addAttribute("produit", existe ? p.getName() : null);
        return "fragments/ref-feedback :: feedback";
    }
    //for modification page
    @PostMapping("/add")
    public String addproduit(@ModelAttribute Produit produit,int idCat, Model model, RedirectAttributes attributes, MultipartFile imagee) throws Exception {
        produit.setCategorie(this.servProduit.getCatById(idCat));
        Produit p = this.servProduit.getProductByRef(produit.getRef());
        model.addAttribute("notif",servNotification.nbNotif());
        if(p!= null){
            model.addAttribute("error", "Le produit avec cette référence existe déjà.");
            model.addAttribute("ancP",produit);
            model.addAttribute("cats", this.servProduit.getAllCats());
            return "gest/addProduct";
        }
        produit.setPic(this.servImage.treatPic(imagee));
        if (this.servProduit.addProduit(produit) == null){
            model.addAttribute("error", "Erreur d'ajout du produit");
            model.addAttribute("cats", this.servProduit.getAllCats());
            return "gest/addProduct";
        }
        attributes.addFlashAttribute("success", "Produit ajouté avec succès.");
        return "redirect:/products/add";
    }
    @GetMapping
    public String showProducts(@RequestParam(required = false) Long productId, Model model){
        List<Produit> lP;
        if (productId != null) {
            Produit produit = this.servProduit.getProductById(productId);
            if (produit == null) {
                model.addAttribute("error", "Ce produit n'existe pas.");
                lP = this.servProduit.getProductsLimit();
            } else {
                lP = List.of(produit);
            }
        } else {
            lP = this.servProduit.getProductsLimit();
        }
        model.addAttribute("products", lP);
        model.addAttribute("prods", this.servProduit.getProductDTOs());
        model.addAttribute("notif", servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Produits et Stock",
                "Galerie des produits"
        ));
        return "gest/showProducts";
    }

    /*@GetMapping("/stock")
    public String showProdSt(Model model, @RequestParam(value = "ref", required = false) String ref){
        List<Produit> lP = new ArrayList<>();
        if (ref != null) {
            Produit produit = servProduit.getProductByRef(ref);
            lP.add(produit);
        }else{
            lP = this.servProduit.getProducts();
        }
        List<Integer> indexs =new ArrayList<>();
        for (int i=1; i<=lP.size();i++){
            indexs.add(i);
        }
        model.addAttribute("products", lP);
        model.addAttribute("prods",this.servProduit.getProducts());
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        model.addAttribute("idxs", indexs);
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Produits et Stock",
                "Page de stock"
        ));
        return "gest/showProdStock";
    }*/

    /*@GetMapping("/stock")
    public String showProdSt(
            Model model,
            @RequestParam(value = "ref", required = false) String ref,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Limiter les tailles de page autorisées
        if (size != 20 && size != 40 && size != 80) {
            size = 20;
        }

        // Éviter les numéros de page négatifs
        if (page < 0) {
            page = 0;
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<Produit> produitsPage =
                servProduit.getStockPage(ref, pageable);

        // Numérotation continue entre les pages
        List<Integer> indexs = new ArrayList<>();

        int debut = page * size + 1;

        for (int i = 0; i < produitsPage.getNumberOfElements(); i++) {
            indexs.add(debut + i);
        }

        model.addAttribute("products", produitsPage.getContent());
        model.addAttribute("idxs", indexs);

        // La liste complète n'est plus nécessaire pour le datalist
        model.addAttribute("prods", List.of());

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", produitsPage.getTotalPages());
        model.addAttribute("totalElements", produitsPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", ref);

        model.addAttribute("notif", servNotification.nbNotif());
        model.addAttribute("cats", servProduit.getAllCats());

        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Produits et Stock",
                "Page de stock"
        ));

        return "gest/showProdStock";
    }*/

    @GetMapping("/stock")
    public String showProdSt(
            Model model,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page) {

        // Taille de page définie dans les paramètres globaux
        int size = servParametre.getProduitsParPage();

        page = Math.max(0, page);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<Produit> produitsPage;

        // Recherche par sélection dans l'autocomplétion
        if (productId != null) {

            Produit produit = servProduit.getProductById(productId);

            if (produit == null) {
                model.addAttribute("error", "Ce produit n'existe pas.");
                produitsPage = new PageImpl<>(List.of(), pageable, 0);
            } else {
                produitsPage = new PageImpl<>(
                        List.of(produit),
                        pageable,
                        1
                );
            }

        } else {
            // Recherche libre par nom ou référence
            String recherche = (query == null || query.isBlank())
                    ? null
                    : query.trim();

            produitsPage = servProduit.getStockPage(recherche, pageable);
        }

        // Éviter une page inexistante
        if (produitsPage.getTotalPages() > 0
                && page >= produitsPage.getTotalPages()) {

            page = produitsPage.getTotalPages() - 1;

            pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(Sort.Direction.DESC, "id")
            );

            if (productId == null) {
                produitsPage = servProduit.getStockPage(
                        query == null || query.isBlank() ? null : query.trim(),
                        pageable
                );
            }
        }

        List<Integer> indexs = new ArrayList<>();
        int debut = page * size + 1;

        for (int i = 0; i < produitsPage.getNumberOfElements(); i++) {
            indexs.add(debut + i);
        }

        model.addAttribute("products", produitsPage.getContent());
        model.addAttribute("idxs", indexs);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", produitsPage.getTotalPages());
        model.addAttribute("totalElements", produitsPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("productId", productId);
        model.addAttribute("query", query);
        model.addAttribute("notif", servNotification.nbNotif());
        model.addAttribute("cats", servProduit.getAllCats());

        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Produits et Stock",
                "Page de stock"
        ));

        return "gest/showProdStock";
    }

    @PostMapping("/stock")
    public String showProdSt(@RequestParam("ref") String ref, RedirectAttributes attributes){
        attributes.addAttribute("ref", ref);
        return "redirect:/products/stock";
    }

    @GetMapping("/{id}/details")
    public String showOneProduct(@PathVariable Long id, Model model){
        Produit p = this.servProduit.getProductById(id);
        model.addAttribute("product", p);
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Produits et Stock",
                p.getName()
        ));
        return "gest/productDetails";
    }
    @GetMapping("/api/search")
    @ResponseBody
    public List<ProductDTO> searchProducts(
            @RequestParam("q") String query) {

        return servProduit.searchProducts(query);
    }
    @GetMapping("/modify")
    public String rechercherProduit(@RequestParam(required = false) Long productId, Model model) {
        model.addAttribute("notif", servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        // Tu n'en as plus besoin pour le datalist
        // mais tu peux le garder si cette liste est utilisée ailleurs
        model.addAttribute("prods", this.servProduit.getProductDTOs());
        if (productId != null) {
            Produit produit = this.servProduit.getProductById(productId);
            if (produit == null) {
                model.addAttribute("error", "Ce produit n'existe pas.");
                model.addAttribute("product", new Produit());
            } else {
                model.addAttribute("product", produit);
            }
        } else {
            model.addAttribute("product", new Produit());
        }
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Produits et Stock",
                "Modifier un produit"
        ));
        return "gest/modifyProduct";
    }

    @PostMapping("/{id}/modify")
    public String modifierProduit(@PathVariable long id,
                                  @RequestParam String ref,
                                  @RequestParam int idCat,
                                  @RequestParam String name,
                                  @RequestParam BigDecimal prixVente,
                                  @RequestParam(defaultValue = "0") int quantite,
                                  @RequestParam MultipartFile imagee,
                                  RedirectAttributes attributes) {

        if (prixVente == null || prixVente.compareTo(BigDecimal.ZERO) <= 0) {
            attributes.addFlashAttribute("error", "Le prix ne peut pas être négatifs ou null.");
            return "redirect:/products/modify?ref=" + ref;
        }

        Produit p = this.servProduit.getProductById(id);
        if (p == null) {
            attributes.addFlashAttribute("error", "Produit introuvable.");
            return "redirect:/products/modify";
        }

        Categorie cat = this.servProduit.getCatById(idCat);
        if (cat == null) {
            attributes.addFlashAttribute("error", "Catégorie invalide.");
            return "redirect:/products/modify?ref=" + p.getRef();
        }

        p.setQuantite(p.getQuantite() + quantite);
        p.setName(name);
        p.setRef(ref);
        p.setPrixVente(prixVente);
        p.setCategorie(cat);
        Produit pt = this.servProduit.addProduit(p);

        if (pt == null) {
            attributes.addFlashAttribute("error", "Un problème est survenu lors de la modification.");
            return "redirect:/products/modify";
        }

        if (!imagee.isEmpty()) {
            if (pt.getPic() != null) {
                this.servImage.modifyPic(imagee, pt.getPic().getId());
            } else {
                try {
                    pt.setPic(this.servImage.treatPic(imagee));
                } catch (Exception e) {
                    attributes.addFlashAttribute("error", "Un problème est survenu lors de la modification de l'image.");
                    return "redirect:/products/modify";
                }
                this.servProduit.addProduit(pt);
            }
        }

        attributes.addFlashAttribute("success", "Produit bien modifié");
        if (pt.getQuantite() > 5) {
            this.servNotification.deletEnr(pt);
        }
        return "redirect:/products/modify";
    }

    @GetMapping("/{id}/delete/{page}")
    public String deletProduct(@PathVariable Long id, RedirectAttributes attributes, @PathVariable String page){
        Produit produit = this.servProduit.getProductById(id);
        this.servPanier.checkProductExi(produit);
        this.servNotification.checkProductExi(produit);
        this.servProduit.deletePrById(id);
        if(page.equals("list")){
            return "redirect:/products/stock";
        }
        return "redirect:/products";
    }
    @PostMapping("/delete-products")
    public String supprimerProduits(@RequestParam("prod_ids") List<Long> ids) {
        List<Produit> lp = this.servProduit.getAllByIds(ids);
        this.servPanier.checkProductsExi(lp);
        this.servNotification.checkProductsExi(lp);
        servProduit.deleteAllById(ids);
        return "redirect:/products";
    }
    @PostMapping("/add-categorie")
    public String addCat(@RequestParam String cat,
                         @RequestParam(defaultValue = "/products/add") String redirectTo,
                         @RequestHeader(value = "HX-Request", required = false) String hxRequest,
                         Model model) {
        this.servProduit.addCat(cat);
        if ("true".equals(hxRequest)) {
            model.addAttribute("cats", this.servProduit.getAllCats());
            return "fragments/cat-select :: catSelect";
        }
        return "redirect:" + redirectTo;
    }


}
