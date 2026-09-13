package com.ecommerce.modulos.busqueda.infrastructure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
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

import java.io.IOException;

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
     * Idempotente: si el índice ya existe con el mapping esperado ({@code idTienda} como
     * {@code keyword} puro) no hace nada. Si existe pero con un mapping viejo/dinámico
     * (por ejemplo un volumen `es_data` de un entorno previo a esta fase), lo borra y lo
     * recrea con el mapping explícito — se pierden los documentos indexados, pero
     * Elasticsearch es un read-side derivado de Postgres vía outbox, así que en teoría es
     * reconstruible a medida que se creen/actualicen productos.
     * <p>
     * Todo el bootstrap corre dentro de un {@code try/catch} que nunca propaga: un problema
     * de Elasticsearch al arrancar (host inalcanzable, timeout, lo que sea) no debe tumbar
     * el {@code ApplicationContext} completo — la búsqueda es un read-side CQRS opcional,
     * no una dependencia dura del checkout/las órdenes. Esto es además necesario para que
     * los {@code @SpringBootTest} del repo que no configuran un Elasticsearch real (todos
     * salvo {@code OutboxIndexacionIntegrationTest}) sigan arrancando su contexto sin este
     * bean tumbándolo al no poder conectar a {@code localhost:9200}.
     * <p>
     * Dentro de la creación, si dos réplicas arrancan a la vez y ambas ven
     * {@code exists()==false} antes de que la primera termine de crear el índice, la
     * segunda recibe un {@code resource_already_exists_exception} (HTTP 400) al llamar
     * {@code create()}; ese caso puntual se trata igual que "el índice ya existe" (log info,
     * no error) en vez de tratarlo como una falla real.
     */
    @Bean
    public ApplicationRunner bootstrapIndiceProductos(ElasticsearchClient elasticsearchClient) {
        return (ApplicationArguments args) -> {
            try {
                boolean existe = elasticsearchClient.indices()
                        .exists(e -> e.index(INDICE_PRODUCTOS))
                        .value();

                if (existe) {
                    if (idTiendaEsKeywordPuro(elasticsearchClient)) {
                        log.info("El índice '{}' ya existe con el mapping esperado, se omite su creación",
                                INDICE_PRODUCTOS);
                    } else {
                        log.warn("El índice '{}' existe con un mapping desactualizado (idTienda no es keyword "
                                + "puro, probablemente dynamic mapping de un entorno previo a esta fase). Se borra "
                                + "y se recrea con el mapping explícito: el catálogo indexado se resetea y se irá "
                                + "repoblando a medida que se creen/actualicen productos.", INDICE_PRODUCTOS);
                        elasticsearchClient.indices().delete(d -> d.index(INDICE_PRODUCTOS));
                        crearIndiceConMappingExplicito(elasticsearchClient);
                    }
                    return;
                }

                crearIndiceConMappingExplicito(elasticsearchClient);
            } catch (Exception e) {
                log.error("No se pudo verificar/crear el índice '{}' en Elasticsearch al arrancar; se continúa "
                        + "sin el bootstrap del índice (la búsqueda es un read-side derivado del outbox, no debe "
                        + "tumbar el arranque de la aplicación)", INDICE_PRODUCTOS, e);
            }
        };
    }

    /**
     * {@code true} si el índice "productos" ya existe y su campo {@code idTienda} está
     * mapeado como {@code keyword} puro (el mapping explícito de esta clase). {@code false}
     * si el campo no existe, es de otro tipo (por ejemplo {@code text} del dynamic mapping
     * viejo), o el índice no tiene mapping para ese campo.
     */
    private boolean idTiendaEsKeywordPuro(ElasticsearchClient elasticsearchClient) throws IOException {
        GetMappingResponse respuesta = elasticsearchClient.indices().getMapping(g -> g.index(INDICE_PRODUCTOS));
        IndexMappingRecord registro = respuesta.get(INDICE_PRODUCTOS);
        if (registro == null) {
            return false;
        }
        Property idTienda = registro.mappings().properties().get("idTienda");
        return idTienda != null && idTienda.isKeyword();
    }

    private void crearIndiceConMappingExplicito(ElasticsearchClient elasticsearchClient) throws IOException {
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
    }
}
