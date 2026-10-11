# Pawlift Java Backend

React → **Servlets** → **Service** → **DAO** → **JDBC** → **MySQL**

A Jakarta Servlet 6 web app (WAR) for Apache Tomcat 10.1. It exposes the REST API that `src/lib/api.ts` in the React project calls.

## 1. Requirements

| Tool | Version |
|------|---------|
| Java (JDK) | 17 or newer |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Apache Tomcat | 10.1.x (Jakarta namespace — Tomcat 9 will NOT work) |

Check: `java -version`, `mvn -v`, `mysql --version`.

## 2. Package structure

```text
com.pawlift
├── config     DatabaseConfig (HikariCP pool), AppLifecycleListener
├── model      User (+ Adopter, ShelterUser, AdminUser), Shelter, Pet, AdoptionApplication, Message, Setting, enums
├── dao        DAO interfaces + BaseDAO
├── dao.impl   JDBC implementations (PreparedStatement only)
├── service    Business rules and authorization
├── servlet    REST endpoints (BaseServlet + 6 servlets)
├── filter     CorsFilter, AuthenticationFilter, RoleAuthorizationFilter
├── exception  ApiException and HTTP-status subclasses
└── util       JsonUtil, ApiResponse, Validator, SessionUtil
```

## 3. MySQL setup

```bash
mysql -u root -p < src/main/resources/schema.sql
```

This creates the `pawlift` database, all tables and the default settings. Then create a dedicated user:

```sql
CREATE USER 'pawlift_user'@'localhost' IDENTIFIED BY 'choose_a_password';
GRANT ALL PRIVILEGES ON pawlift.* TO 'pawlift_user'@'localhost';
FLUSH PRIVILEGES;
```

**First admin:** register through the app (or `POST /api/auth/register`), then run:

```sql
UPDATE pawlift.users SET role = 'admin' WHERE email = 'you@example.com';
```

## 4. Configuration (environment variables)

Read via `System.getenv()` or `-D` system properties. See `.env.example`.

| Variable | Example |
|----------|---------|
| `DB_URL` | `jdbc:mysql://localhost:3306/pawlift?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USER` | `pawlift_user` |
| `DB_PASSWORD` | your password |
| `ALLOWED_ORIGINS` | `http://localhost:5173` (comma-separated React origins allowed to send cookies) |

For Tomcat, put them in `TOMCAT_HOME/bin/setenv.sh` (Linux/macOS):

```bash
export DB_URL="jdbc:mysql://localhost:3306/pawlift?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USER="pawlift_user"
export DB_PASSWORD="choose_a_password"
export ALLOWED_ORIGINS="http://localhost:5173"
```

or `setenv.bat` on Windows (`set DB_URL=...`).

## 5. Build

```bash
cd java-backend
mvn clean package
```

Output: `target/pawlift.war`.

## 6. Deploy to Tomcat 10.1

1. Copy `target/pawlift.war` to `TOMCAT_HOME/webapps/`.
2. Start Tomcat: `bin/startup.sh` (or `startup.bat`).
3. The API is at `http://localhost:8080/pawlift/api`.

Note: Tomcat uses port 8080 by default. If the React dev server also uses 8080, change one of them (for example Tomcat's `conf/server.xml` connector port) and update `VITE_API_URL`.

In IntelliJ/Eclipse you can instead add a Tomcat 10.1 run configuration that deploys the `pawlift:war` artifact with context `/pawlift`.

## 7. Connecting React

In the React project's `.env`:

```text
VITE_API_URL=http://localhost:8080/pawlift/api
```

`src/lib/api.ts` sends every request with `credentials: "include"`, so the `JSESSIONID` session cookie is used for login. The React origin must be listed in `ALLOWED_ORIGINS`.

## 8. Security model

- Passwords hashed with BCrypt; never returned in JSON.
- Login stores only the user id + role in the `HttpSession` (new session on login to prevent fixation).
- `AuthenticationFilter` reloads the user from MySQL on every request. Protected endpoints return **401** without a session.
- `RoleAuthorizationFilter` guards `/api/admin/*` (admin only, **403** otherwise).
- Services re-check roles and ownership (a shelter can only edit its own pets and decide on its own applications). User ids and roles are never taken from the request body.

Public (no sign-in): `GET /api/pets`, `GET /api/pets/{id}`, `GET /api/shelters`, `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/logout`.

## 9. API endpoints

All bodies are JSON with snake_case fields. Errors: `{ "error": "..." }` with 400/401/403/404/405/409/500.

| Method | Path | Who | Response |
|--------|------|-----|----------|
| POST | `/api/auth/register` | public | `{ user }` (201) |
| POST | `/api/auth/login` | public | `{ user }` |
| POST | `/api/auth/logout` | any | `{ message }` |
| GET | `/api/auth/me` | signed in | `{ user, shelter }` |
| GET | `/api/pets?q=&species=&city=` | public | `Pet[]` (approved only) |
| GET | `/api/pets/{id}` | public | `{ pet, shelter }` |
| POST | `/api/pets` | shelter | `{ message }` (201) |
| PUT/PATCH | `/api/pets/{id}` | owning shelter / admin | `{ message }` |
| DELETE | `/api/pets/{id}` | owning shelter / admin | `{ message }` |
| GET | `/api/shelters` | public | `Shelter[]` |
| GET | `/api/applications` | signed in | `Application[]` (own / shelter's / all for admin) |
| POST | `/api/applications` | adopter | `{ message }` (201) |
| PUT/PATCH | `/api/applications/{id}/status` | owning shelter / admin | `{ message }` |
| GET | `/api/messages?application_id=` | participants / admin | `Message[]` |
| POST | `/api/messages` | participants / admin | `{ message }` (201) |
| GET | `/api/admin/overview` | admin | `{ users, pets, shelters, settings, analytics }` |
| PUT/PATCH | `/api/admin/users/{id}/role` | admin | `{ message }` |
| DELETE | `/api/admin/users/{id}` | admin | `{ message }` |
| PUT/PATCH | `/api/admin/settings/{key}` | admin | `{ message }` |

## 10. Testing locally with curl

```bash
API=http://localhost:8080/pawlift/api

# register a shelter (cookie saved to jar)
curl -i -c jar -H "Content-Type: application/json" \
  -d '{"full_name":"Ann","email":"ann@test.com","password":"password123","role":"shelter","shelter_name":"Happy Paws","city":"Pune"}' \
  $API/auth/register

curl -b jar $API/auth/me

# add a pet
curl -b jar -H "Content-Type: application/json" \
  -d '{"name":"Milo","species":"dog","breed":"Beagle","age_label":"2 years","size":"Medium","temperament":"Friendly","description":"Loves walks","image_url":""}' \
  $API/pets

curl $API/pets
curl -i $API/admin/overview   # 401 without a session
```
