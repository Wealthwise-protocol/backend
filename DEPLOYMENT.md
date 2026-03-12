# WealthWise Backend — Deployment Guide (Railway)

> **Recommended Platform:** [Railway](https://railway.app) — free tier, supports Spring Boot JARs natively, no config needed beyond env vars.  
> **Database:** Neon PostgreSQL (already configured — no migration needed)

---

## 🚀 Deploy to Railway (Recommended — Free)

### Step 1 — Push code to GitHub (Feature Branch)

> ⚠️ **Team Project:** Never push directly to `main`. Always use a feature branch and open a Pull Request.

```bash
cd /Users/krishmalvia/Downloads/wealthwise-backend
git init
git add .
git commit -m "feat(auth): add user authentication & management module"
git remote add origin https://github.com/YOUR_ORG/backend.git
git checkout -b feature/user-auth-module
git push -u origin feature/user-auth-module
```

Then open a **Pull Request** on GitHub:  
`feature/user-auth-module` → `main`

> 💡 Or just run the included helper script:
> ```bash
> chmod +x push-feature.sh
> ./push-feature.sh
> ```

---

### Step 2 — Create Railway project

1. Go to [railway.app](https://railway.app) and sign in with GitHub
2. Click **New Project → Deploy from GitHub repo**
3. Select your `wealthwise-backend` repo
4. Railway will auto-detect the `Dockerfile` and start building

---

### Step 3 — Set Environment Variables in Railway

In your Railway project → **Variables** tab, add these:

| Variable | Value |
|----------|-------|
| `PORT` | `8080` |
| `DB_URL` | `jdbc:postgresql://ep-calm-paper-adsxz8lc-pooler.c-2.us-east-1.aws.neon.tech:5432/wealthwise_db?sslmode=require` |
| `DB_USERNAME` | `neondb_owner` |
| `DB_PASSWORD` | `npg_VihaSsLJ7R3r` |
| `JWT_SECRET` | `your-very-strong-random-secret-at-least-32-chars` |
| `JWT_EXPIRATION_MS` | `86400000` |
| `CORS_ALLOWED_ORIGINS` | `https://your-frontend-url.com` |

> ⚠️ **Change `JWT_SECRET`** to a strong random value in production. Never use the default.

---

### Step 4 — Get your live URL

After deployment Railway gives you a public URL like:
```
https://wealthwise-backend-production.up.railway.app
```

Test it:
```
POST https://wealthwise-backend-production.up.railway.app/auth/signup
```

---

## 🐳 Alternative — Deploy with Docker locally

If you want to test the Docker build locally first:

```bash
cd /Users/krishmalvia/Downloads/wealthwise-backend

# Build the image
docker build -t wealthwise-backend .

# Run the container
docker run -p 8080:8080 \
  -e DB_URL="jdbc:postgresql://ep-calm-paper-adsxz8lc-pooler.c-2.us-east-1.aws.neon.tech:5432/wealthwise_db?sslmode=require" \
  -e DB_USERNAME="neondb_owner" \
  -e DB_PASSWORD="npg_VihaSsLJ7R3r" \
  -e JWT_SECRET="your-strong-secret-here" \
  -e PORT=8080 \
  wealthwise-backend
```

Then test at: `http://localhost:8080/auth/signup`

---

## ☁️ Alternative — Deploy to Render (Also Free)

1. Go to [render.com](https://render.com) and sign in with GitHub
2. Click **New → Web Service**
3. Connect your `wealthwise-backend` GitHub repo
4. Set:
   - **Environment:** `Docker`
   - **Branch:** `main`
5. Add the same environment variables as above under **Environment**
6. Click **Create Web Service**

Render gives you a URL like:
```
https://wealthwise-backend.onrender.com
```

> ⚠️ Render free tier **spins down after 15 min of inactivity** — first request may take ~30 seconds to wake up.

---

## ✅ Post-Deployment Checklist

- [ ] Server responds to `GET /auth/me` with `401` (means it's alive and secured)
- [ ] `POST /auth/signup` returns `201` with user + token
- [ ] `POST /auth/signin` returns `200` with user + token
- [ ] User appears in Neon DB after signup
- [ ] Update Postman base URL to the live Railway/Render URL
- [ ] Update `CORS_ALLOWED_ORIGINS` env var to your frontend's deployed URL

---

## 📮 Update Postman After Deployment

Replace `http://localhost:9095` with your live URL in all requests:

| Local | Deployed |
|-------|----------|
| `http://localhost:9095/auth/signup` | `https://your-app.up.railway.app/auth/signup` |
| `http://localhost:9095/auth/signin` | `https://your-app.up.railway.app/auth/signin` |
| `http://localhost:9095/auth/me` | `https://your-app.up.railway.app/auth/me` |

---

## 🔐 Production Security Checklist

- [ ] Set a strong `JWT_SECRET` (at least 32 random characters)
- [ ] Set `CORS_ALLOWED_ORIGINS` to your actual frontend URL only
- [ ] Never commit `.env` or real credentials to GitHub
- [ ] The `application.properties` in this repo uses env var fallbacks — safe to commit


