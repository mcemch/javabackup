PROG_NAME = JavaBackup
VERSION = 0.1

help:
	@echo
	@echo "usage: make {release|dist|clean|run|help}"
	@echo

release: dist
	mkdir -p release/$(PROG_NAME)
	cp -ar dist/* release/$(PROG_NAME)
	cp cyg* release/$(PROG_NAME)
	cp rsync.exe release/$(PROG_NAME)
	cp -ar license release/$(PROG_NAME)/license
	cd release && zip -r $(PROG_NAME)-$(VERSION).zip $(PROG_NAME)

dist: clean
	ant 

clean:
	ant clean
	rm -rf release

run:
	@cd release/$(PROG_NAME) && java -jar $(PROG_NAME).jar

