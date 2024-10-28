TAG?=2.4.1
VERSION:=2.4.1
NAME:=sonic-server-controller
DOCKER_REPOSITORY:=blacklee123
DOCKER_IMAGE_NAME:=$(DOCKER_REPOSITORY)/$(NAME)
GIT_COMMIT:=$(shell git describe --dirty --always)
EXTRA_RUN_ARGS?=

build:
	mvn clean package
	docker build --platform=linux/amd64 -f sonic-server-controller/Dockerfile -t $(DOCKER_IMAGE_NAME):$(VERSION) .

push:
	docker tag $(DOCKER_IMAGE_NAME):$(VERSION) $(DOCKER_IMAGE_NAME):latest
	docker push $(DOCKER_IMAGE_NAME):$(VERSION)
	docker push $(DOCKER_IMAGE_NAME):latest
