FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
COPY apps ./apps
COPY packages ./packages
COPY tsconfig.json ./
RUN npm ci
ARG VITE_API_URL=/api/v1
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/apps/candidate/dist /usr/share/nginx/html/candidate
COPY --from=build /app/apps/employer/dist /usr/share/nginx/html/employer
EXPOSE 80
