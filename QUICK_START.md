# 🚀 Quick Start Guide - Buy-01 E-Commerce Platform

## ⚡ Fastest Way to Run Everything

```bash
# Step 1: Clone the repository
git clone https://github.com/mahdikheirkhah/buy-01.git
cd buy-01

# Step 2: Build and start everything (one command!)
make all

# That's it! Everything builds and starts.
```

## ✅ Verify Everything is Running

```bash
# Check all containers
docker ps

# You should see:
# - api-gateway (Port 8443)
# - user-service (Port 8081)fmmfkmfmfmf
# - product-service (Port 8082)
# - media-service (Port 8083)
# - discovery-service (Port 8761)
# - frontend (Port 4200)
# - kafka, zookeeper, mongo, sonarqube
```

## 🌐 Access the Applications

| Service              | URL                                    | Credentials    |
| -------------------- | -------------------------------------- | -------------- |
| **Frontend**         | https://localhost:4200                 | Create account |
| **API Gateway**      | https://localhost:8443/actuator/health | -              |
| **Eureka Discovery** | http://localhost:8761                  | -              |
| **SonarQube**        | http://localhost:9000                  | admin / admin  |

## 📊 Common Commands

### Start/Stop Services

```bash
# Start everything
make all

# Just start (if already built)
make up

# Stop everything
make down

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f api-gateway
```

### Clean Up

```bash
# Stop and remove everything (keeps code)
make clean

# Remove all images and volumes (full reset)
docker system prune -a --volumes
```

### Build Only

```bash
# Rebuild all images
make build

# Build specific service
docker build \
  --file backend/user-service/Dockerfile \
  --tag mahdikheirkhah/user-service:latest \
  backend/user-service/
```

## 🐛 Troubleshooting

### Problem: Ports Already in Use

```bash
# Find what's using port 8443
lsof -i :8443

# Kill it
kill -9 <PID>

# Or change docker-compose.yml ports
```

### Problem: Build Fails

```bash
# Build with fresh images (no cache)
docker-compose build --no-cache

# Check logs
docker logs container-name
```

### Problem: Can't Connect to MongoDB

```bash
# Check if mongo is running
docker ps | grep mongo

# Check mongo logs
docker logs buy-01

# Verify connection
docker exec -it buy-01 mongosh --eval "db.adminCommand('ping')"
```

### Problem: Services won't start

```bash
# Check all logs
docker-compose logs

# Restart services
docker-compose down
docker-compose up -d
```

## 📝 What Do I Do Now?

### 1. **Register & Explore** (2 mins)

- Open https://localhost:4200
- Click "Sign Up"
- Choose role: **Seller** (to test all features)
- Create account with email/password

### 2. **Create a Product** (2 mins)

- Go to **Seller Dashboard**
- Click **Create Product**
- Fill in name, description, price
- Save product

### 3. **Upload Product Images** (2 mins)

- Go to **Media Management**
- Upload an image (JPG/PNG, max 2MB)
- Use it in your product

### 4. **Test as Client** (2 mins)

- Logout and create a **Client** account
- Browse products
- View product details

### 5. **Check Logs** (1 min)

```bash
docker-compose logs -f api-gateway
```

### 6. **Monitor Code Quality** (Optional)

- Open http://localhost:9000
- Login: admin / admin
- Run analysis (see README.md for details)

## 🎯 Success Indicators

You'll know everything works when:

✅ All containers running: `docker ps` shows 13+ containers  
✅ Frontend loads: https://localhost:4200 (may warn about SSL)  
✅ Can register account: Sign up page works  
✅ Can create product: Seller dashboard loads  
✅ Can upload image: Media upload succeeds  
✅ Can browse products: Products appear on home page  
✅ Services healthy: `curl http://localhost:8761` returns Eureka page

## 💡 Pro Tips

```bash
# Follow all logs in real-time
docker-compose logs -f

# Build with progress
docker-compose build --progress=plain

# See what's using ports
netstat -an | grep LISTEN

# Clean Docker completely (warning: removes all Docker data)
docker system prune -a --volumes --force

# Run a service locally instead of Docker
cd backend/user-service
mvn spring-boot:run

# Test API endpoint
curl -k https://localhost:8443/actuator/health

# Access database shell
docker exec -it buy-01 mongosh --username admin --password password
```

## 🤔 FAQ

**Q: Do I need SonarQube?**  
A: No, it's optional. Run `make all` to skip it. Use it later if you want code quality metrics.

**Q: Do I need Jenkins?**  
A: No, it's optional. It's for CI/CD automation. Use it later if you want automated pipelines.

**Q: Can I run services locally without Docker?**  
A: Yes! See the README.md "Local Development" section.

**Q: How do I change ports?**  
A: Edit `docker-compose.yml` and change port mappings.

**Q: How do I use my own database?**  
A: Edit environment variables in `docker-compose.yml` under each service.

**Q: Where are images stored?**  
A: In `backend/media-service/uploads/` directory.

## 📚 More Information

- **README.md** - Complete documentation
- **FIXES_APPLIED.md** - Technical details of recent fixes
- **Jenkinsfile** - CI/CD pipeline definition
- **docker-compose.yml** - Service configuration
- **Makefile** - Build automation

## 🆘 Need Help?

1. Check logs: `docker-compose logs -f`
2. Read README.md Troubleshooting section
3. Check FIXES_APPLIED.md for recent changes
4. Check Docker Desktop Dashboard for visual container status

---

**Last Updated**: January 5, 2026  
**Status**: ✅ All systems operational  
**Ready to Start**: Yes! Run `make all`
