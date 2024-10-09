TAG?=2.4.1
VERSION:=2.4.1
NAME:=sonic-client-web
DOCKER_REPOSITORY:=blacklee123
DOCKER_IMAGE_NAME:=$(DOCKER_REPOSITORY)/$(NAME)
GIT_COMMIT:=$(shell git describe --dirty --always)
EXTRA_RUN_ARGS?=

build:
	mvn clean package
	docker build --platform=linux/amd64 -f sonic-server-controller/Dockerfile -t $(DOCKER_REPOSITORY)/sonic-server-controller:$(VERSION) .

push:
	docker tag $(DOCKER_REPOSITORY)/sonic-server-controller:$(VERSION) $(DOCKER_REPOSITORY)/sonic-server-controller:latest
	#docker push $(DOCKER_REPOSITORY)/sonic-server-controller:$(VERSION)
	#docker push $(DOCKER_REPOSITORY)/sonic-server-controller:latest
