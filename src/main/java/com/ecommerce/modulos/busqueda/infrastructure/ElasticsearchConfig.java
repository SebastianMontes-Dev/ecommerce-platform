package com.ecommerce.modulos.busqueda.infrastructure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ElasticsearchConfig {

    private static final String INDICE_PRODUCTOS = "productos";

    @Value("${app.elasticsearch.host:localhost}")
    private String host;

    @Value("${app.elasticsearch.port:9200}")
    private int port;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(new HttpHost(host, port, "http")).build();

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper(mapper));

        return new ElasticsearchClient(transport);
    }

    /**
     * Crea el índice "productos" con un mapping explícito la primera vez que arranca la
     * aplicación, en vez de depender del dynamic mapping por defecto de Elasticsearch (que
     * hoy infiere, por ejemplo, {@code precio} como {@code long} y {@code idTienda} como
     * {@code text}+{@code .keyword} en lugar de {@code keyword} puro).
     * <p>
     * Idempotente: verifica primero con {@code indices().exists(...)} y no hace nada si el
     * índice ya existe, para no romper arranques repetidos del contexto (tests de
     * integración) ni intentar recrear un índice ya poblado. Además, si dos réplicas
     * arrancan a la vez y ambas ven {@code exists()==false} antes de que la primera termine
     * de crear el índice, la segunda recibe un {@code resource_already_exists_exception}
     * (HTTP 400) al llamar {@code create()}; ese caso puntual se trata igual que "el índice
     * ya existe" en vez de tumbar el arranque de esa réplica.
     */
    @Bean
    public ApplicationRunner bootstrapIndiceProductos(ElasticsearchClient elasticsearchClient) {
        return (ApplicationArguments args) -> {
            boolean existe = elasticsearchClient.indices()
                    .exists(e -> e.index(INDICE_PRODUCTOS))
                    .value();

            if (existe) {
                log.info("El índice '{}' ya existe en Elasticsearch, se omite su creación", INDICE_PRODUCTOS);
                return;
            }

            try {
                elasticsearchClient.indices().create(c -> c
                        .index(INDICE_PRODUCTOS)
                        .mappings(m -> m
                                .properties("id", p -> p.keyword(k -> k))
                                .properties("idTienda", p -> p.keyword(k -> k))
                                .properties("nombre", p -> p.text(t -> t.analyzer("spanish")
                                        .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                                .properties("descripcion", p -> p.text(t -> t.analyzer("spanish")))
                                .properties("nombreCategoria", p -> p.text(t -> t.analyzer("spanish")
                                        .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                                .properties("enlaceCorto", p -> p.keyword(k -> k))
                                .properties("urlImagen", p -> p.keyword(k -> k))
                                .properties("precio", p -> p.double_(d -> d))
                                .properties("calificacionPromedio", p -> p.double_(d -> d))
                                .properties("conteoResenas", p -> p.long_(l -> l))));

                log.info("Índice '{}' creado en Elasticsearch con mapping explícito", INDICE_PRODUCTOS);
            } catch (ElasticsearchException e) {
                String tipoError = e.error().type();
                if (e.status() == 400 && tipoError != null && tipoError.contains("resource_already_exists_exception")) {
                    log.info("El índice '{}' ya existe (creado concurrentemente por otra instancia), se omite",
                            INDICE_PRODUCTOS);
                } else {
                    throw e;
                }
            }
        };
    }
}
