IMAGE_NAME=captainhook-release

build:
	docker build --network="host" -t $(IMAGE_NAME) ./dbg

run-dbg:
	docker run -v $(shell (pwd))/dbg/shared:/workdir:rw -w /workdir -it $(IMAGE_NAME)

.PHONY: build run
