# Novadent — Backend

Backend del sistema de gestión odontológica. Este repositorio se encuentra en la etapa de preparación del proyecto: actualmente contiene la documentación inicial y todavía no incluye un scaffold ejecutable de Spring Boot.

## Stack objetivo

La implementación prevista para el backend utilizará:

- Java
- Spring Boot
- Spring Web para la API REST
- Spring Data JPA para persistencia
- Bean Validation para validaciones
- Maven para dependencias y build
- OpenAPI/Swagger para documentar la API
- Git y GitHub para colaboración

> Las versiones de Java, Spring Boot, la base de datos y la estrategia de autenticación se confirmarán antes de crear el proyecto ejecutable. No se deben asumir endpoints, entidades ni dependencias que todavía no estén implementados.

## Estado actual

- Rama base de integración: `develop`
- Rama de trabajo de este bloque: `Braian`
- El repositorio aún no contiene `pom.xml`, `src/` ni una aplicación Spring Boot.
- El dashboard frontend usa vistas placeholder y no consume una API real todavía.

La frontera de integración está documentada en [`docs/braian-dashboard-integration.md`](docs/braian-dashboard-integration.md).

## Próximo paso

Completar `TR-00.5 — Preparar el esqueleto inicial del backend`:

1. Crear el proyecto Spring Boot.
2. Confirmar versiones y dependencias.
3. Configurar perfiles por ambiente sin credenciales en Git.
4. Añadir una verificación mínima de arranque.
5. Definir el contrato OpenAPI antes de conectar módulos del frontend.

Cuando esa tarea esté terminada, esta sección deberá incorporar los comandos reales de instalación, ejecución y pruebas.

## Flujo de trabajo

- `develop` es la base de integración.
- Las ramas de trabajo son `Mateo`, `Kevin`, `Braian`, `Josue` e `Iris`.
- Los Pull Requests deben apuntar a `develop` y vincular su HU/TR.
- Mantener commits pequeños, descriptivos y sin secretos.

## Documentación

- [Wiki del proyecto](https://github.com/Proyecto-Houssay/backend-odonto/wiki)
- [Sprint 0](https://github.com/Proyecto-Houssay/backend-odonto/wiki/Sprint-0)
- [Stack tecnológico objetivo](https://github.com/Proyecto-Houssay/backend-odonto/wiki/Stack-tecnologico)
- [`docs/braian-dashboard-integration.md`](docs/braian-dashboard-integration.md)
