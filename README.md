# California Housing Analytics 6 ML Engine 🏠⚡

Un ecosistema integral de **Ingeniería de Datos**, **Machine Learning** y **Deep Learning** construido en Java para procesar, analizar y predecir valores de viviendas basados en el dataset *California Housing*.

El sistema integra desde la ingesta de datos y orquestación ETL hasta la ejecución distribuida, entrenamiento de modelos predictivos y exposición de un microservicio REST listo para producción.

## 🛠️ Arquitectura y Tecnologías

```text
                                    +-----------------------+
                                    |   CSV Raw Dataset     |
                                    +-----------+-----------+
                                                |
                                                v
                                  +---------------------------+
                                  |   Pentaho Data Integration|
                                  |      (Feature Eng. ETL)   |
                                  +-------------+-------------+
                                                |
                                                v
                                   +-------------------------+
                                   |      MySQL Staging      |
                                   | (stg_housing_processed) |
                                   +----+---------------+----+
                                        |               |
                    +-------------------+               +-------------------+
                    |                                                       |
                    v                                                       v
     +------------------------------+                        +------------------------------+
     |  Apache Spark (Batch/SQL)    |                        |   Apache Beam / Dataflow     |
     | (agg_housing_ocean_summary)  |                        |  (Transformación Distribuida)|
     +------------------------------+                        +------------------------------+
                    |
                    +-------------------+-------------------+
                                        |
                                        v
                    +---------------------------------------+
                    |      MLlib & Deep Java Library        |
                    | (Random Forest, GBTs & MLP PyTorch)   |
                    +-------------------+-------------------+
                                        |
                                        v
                    +---------------------------------------+
                    |        Spring Boot REST API           |
                    |     (/api/v1/housing/predict)         |
                    +---------------------------------------+
```

| **Capa / Módulo** | **Tecnología** | **Descripción** |
| --- | --- | --- |
| **ETL & Orquestación** | Pentaho PDI (`.ktr`, `.kjb`) | Extracción, feature engineering inicial e ingesta a MySQL. |
| **Base de Datos** | MySQL 8.x | Almacenamiento en capas (*Staging* y *Analytics*). |
| **Procesamiento Batch** | Apache Spark SQL 3.5 | Agregaciones estadísticas masivas e indicadores por zonas. |
| **Pipeline Distribuido** | Apache Beam 2.54 | Procesamiento distribuido preparado para Dataflow. |
| **Machine Learning** | Spark MLlib | Regresión con *Random Forest*, *Gradient-Boosted Trees (GBT)* y ajuste mediante `CrossValidator`. |
| **Deep Learning** | Deep Java Library (DJL) | Perceptrón multicapa (MLP) sobre motor PyTorch desde Java. |
| **Servicio REST** | Spring Boot 3.2 | API para consumo e inferencia del modelo en tiempo real. |

```text
HousingAnalytics/
├── data/
│   └── housing.csv                     # Dataset fuente
├── pentaho/
│   ├── trans_housing_feature_engineering.ktr
│   └── job_housing_etl_master.kjb
├── sql/
│   └── init_schema.sql                 # Esquemas MySQL
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── datanalytics/
│       │           ├── HousingAnalyticsApplication.java  # Main Spring Boot
│       │           ├── controller/                       # API Endpoints
│       │           ├── service/                          # Lógica de inferencia
│       │           ├── dataflow/                         # Apache Beam Pipeline
│       │           ├── spark/                            # Spark Batch Jobs
│       │           └── ml/                               # MLlib, VIF & DJL MLP
│       └── resources/
│           └── application.properties
└── pom.xml                             # Configuración Maven y dependencias
```

## 🚀 Guía de Instalación y Ejecución

### Prerrequisitos

- **Java:** JDK 17
- **Apache Maven:** 3.8+
- **Base de datos:** MySQL 8.x activo en `localhost:3306`
- **Pentaho Data Integration:** instalado si vas a ejecutar los procesos ETL
- **Dataset:** archivo `data/housing.csv` disponible

### 1. Configuración de la base de datos

Crea la base de datos MySQL desde tu cliente SQL:

```sql
CREATE DATABASE IF NOT EXISTS housing_db;
```

Después, revisa y ejecuta el script de inicialización:

```bash
mysql -u <usuario> -p housing_db < sql/init_schema.sql
```

Ajusta las credenciales de conexión en `src/main/resources/application.properties` o mediante variables de entorno, según la configuración de la aplicación.

### 2. Ejecutar la transformación ETL (Pentaho PDI)

Abre la transformación o el Job con Spoon, o ejecuta el Job desde la consola con Kitchen:

```bash
kitchen.sh -file=pentaho/job_housing_etl_master.kjb
```

En Windows, utiliza el ejecutable equivalente de Pentaho. Verifica que el Job termine correctamente y cargue la tabla `stg_housing_processed`.

### 3. Compilar el proyecto Java

```bash
mvn clean install -DskipTests
```

> `-DskipTests` omite la ejecución de pruebas; no reemplaza una validación mediante tests antes de desplegar.

## 🔬 Ejecución de módulos

