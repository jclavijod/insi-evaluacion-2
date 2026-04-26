# Informe Técnico: Sistema de Integración TechNova SpA

**Asignatura:** Integración de Sistemas Informáticos  
**Evaluación:** Unidad II - Arquitecturas Orientadas a Mensajería  
**Autor:** José Clavijo

## 1. Descripción del proyecto

Este proyecto implementa una solución de integración para **TechNova SpA**, con el objetivo de centralizar y procesar pedidos provenientes de distintos canales de venta:

- **Tienda Web** interna, que envía pedidos en **XML**
- **Marketplace** externo, que envía pedidos en **JSON**
- **Sistema Legacy de Facturación**, que consume mensajes **SOAP**
- **Sistema de Bodega / Warehouse**, que recibe el pedido canónico para despacho

La solución utiliza **mensajería asíncrona** con **Jakarta Messaging (JMS)** y **Apache ActiveMQ Artemis** como broker de colas, permitiendo desacoplar los sistemas y aplicar patrones de integración empresarial.

## 2. Arquitectura general

La integración se implementa mediante un flujo de mensajes con estas etapas:

1. **Ingreso del pedido**
   - La tienda web publica pedidos en XML.
   - El marketplace publica pedidos en JSON.

2. **Normalización**
   - Un traductor transforma los mensajes heterogéneos a un **modelo canónico JSON**.

3. **Distribución**
   - El mensaje canónico se publica hacia los sistemas destino.

4. **Consumo especializado**
   - **Facturación** convierte el JSON canónico a SOAP para el sistema legacy.
   - **Bodega** consume el pedido canónico para su procesamiento logístico.

## 3. Patrones de integración utilizados

En la solución se aplican los siguientes **Enterprise Integration Patterns (EIP)**:

- **Canonical Data Model**
  - Se definió un formato JSON canónico para unificar los pedidos de distintos orígenes.

- **Message Translator**
  - Traducción de XML a JSON canónico.
  - Traducción de JSON canónico a SOAP para el sistema legacy.

- **Pipes and Filters**
  - El flujo pasa por varias colas JMS que actúan como canales de comunicación desacoplados.

- **Recipient List**
  - El adaptador de facturación distribuye el pedido hacia más de un destino cuando corresponde.

- **Service Activator**
  - Los listeners JMS reaccionan al recibir mensajes y ejecutan la lógica de negocio.

## 4. Tecnologías utilizadas

- **Java 21**
- **Jakarta Messaging / JMS**
- **Apache ActiveMQ Artemis 2.x**
- **Maven**
- **JSON**
- **SOAP**
- **Git / GitHub**

## 5. Estructura del repositorio

El repositorio contiene los siguientes módulos principales:

- `activemq-config`
  - Configuración del broker Artemis
- `message-translators`
  - Traductor de mensajes entre la tienda web / marketplace y el modelo canónico
- `webstore-mock`
  - Simulador de pedidos XML desde la tienda web
- `marketplace-adapter`
  - Adaptador para pedidos provenientes del marketplace
- `facturacion-adapter`
  - Adaptador hacia el sistema legacy de facturación y simulador de respuesta
- `warehouse-adapter`
  - Consumidor final para el sistema de bodega
- `tienda-web`
  - Proyecto base de la aplicación web
- `evidences`
  - Carpeta destinada a capturas y evidencias de ejecución

## 6. Colas y canales de mensajería

El sistema utiliza las siguientes colas en ActiveMQ Artemis:

| Cola | Descripción |
|------|-------------|
| `jcl_web_pedidos` | Entrada de pedidos XML desde la tienda web |
| `jcl_mkp_pedidos` | Entrada de pedidos JSON desde el marketplace |
| `jcl_marketplace_pedidos` | Cola adicional configurada para marketplace |
| `jcl_canonical_pedidos` | Cola del mensaje canónico JSON |
| `jcl_pedidos` | Canal intermedio adicional de pedidos |
| `jcl_facturacion` | Cola de salida hacia el sistema legacy de facturación |
| `jcl_facturacion_respuesta` | Cola de respuesta / ACK del sistema legacy |
| `jcl_bodega` | Cola para el sistema de bodega |

