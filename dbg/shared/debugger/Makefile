compile: 
	javac -classpath .:/jackson/jackson-databind.jar:/jackson/jackson-core.jar:/jackson/jackson-annotations.jar *.java
debug: compile
	java -Xint -classpath .:/jackson/jackson-databind.jar:/jackson/jackson-core.jar:/jackson/jackson-annotations.jar MyDebugger $(MAIN_HOST) $(JDWP_PORT)

clean:
	rm *.class *.log

.PHONY: debug clean
