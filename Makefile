DOCKER_COMPOSE=docker compose

ps:
	${DOCKER_COMPOSE} ps

up:
	${DOCKER_COMPOSE} up -d --build

down:
	${DOCKER_COMPOSE} down