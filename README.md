# Novadent — Backend

Backend del sistema de gestión odontológica **Novadent**.

El proyecto se encuentra construido sobre Spring Boot y está preparado para incorporar progresivamente los módulos de negocio definidos mediante Historias de Usuario (HU) y Tareas (TR).

## Stack tecnológico

* Java 17
* Spring Boot 3.5.6
* Maven
* Spring Web
* Bean Validation
* Spring Data JPA
* Base de datos relacional
* Git y GitHub
* OpenAPI/Swagger, previsto para la documentación de la API

## Estado actual

El proyecto cuenta con el scaffold inicial de Spring Boot.

Actualmente incluye:

* Configuración base de Spring Boot.
* Maven Wrapper (`mvnw` / `mvnw.cmd`).
* Spring Web.
* Bean Validation.
* Configuración inicial de pruebas.
* Estructura preparada para incorporar los módulos de negocio.

La implementación de las entidades, persistencia, autenticación, roles y demás funcionalidades se realizará progresivamente mediante las HU/TR correspondientes.

## Inicio rápido

### Windows

```powershell
git clone https://github.com/Proyecto-Houssay/backend-odonto.git
cd backend-odonto
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

### macOS / Linux

```bash
./mvnw test
./mvnw spring-boot:run
```

## Estructura inicial

```text
src/
├── main/
│   ├── java/
│   │   └── com/proyectohoussay/odonto/
│   │       └── BackendOdontoApplication.java
│   │
│   └── resources/
│       └── application.properties
│
└── test/
    └── java/
        └── com/proyectohoussay/odonto/
            └── BackendOdontoApplicationTests.java

docs/
├── backend-scaffold.md
└── braian-dashboard-integration.md
```

La estructura crecerá a medida que se incorporen los módulos funcionales del sistema.

## Flujo de trabajo

* `develop` es la rama base de integración.
* Las ramas de trabajo se utilizan para desarrollar las HU/TR asignadas.
* Los Pull Requests deben apuntar a `develop`.
* Cada Pull Request debe vincular la HU/TR correspondiente.
* Mantener commits pequeños y descriptivos.
* No publicar credenciales, contraseñas ni archivos de configuración sensibles.

## Próximos pasos

1. Incorporar las entidades del dominio.
2. Configurar persistencia con Spring Data JPA.
3. Implementar las relaciones entre entidades.
4. Incorporar validaciones mediante Bean Validation.
5. Crear repositorios y pruebas de persistencia.
6. Implementar progresivamente las HU/TR del proyecto.
7. Definir y documentar la API mediante OpenAPI/Swagger.
8. Incorporar autenticación y autorización.
9. Integrar los módulos del backend con el frontend.

## Documentación

* `docs/backend-scaffold.md`
* `docs/braian-dashboard-integration.md`
* Wiki del proyecto:
  https://github.com/Proyecto-Houssay/backend-odonto/wiki

## Proyecto

Repositorio:

https://github.com/Proyecto-Houssay/backend-odonto
