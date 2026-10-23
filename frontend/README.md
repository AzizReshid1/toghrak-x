# Toghrak X: frontend

Angular 21 application for Toghrak X. See the [main README](../README.md) for the whole project and deployment.

## Run

```bash
npm install
npx ng serve
```

Open `http://localhost:4200`. The dev server forwards `/api` and `/images` to the backend on `http://localhost:8081` (see `proxy.conf.json`), so the backend must be running.

## Build

```bash
npx ng build
```

The output goes to `dist/toghrak-x/browser`. The app calls the API through the relative URL `/api`, so in production the web server must forward `/api` and `/images` to the backend.

## Tests

```bash
npx ng test                                        # Karma, Chrome, watch mode
npx ng test --watch=false --browsers=ChromeHeadless  # single headless run
```

## Layout

| Folder | Contents |
|---|---|
| `src/app/features` | Screens, grouped by area: `posts`, `admin`, `auth`, `user`, `static`, `home` |
| `src/app/services` | API calls and shared state (session, toasts) |
| `src/app/interceptors` | Adds the login token to API calls; shows errors as toasts |
| `src/app/guards` | Route protection by login and role |
| `src/app/models` | TypeScript types for API data |
| `src/app/editor-config.ts` | Rich-text editor (Quill) settings |
