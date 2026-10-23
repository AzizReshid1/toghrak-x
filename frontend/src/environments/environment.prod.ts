export const environment = {
  production: true,
  // Relative: the browser calls /api on the same domain the site was loaded from, and the
  // reverse proxy (Nginx or the AWS load balancer) forwards /api to Spring Boot.
  apiBaseUrl: '/api',
};
