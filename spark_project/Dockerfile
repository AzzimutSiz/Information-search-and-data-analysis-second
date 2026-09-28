FROM eclipse-temurin:17-jre-jammy AS spark-download

ARG SPARK_VERSION=3.5.7
ARG HADOOP_VERSION=3
ARG SPARK_DOWNLOAD_URL=https://www.apache.org/dyn/closer.lua

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends ca-certificates curl gnupg; \
    rm -rf /var/lib/apt/lists/*; \
    archive="spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz"; \
    gpg_key="564CA14951C29266889F9C5B90E2BA86F7A9B307"; \
    temp_dir="$(mktemp -d)"; \
    gpg_dir="$(mktemp -d)"; \
    curl --fail --location --retry 3 \
        "${SPARK_DOWNLOAD_URL}/spark/spark-${SPARK_VERSION}/${archive}?action=download" \
        --output "${temp_dir}/${archive}"; \
    curl --fail --location --retry 3 \
        "${SPARK_DOWNLOAD_URL}/spark/spark-${SPARK_VERSION}/${archive}.asc?action=download" \
        --output "${temp_dir}/${archive}.asc"; \
    GNUPGHOME="${gpg_dir}" gpg --batch --keyserver hkps://keys.openpgp.org \
        --recv-keys "${gpg_key}" || \
    GNUPGHOME="${gpg_dir}" gpg --batch --keyserver hkps://keyserver.ubuntu.com \
        --recv-keys "${gpg_key}"; \
    GNUPGHOME="${gpg_dir}" gpg --batch --verify \
        "${temp_dir}/${archive}.asc" "${temp_dir}/${archive}"; \
    mkdir -p /opt/spark; \
    tar -xzf "${temp_dir}/${archive}" --directory /opt/spark --strip-components=1; \
    rm -rf "${temp_dir}" "${gpg_dir}"

FROM maven:3.9.11-eclipse-temurin-17 AS java-build

WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src/main ./src/main
RUN mvn -B package

FROM eclipse-temurin:17-jre-jammy AS spark-runtime

ENV SPARK_HOME=/opt/spark \
    SPARK_MASTER_URL=spark://spark-master:7077 \
    SPARK_DRIVER_HOST=spark-api \
    SPARK_EXECUTOR_MEMORY=512m \
    SPARK_CORES_MAX=2

COPY --from=spark-download /opt/spark /opt/spark

WORKDIR /opt/spark-app
COPY --from=java-build /build/target/spark-api.jar ./spark-api.jar

CMD ["bash", "-lc", "exec \"$SPARK_HOME/bin/spark-submit\" --master \"$SPARK_MASTER_URL\" --deploy-mode client --executor-memory \"$SPARK_EXECUTOR_MEMORY\" --conf \"spark.cores.max=$SPARK_CORES_MAX\" --class ru.sparkproject.api.SparkApiApplication /opt/spark-app/spark-api.jar"]
