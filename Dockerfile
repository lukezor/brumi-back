# syntax=docker/dockerfile:1
#
# Imagem da API para o Cloud Run. Duas etapas ("multi-stage"):
#   1. build: JDK completo + Maven Wrapper compilam o jar.
#   2. final: so o JRE (menor) + o jar extraido + o cache AOT. O codigo-fonte e o Maven ficam para tras.
#
# JDK 25.0.5 fixo, a MESMA versao da maquina de desenvolvimento. NAO trocar por 25.0.5.1 (ou "25-jre"),
# porque a empresa ainda nao liberou a atualizacao: o contorno do Java4Shell e a flag -XX:-UseBASE64Intrinsics.

# ---------- 1. build ----------
FROM eclipse-temurin:25.0.5_8-jdk-noble AS build
WORKDIR /src

# Primeiro so o que define as dependencias: enquanto o pom.xml nao mudar, o Docker reaproveita esta camada
# e nao baixa tudo de novo a cada build.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
# extract: separa o jar em app.jar + lib/ (formato que o cache AOT exige). O nome final vem do jar de origem,
# por isso a copia para app.jar antes.
RUN ./mvnw -q -B package -DskipTests \
 && cp target/brumi-api-*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --destination /app

# ---------- 2. final ----------
FROM eclipse-temurin:25.0.5_8-jre-noble

# Usuario sem privilegios: se alguem explorar uma falha na API, nao ganha "root" no container.
RUN useradd --system --uid 10001 --no-create-home brumi \
 && mkdir /app && chown brumi:brumi /app
WORKDIR /app
COPY --from=build --chown=brumi:brumi /app ./
USER brumi

# Opcoes da JVM, iguais no treino e na execucao (o cache AOT so vale com as mesmas opcoes).
#  -XX:-UseBASE64Intrinsics: contorno do Java4Shell (a app se recusa a subir sem ela no perfil prod).
#  -Xmx320m -XX:+UseG1GC: heap e coletor FIXOS. O Cloud Run da 512 MiB e 1 CPU; o build roda numa maquina
#   maior. Se o heap ou o coletor mudarem entre o treino e a execucao, a JVM nao consegue usar o cache AOT.
#   G1 explicito: com 1 CPU a JVM escolheria o SerialGC sozinha, que nao carrega os objetos do cache.
# Treino do cache AOT (Java 25): sobe o Spring ate o fim da configuracao (spring.context.exit=onRefresh) e
# grava as classes ja carregadas e ligadas em app.aot. Na execucao real, a JVM parte desse cache e sobe mais
# rapido. O treino NAO usa banco: Flyway desligado, Hibernate sem consultar o banco, fotos em disco.
# Os valores abaixo sao ficticios e so existem durante o treino.
RUN java -XX:-UseBASE64Intrinsics -Xmx320m -XX:+UseG1GC -XX:AOTCacheOutput=app.aot \
      -Dspring.context.exit=onRefresh \
      -Dspring.profiles.active=prod \
      -Dbrumi.armazenamento.tipo=local \
      -Dspring.flyway.enabled=false \
      -Dspring.jpa.hibernate.ddl-auto=none \
      -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
      -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
      -DDB_URL=jdbc:postgresql://localhost:5432/treino -DDB_USER=treino -DDB_PASSWORD=treino \
      -DJWT_SECRET=somente-para-o-treino-do-cache-aot-nao-e-segredo \
      -jar app.jar \
 && rm -rf uploads

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
# Forma "exec" (lista): o java vira o processo principal e recebe o sinal de parada do Cloud Run (SIGTERM).
ENTRYPOINT ["java", "-XX:-UseBASE64Intrinsics", "-Xmx320m", "-XX:+UseG1GC", "-XX:AOTCache=app.aot", "-jar", "app.jar"]
