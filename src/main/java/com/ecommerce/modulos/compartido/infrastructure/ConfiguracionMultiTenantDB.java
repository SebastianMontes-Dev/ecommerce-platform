package com.ecommerce.modulos.compartido.infrastructure;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ConfiguracionMultiTenantDB {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource defaultDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSource defaultDataSource) {
        AbstractRoutingDataSource routingDataSource = new EnrutadorFuenteDatosInquilino();

        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("default", defaultDataSource);

        // Aquí registraríamos dinámicamente las bases de datos de clientes Premium 
        // conectándonos a una DB "Master" o leyendo variables de entorno.
        // Ejemplo ficticio:
        // dataSources.put("tenant-premium-1", crearPremiumDataSource("jdbc:postgresql://db-premium-1/tenant"));

        routingDataSource.setDefaultTargetDataSource(defaultDataSource);
        routingDataSource.setTargetDataSources(dataSources);
        routingDataSource.afterPropertiesSet();

        return routingDataSource;
    }
}
