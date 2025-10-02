.PHONY: help install-all install-traefik deploy-event-api clean clean-all status

# Default target
help:
	@echo "Available targets:"
	@echo "  help              - Show this help message"
	@echo "  install-all       - Install Traefik, and deploy Event API"
	@echo "  install-traefik   - Install Traefik ingress controller"
	@echo "  deploy-event-api  - Deploy the Event API application"
	@echo "  status            - Check status of all deployments"
	@echo "  clean             - Remove Event API deployment"
	@echo "  clean-all         - Remove all deployments (Event API, Traefik)"
	@echo "  restart-event-api - Restart Event API pods"
	@echo "  logs-event-api    - Tail Event API logs"

# Install everything in order
install-all: install-traefik deploy-event-api
	@echo "✅ All components installed successfully!"

# Install Traefik
install-traefik:
	@echo "🚀 Installing Traefik..."
	kubectl apply -f treafik/traefik.namespace.yaml
	helm repo add traefik https://traefik.github.io/charts
	helm repo update
	helm install traefik traefik/traefik \
		--namespace traefik \
		--values treafik/traefik-values.yaml
	@echo "⏳ Waiting for Traefik to be ready..."
	kubectl wait --namespace traefik \
		--for=condition=ready pod \
		--selector=app.kubernetes.io/name=traefik \
		--timeout=90s
	@echo "✅ Traefik installed successfully"

# Deploy Event API
deploy-event-api:
	@echo "🚀 Deploying Event API..."
	kubectl apply -f event-api/event-api.namespace.yaml
	kubectl apply -f event-api/event-api.deployment.yaml
	kubectl apply -f event-api/event-api.service.yaml
	kubectl apply -f event-api/event-api.ingressroute.yaml
	@echo "⏳ Waiting for Event API to be ready..."
	kubectl wait --namespace event-api \
		--for=condition=ready pod \
		--selector=app=event-api \
		--timeout=90s
	@echo "✅ Event API deployed successfully"

# Check status of all components
status:
	@echo ""
	@echo "📊 Checking Traefik status..."
	@kubectl get pods -n traefik 2>/dev/null || echo "Traefik not installed"
	@kubectl get svc -n traefik 2>/dev/null || true
	@echo ""
	@echo "📊 Checking Event API status..."
	@kubectl get pods -n event-api 2>/dev/null || echo "Event API not deployed"
	@kubectl get svc -n event-api 2>/dev/null || true
	@kubectl get ingressroute -n event-api 2>/dev/null || true

# Restart Event API pods
restart-event-api:
	@echo "🔄 Restarting Event API pods..."
	kubectl rollout restart deployment -n event-api
	kubectl rollout status deployment -n event-api

# View Event API logs
logs-event-api:
	@echo "📜 Tailing Event API logs..."
	kubectl logs -n event-api -l app=event-api --tail=100 -f

# Clean Event API only
clean:
	@echo "🧹 Removing Event API..."
	kubectl delete -f event-api/event-api.ingressroute.yaml --ignore-not-found
	kubectl delete -f event-api/event-api.service.yaml --ignore-not-found
	kubectl delete -f event-api/event-api.deployment.yaml --ignore-not-found
	kubectl delete -f event-api/event-api.namespace.yaml --ignore-not-found
	@echo "✅ Event API removed"

# Clean everything
clean-all: clean
	@echo "🧹 Removing Traefik..."
	helm uninstall traefik -n traefik 2>/dev/null || true
	kubectl delete namespace traefik --ignore-not-found
	@echo "✅ All components removed"

# Development helpers
dev-rebuild:
	@echo "🔨 Rebuilding and redeploying Event API..."
	kubectl delete -f event-api/event-api.deployment.yaml --ignore-not-found
	kubectl apply -f event-api/event-api.deployment.yaml
	kubectl wait --namespace event-api \
		--for=condition=ready pod \
		--selector=app=event-api \
		--timeout=90s

# Show ingress information
show-ingress:
	@echo "🌐 Ingress Information:"
	@kubectl get svc -n traefik traefik -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "No LoadBalancer IP assigned yet"

deploy-event-api:
	@echo "Deploying new version of Event API"
	docker build -t event-api:1.0.0 event-api/
	kubectl delete -f event-api/event-api.deployment.yaml --ignore-not-found
	kubectl apply -f event-api/event-api.deployment.yaml
	kubectl rollout restart deploy/event-api -n event-api
# 	kubectl wait --namespace event-api \
# 		--for=condition=ready pod \
# 		--selector=app=event-api \
# 		--timeout=90s
