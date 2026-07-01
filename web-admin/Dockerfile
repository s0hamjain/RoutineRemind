FROM node:22-alpine AS build
WORKDIR /app
ARG API_BASE_URL=/api/v1
COPY package*.json ./
RUN npm ci --no-audit --no-fund --loglevel=error
COPY . .
RUN node -e "const fs=require('fs'); const p='src/environments/environment.prod.ts'; let s=fs.readFileSync(p,'utf8'); s=s.replace(/apiBaseUrl: '[^']*'/, \"apiBaseUrl: '\" + process.env.API_BASE_URL + \"'\"); fs.writeFileSync(p,s);" 
RUN npm run build -- --configuration production

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/web-admin/browser /usr/share/nginx/html
EXPOSE 8080