## 7. Flujo funcional

### 7.1 Flujo desde la tienda web

1. `WebStoreMock` envía un pedido en XML a `jcl_web_pedidos`
2. `MessageTranslators` consume el XML y lo transforma a JSON canónico
3. El JSON canónico se publica en `jcl_canonical_pedidos`
4. `BillingAdapterMain` consume el mensaje canónico
5. El adaptador construye el SOAP y lo envía a `jcl_facturacion`
6. `FacturacionAdapterMain` simula el sistema legacy y responde con un ACK en `jcl_facturacion_respuesta`
7. El pedido también se reenvía a `jcl_bodega`
8. `WarehouseAdapterMain` consume el pedido en bodega

### 7.2 Flujo desde marketplace

1. El marketplace publica un pedido JSON en su cola de entrada
2. `MessageTranslators` normaliza el contenido al modelo canónico
3. El flujo continúa hacia facturación y bodega

## 8. Evidencia de funcionamiento

Durante la ejecución se verificó el siguiente comportamiento:

- El broker Artemis levantó correctamente con las colas configuradas
- El mock de tienda web envió el pedido XML sin errores
- El traductor generó el **JSON canónico**
- El adaptador de facturación construyó y envió el **SOAP**
- El simulador legacy respondió con un **ACK**
- El sistema de bodega consumió correctamente el pedido final

Las capturas de ejecución muestran el flujo completo de extremo a extremo.

## 9. Configuración del broker

El broker Artemis fue configurado para pruebas con un umbral de disco más permisivo, evitando bloqueos prematuros por uso elevado del almacenamiento.  
Esto permitió mantener el broker operativo durante las pruebas de integración.

## 10. Requisitos previos

Antes de ejecutar el proyecto, asegúrate de tener instalado:

- Java 21
- Maven
- Apache ActiveMQ Artemis 2.x
- Git

## 11. Cómo ejecutar el sistema

### 11.1 Levantar el broker

Desde el directorio de Artemis:

```bash
./artemis.cmd run
```

### 11.2 Ejecutar los módulos

Compilar cada módulo si es necesario:

```bash
mvn -f ./message-translators/pom.xml clean package -DskipTests
mvn -f ./facturacion-adapter/pom.xml clean package -DskipTests
mvn -f ./warehouse-adapter/pom.xml clean package -DskipTests
mvn -f ./webstore-mock/pom.xml clean package -DskipTests
```

Ejecutar los componentes en terminales separadas:

```bash
mvn -f ./message-translators/pom.xml exec:java "-Dexec.mainClass=cl.jclavijo.MessageTranslators"
mvn -f ./facturacion-adapter/pom.xml exec:java "-Dexec.mainClass=cl.jclavijo.facturacion.BillingAdapterMain"
mvn -f ./facturacion-adapter/pom.xml exec:java "-Dexec.mainClass=cl.jclavijo.facturacion.FacturacionAdapterMain"
mvn -f ./warehouse-adapter/pom.xml exec:java "-Dexec.mainClass=cl.jclavijo.bodega.WarehouseAdapterMain"
mvn -f ./webstore-mock/pom.xml exec:java "-Dexec.mainClass=cl.jclavijo.WebStoreMock"
```

### 11.3 Verificación

Puedes revisar el estado de las colas en la consola de Artemis:

[http://localhost:8161/console](http://localhost:8161/console)

## 12. Resultados esperados

Al ejecutar el sistema correctamente, deberías ver:

- Mensajes entrando en `jcl_web_pedidos`
- Transformación a JSON canónico en consola
- Envío de SOAP hacia `jcl_facturacion`
- Respuesta ACK en `jcl_facturacion_respuesta`
- Consumo del pedido en `jcl_bodega`

## 13. Observaciones finales

Este proyecto demuestra una arquitectura de integración desacoplada, basada en mensajería y patrones EIP, capaz de conectar sistemas heterogéneos sin dependencia directa entre ellos.

La solución valida correctamente:

- Transformación de formatos
- Comunicación asíncrona
- Integración con sistema legacy
- Distribución hacia múltiples destinos
- Evidencia completa del flujo end-to-end

## 14. Autor

**José Clavijo**
