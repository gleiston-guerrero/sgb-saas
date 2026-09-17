package com.uteq.backend.repository.projection;

public interface ReportInventoryProjection {

    /** Identificador del libro. */
    Long getBookId();

    /** Título del libro. */
    String getTitle();

    /** ISBN del libro. */
    String getIsbn();

    /** Nombres de autores concatenados. */
    String getAuthorName();

    /** Nombres de categorías concatenadas. */
    String getCategoryName();

    /** Nombre de la editorial. */
    String getPublisherName();

    /** Nombre del proveedor. */
    String getSupplierName();

    /** Nombre del idioma. */
    String getLanguageName();

    /** Nombre del estado del libro. */
    String getStatusBookName();

    /** Año de publicación. */
    Short getYearPublication();

    /** Ubicación física en estantería. */
    String getLocationPhysical();

    /** Ejemplares totales. */
    Short getStockTotal();

    /** Ejemplares disponibles. */
    Short getStockAvailable();

    /** Disponibilidad calculada (Agotado/Baja disponibilidad/Disponible). */
    String getStatusAvailability();
}
