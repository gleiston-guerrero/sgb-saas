package com.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Objects;

// equals()/hashCode()/toString() explícitos: el record compararía el
// campo byte[] por referencia; con Arrays se compara por contenido.
// No usar esta clase como clave en HashMap ni como elemento de HashSet.
public record EvidenceDamageFileDTO( @JsonProperty("archivoTipo") String fileType, @JsonProperty("archivoBytes") byte[] fileBytes
) {
    /**
     * Compara por contenido del binario (no por referencia del arreglo).
     *
     * @param o otro objeto a comparar
     * @return true si es la misma evidencia (tipo y bytes iguales)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvidenceDamageFileDTO otro)) return false;
        return Objects.equals(fileType, otro.fileType) && Arrays.equals(fileBytes, otro.fileBytes);
    }
    /**
     * Hash coherente con {@link #equals(Object)} (contenido del binario).
     *
     * @return hash combinado de tipo y bytes
     */
    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(fileType) + Arrays.hashCode(fileBytes);
    }
    /**
     * Representación sin volcar el binario completo.
     *
     * @return tipo y resumen de los bytes
     */
    @Override
    public String toString() {
        return "EvidenciaDanoArchivoDTO[archivoTipo=" + fileType + ", archivoBytes=" + Arrays.toString(fileBytes) + "]";
    }
}
