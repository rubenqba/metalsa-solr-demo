package com.github.rubenqba.solr.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Created by IntelliJ Idea
 *
 * @author ruben.bresler
 */
@Data
@Entity(name = "nvc_v_items_all")
public class DataItem implements Serializable {
    @Id
    @Column(name = "iditem")
    protected Long id;

    @Column(name = "codigoitem")
    protected String codigo;

    protected String descripcion;

    @Column(name = "nombreproveedor")
    protected String proveedor;

    protected String color;

    protected Double precio;

    @Column(name = "nombreuen")
    protected String nombreUen;

    @Column(name = "tiempoentrega")
    protected Integer tiempoEntrega;

    @Column(name = "fechacreacion")
    protected LocalDateTime fechaCreacion;
}
