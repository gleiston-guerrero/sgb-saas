package com.uteq.backend.repository.projection;

public interface ReportInventoryProjection {

    /**
     * Identificador del libro.
     *
     * @return identificador persistente del libro.
     */
    Long getBookId();

    /**
     * Título del libro.
     *
     * @return título del inventario.
     */
    String getTitle();

    /**
     * ISBN del libro.
     *
     * @return ISBN del libro.
     */
    String getIsbn();

    /**
     * Nombres de autores concatenados.
     *
     * @return autores asociados al libro.
     */
    String getAuthorName();

    /**
     * Nombres de categorías concatenadas.
     *
     * @return categorías asociadas al libro.
     */
    String getCategoryName();

    /**
     * Nombre de la editorial.
     *
     * @return editorial del libro.
     */
    String getPublisherName();

    /**
     * Nombre del proveedor.
     *
     * @return proveedor del libro.
     */
    String getSupplierName();

    /**
     * Nombre del idioma.
     *
     * @return idioma catalogado para el libro.
     */
    String getLanguageName();

    /**
     * Nombre del estado del libro.
     *
     * @return estado actual del libro.
     */
    String getStatusBookName();

    /**
     * Año de publicación.
     *
     * @return año bibliográfico del libro.
     */
    Short getYearPublication();

    /**
     * Ubicación física en estantería.
     *
     * @return referencia de la ubicación física.
     */
    String getLocationPhysical();

    /**
     * Ejemplares totales.
     *
     * @return número total de ejemplares.
     */
    Short getStockTotal();

    /**
     * Ejemplares disponibles.
     *
     * @return número de ejemplares disponibles.
     */
    Short getStockAvailable();

    /**
     * Disponibilidad calculada (Agotado/Baja disponibilidad/Disponible).
     *
     * @return etiqueta de disponibilidad calculada.
     */
    String getStatusAvailability();
}
