JAR := $(shell ls target/*.jar | grep -v original)

build-BotHandlerFunction:
	cp $(JAR) $(ARTIFACTS_DIR)/

build-VacancyScannerFunction:
	cp $(JAR) $(ARTIFACTS_DIR)/
