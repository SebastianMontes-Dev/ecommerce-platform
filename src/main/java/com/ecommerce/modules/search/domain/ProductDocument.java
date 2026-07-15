package com.ecommerce.modules.search.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductDocument {

    private String id;
    private UUID idTienda;
    private String nombre;
    private String descripcion;
    private String slug;
    private BigDecimal precio;
    private String nombreCategoria;
    private String urlImagen;
    private double calificacionPromedio;
    private long conteoResenas;

    public ProductDocument() {}

    public ProductDocument(UUID idProducto, UUID idTienda, String nombre, String descripcion,
                           String slug, BigDecimal precio, String nombreCategoria) {
        this.id = idProducto.toString();
        this.idTienda = idTienda;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.slug = slug;
        this.precio = precio;
        this.nombreCategoria = nombreCategoria;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public UUID getIdTienda() { return idTienda; }
    public void setIdTienda(UUID idTienda) { this.idTienda = idTienda; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }
    public String getUrlImagen() { return urlImagen; }
    public void setUrlImagen(String urlImagen) { this.urlImagen = urlImagen; }
    public double getCalificacionPromedio() { return calificacionPromedio; }
    public void setCalificacionPromedio(double calificacionPromedio) { this.calificacionPromedio = calificacionPromedio; }
    public long getConteoResenas() { return conteoResenas; }
    public void setConteoResenas(long conteoResenas) { this.conteoResenas = conteoResenas; }
}
