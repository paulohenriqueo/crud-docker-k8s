#!/usr/bin/env bash
# Sobe backend + MySQL para desenvolvimento e teste manual (Postman, curl).
#
# Provisório: existe porque esta máquina não tem JDK nem Maven instalados, então
# o build só roda dentro de container. Na Semana 2 isto vira `docker compose up`
# e este script é apagado.
set -euo pipefail

REDE=crud-net
MYSQL=crud-mysql
API=crud-api
IMAGEM_MYSQL=mysql:8.4
IMAGEM_MAVEN=maven:3.9-eclipse-temurin-21
PORTA_API=8080
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Credenciais de desenvolvimento local. Não são segredo: o banco só escuta na
# máquina e o volume é descartável. Em Compose e K8s virão de .env e de Secret.
DB_NOME=crud
DB_USUARIO=crud
DB_SENHA=crud-teste-local
DB_SENHA_ROOT=root-teste-local

up() {
  docker network create "$REDE" >/dev/null 2>&1 || true

  if [ -z "$(docker ps -q -f name="^${MYSQL}$")" ]; then
    docker rm -f "$MYSQL" >/dev/null 2>&1 || true
    echo "subindo o MySQL..."
    docker run -d --name "$MYSQL" --network "$REDE" -p 3306:3306 \
      -e MYSQL_ROOT_PASSWORD="$DB_SENHA_ROOT" \
      -e MYSQL_DATABASE="$DB_NOME" \
      -e MYSQL_USER="$DB_USUARIO" \
      -e MYSQL_PASSWORD="$DB_SENHA" \
      -v crud-mysql-data:/var/lib/mysql \
      "$IMAGEM_MYSQL" >/dev/null
  fi

  docker rm -f "$API" >/dev/null 2>&1 || true
  echo "subindo a API..."
  # A aplicação tolera o banco ainda não estar pronto (spring.flyway.connect-retries),
  # então não é preciso esperar o MySQL aqui.
  docker run -d --name "$API" --network "$REDE" -p "${PORTA_API}:8080" \
    -u "$(id -u):$(id -g)" -e HOME="$HOME" \
    -e MYSQL_HOST="$MYSQL" \
    -e MYSQL_DATABASE="$DB_NOME" \
    -e MYSQL_USER="$DB_USUARIO" \
    -e MYSQL_PASSWORD="$DB_SENHA" \
    -e CORS_ALLOWED_ORIGINS=http://localhost:5173 \
    -v "$HOME/.m2":"$HOME/.m2" \
    -v "$RAIZ/backend":/app -w /app \
    "$IMAGEM_MAVEN" \
    mvn -B -Dmaven.repo.local="$HOME/.m2/repository" spring-boot:run >/dev/null

  printf 'aguardando a API responder'
  for _ in $(seq 1 80); do
    if [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${PORTA_API}/actuator/health" 2>/dev/null)" = "200" ]; then
      echo; echo "pronta em http://localhost:${PORTA_API}"
      echo "  health   http://localhost:${PORTA_API}/actuator/health"
      echo "  usuarios http://localhost:${PORTA_API}/api/usuarios"
      return 0
    fi
    printf '.'; sleep 3
  done
  echo; echo "a API não respondeu a tempo. Veja: $0 logs" >&2
  return 1
}

down() {
  # Só remove os containers. O volume do MySQL sobrevive de propósito: derrubar
  # o ambiente não pode apagar dado. Para limpar o banco, use `$0 reset`.
  docker rm -f "$API" "$MYSQL" >/dev/null 2>&1 || true
  docker network rm "$REDE" >/dev/null 2>&1 || true
  echo "ambiente derrubado (o volume crud-mysql-data foi preservado)"
}

reset() {
  echo "Isto APAGA os dados do MySQL local (volume crud-mysql-data)."
  read -r -p "confirmar? [s/N] " r
  [ "$r" = "s" ] || { echo "cancelado"; return 1; }
  down
  docker volume rm crud-mysql-data >/dev/null 2>&1 || true
  echo "volume removido"
}

case "${1:-}" in
  up)     up ;;
  down)   down ;;
  reset)  reset ;;
  logs)   docker logs -f "$API" ;;
  status)
    docker ps -a --filter "name=${MYSQL}" --filter "name=${API}" \
      --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
    ;;
  *) echo "uso: $0 {up|down|reset|logs|status}" >&2; exit 1 ;;
esac