Los siguientes comandos asumen que las clases indicadas existen en el proyecto y que las dependencias están configuradas en `pom.xml`. Si los módulos se ejecutan mediante `exec-maven-plugin`, puedes iniciarlos así:

### Análisis estadístico y VIF

Calcula la matriz de correlación de Pearson y el factor de inflación de la varianza (VIF) para explorar posibles problemas de multicolinealidad.

```bash
mvn exec:java -Dexec.mainClass="com.datanalytics.ml.HousingStatisticalAnalysis"
```

### Job batch en Spark

Ejecuta agregaciones sobre la proximidad al océano y persiste los resultados en `agg_housing_ocean_summary`.

```bash
mvn exec:java -Dexec.mainClass="com.datanalytics.spark.HousingSparkBatchJob"
```

### Pipeline Apache Beam

```bash
mvn exec:java -Dexec.mainClass="com.datanalytics.dataflow.HousingDataFlowPipeline"
```

> Para ejecutarlo en Google Cloud Dataflow, configura el runner, el proyecto, la región, las credenciales y las ubicaciones de staging requeridas. El comando anterior, por sí solo, no garantiza una ejecución en Dataflow.

### Entrenamiento de Machine Learning (Spark MLlib)

Entrena modelos de regresión como Random Forest y Gradient-Boosted Trees, con ajuste de hiperparámetros mediante `CrossValidator` si está implementado en el pipeline.

```bash
mvn exec:java -Dexec.mainClass="com.datanalytics.ml.HousingMLPipeline"
```

### Red neuronal (DJL / PyTorch)

```bash
mvn exec:java -Dexec.mainClass="com.datanalytics.ml.HousingDeepLearningMLP"
```

Asegúrate de que el motor de PyTorch y las dependencias nativas compatibles con tu sistema operativo estén declarados y configurados en Maven.

## 🌐 Servicio de inferencia en tiempo real

Inicia la aplicación Spring Boot:

```bash
mvn spring-boot:run
```

También puedes ejecutar la clase `HousingAnalyticsApplication.java` directamente desde tu IDE.

### Endpoints

#### 1. Healthcheck

```http
GET http://localhost:8080/api/v1/housing/health
```

#### 2. Predicción del valor de una vivienda

```http
POST http://localhost:8080/api/v1/housing/predict
Content-Type: application/json
```

**Cuerpo de la petición (request JSON):**

```json
{
  "longitude": -122.23,
  "latitude": 37.88,
  "housingMedianAge": 41.0,
  "totalRooms": 880.0,
  "totalBedrooms": 129.0,
  "population": 322.0,
  "households": 126.0,
  "medianIncome": 8.3252,
  "oceanProximity": "NEAR BAY"
}
```

**Ejemplo de respuesta exitosa (response JSON):**

```json
{
  "predictedMedianHouseValue": 452250.0,
  "modelVersion": "v1.0.0-RandomForest/SparkML",
  "status": "SUCCESS"
}
```

> La respuesta es ilustrativa: el valor predicho y la versión del modelo dependerán del modelo entrenado, sus datos y la implementación real del endpoint.

### Ejemplo con `curl`

```bash
curl -X POST "http://localhost:8080/api/v1/housing/predict" \
  -H "Content-Type: application/json" \
  -d '{
    "longitude": -122.23,
    "latitude": 37.88,
    "housingMedianAge": 41.0,
    "totalRooms": 880.0,
    "totalBedrooms": 129.0,
    "population": 322.0,
    "households": 126.0,
    "medianIncome": 8.3252,
    "oceanProximity": "NEAR BAY"
  }'
```

## 📊 Flujo de datos

1. **Ingesta:** lectura del dataset CSV original.
2. **Transformación:** limpieza y generación inicial de características mediante Pentaho PDI.
3. **Persistencia:** carga de los datos procesados en MySQL.
4. **Analítica distribuida:** agregaciones batch y análisis por zona con Spark; pipeline distribuido con Beam.
5. **Entrenamiento:** comparación de modelos de ML y entrenamiento de una red neuronal.
6. **Inferencia:** exposición del modelo seleccionado mediante una API REST de Spring Boot.

## ✅ Recomendaciones antes de producción

- Separar configuración y secretos de las propiedades versionadas; utilizar variables de entorno o un gestor de secretos.
- Versionar los modelos entrenados y guardar metadatos de entrenamiento, métricas y esquema de entrada.
- Validar los datos de entrada con Bean Validation y manejar errores mediante respuestas HTTP consistentes.
- Medir el rendimiento del modelo con un conjunto de prueba independiente y métricas adecuadas, por ejemplo RMSE, MAE y \(R^2\).
- Añadir pruebas unitarias e integración para ETL, procesamiento, entrenamiento y endpoints.
- Configurar logs, métricas, health checks y trazabilidad.
- Documentar la compatibilidad de versiones entre Java, Spark, Beam, DJL y sus dependencias nativas.

## 📌 Estado del proyecto

Este README describe la arquitectura objetivo y comandos de referencia. La disponibilidad real de cada módulo, clase, tabla, endpoint y dependencia debe verificarse en el código fuente y en la configuración del entorno.

---

**California Housing Analytics & ML Engine** — Ingeniería de Datos, analítica y modelos predictivos con Java.