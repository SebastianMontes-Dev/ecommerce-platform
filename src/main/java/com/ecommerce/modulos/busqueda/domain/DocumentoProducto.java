package com.ecommerce.modulos.busqueda.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class DocumentoProducto {

    private String id;
    private UUID idTienda;
    private String nombre;
    private String descripcion;
    private String enlaceCorto;
    private BigDecimal precio;
    private String nombreCategoria;
    private String urlImagen;
    // Ningún código del repo escribe estos dos campos todavía (ni al crear/actualizar un
    // producto ni al crear una reseña) — siempre quedan en su default (0.0 / 0). El filtro
    // minRating y el sort=rating de ServicioBusqueda.busqueda se descoparon por esto mismo;
    // ver el javadoc de ese método para el porqué y el plan de retomarlo.
    private double calificacionPromedio;
    private long conteoResenas;

    public DocumentoProducto() {}

    public DocumentoProducto(UUID idProducto, UUID idTienda, String nombre, String descripcion,
                           String enlaceCorto, BigDecimal precio, String nombreCategoria) {
        this.id = idProducto.toString();
        this.idTienda = idTienda;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.enlaceCorto = enlaceCorto;
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
    public String getEnlaceCorto() { return enlaceCorto; }
    public void setEnlaceCorto(String enlaceCorto) { this.enlaceCorto = enlaceCorto; }
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
