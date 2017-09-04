package com.metalsa.spx.search;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Created by IntelliJ Idea
 *
 * @author ruben.bresler
 */
@Data
public class Pojo {
    protected Long id;

    protected String codigo;

    protected String descripcion;

    protected String proveedor;

    protected String color;

    protected Double precio;

    protected String nombreUen;

    protected Integer tiempoEntrega;

    protected LocalDateTime fechaCreacion;
}
