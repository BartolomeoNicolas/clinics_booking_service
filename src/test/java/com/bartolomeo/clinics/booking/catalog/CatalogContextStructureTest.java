package com.bartolomeo.clinics.booking.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

/**
 * Hace ejecutable la prohibición más importante de este contexto: el catálogo no se replica.
 *
 * <p>El enunciado es tajante —el servicio de turnos debe obtener la información vigente del
 * catálogo y no debe guardar copia propia (PLAN H4, enunciado §4.2)— y una prohibición que
 * solo está escrita en un javadoc se rompe un viernes a la tarde sin que nadie se entere.
 * Este test la mide recorriendo el paquete {@code catalog} completo y fallando si en algún
 * archivo aparece una entidad JPA o una interfaz de Spring Data.
 *
 * <p><strong>Se revisa la forma de guardar, no que haya clases.</strong> El contexto tiene
 * legítimamente modelos, puertos, adaptadores y excepciones: eso es justo lo que debe tener
 * quien habla con otro servicio sin hacerse cargo de sus datos. Lo que no puede aparecer es
 * nada que signifique "esto lo guardo en una tabla".
 *
 * <p>El escáner recorre los {@code .class} de todas las clases del paquete en vez de pedirle
 * a Spring una lista de candidatos a bean, porque los candidatos descartan las interfaces, y
 * una interfaz de repositorio es justamente una de las dos formas en que esto se podría
 * colar.
 */
@DisplayName("el contexto catalog no guarda nada del catálogo")
class CatalogContextStructureTest {

    private static final String PAQUETE = "com/bartolomeo/clinics/booking/catalog";
    private static final String PAQUETE_JAVA = "com.bartolomeo.clinics.booking.catalog";

    @Test
    @DisplayName("no hay entidades JPA ni repositories en todo el paquete catalog")
    void noHayPersistenciaDeCatalogo() throws IOException {
        List<String> recorridas = new ArrayList<>();
        List<String> entidades = new ArrayList<>();
        List<String> repositories = new ArrayList<>();

        for (Resource recurso : new PathMatchingResourcePatternResolver()
                .getResources("classpath*:" + PAQUETE + "/**/*.class")) {
            MetadataReader lector = new SimpleMetadataReaderFactory().getMetadataReader(recurso);
            ClassMetadata meta = lector.getClassMetadata();
            recorridas.add(meta.getClassName());
            if (lector.getAnnotationMetadata().hasAnnotation(Entity.class.getName())) {
                entidades.add(meta.getClassName());
            }
            if (meta.isInterface() && Arrays.stream(meta.getInterfaceNames())
                    .anyMatch(nombre -> nombre.startsWith("org.springframework.data.repository"))) {
                repositories.add(meta.getClassName());
            }
        }

        // El escaner tiene que haber visto clases de verdad: un test que mide nada pasa
        // siempre, y seria peor que no tenerlo.
        assertThat(recorridas)
                .as("el escaner tuvo que recorrer el paquete entero")
                .contains(PAQUETE_JAVA + ".infrastructure.rest.CatalogRestAdapter");

        assertThat(entidades)
                .as("entidades JPA dentro de " + PAQUETE_JAVA + ": eso seria una copia"
                        + " local del catalogo, que el enunciado prohibe")
                .isEmpty();
        assertThat(repositories)
                .as("interfaces de Spring Data dentro de " + PAQUETE_JAVA)
                .isEmpty();
    }
}
