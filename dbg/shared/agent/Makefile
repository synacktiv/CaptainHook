MAVEN_CLI_OPTS=

agent:
	cd agent && mvn $(MAVEN_CLI_OPTS) package
	cd agent/target && rm -rf unzipped/ && mkdir unzipped
	cd agent/target/unzipped && jar xf ../agent-1.0-SNAPSHOT-jar-with-dependencies.jar
	cd agent/target/unzipped && sed -i 's/Main-Class/Agent-Class/g' META-INF/MANIFEST.MF
	cd agent/target/unzipped && head -n -1 META-INF/MANIFEST.MF > temp.txt; mv temp.txt META-INF/MANIFEST.MF
	cd agent/target/unzipped && echo "Can-Retransform-Classes: true" >> META-INF/MANIFEST.MF
	cd agent/target/unzipped && echo "Can-Redefine-Classes: true" >> META-INF/MANIFEST.MF
	cd agent/target/unzipped && echo "" >> META-INF/MANIFEST.MF
	cd agent/target/unzipped && jar cmf0 META-INF/MANIFEST.MF /workdir/agent/agent.jar *
	rm -r agent/target

	

launcher:
	cd launcher && mvn $(MAVEN_CLI_OPTS) package; cd ..
	cd launcher/target && mv launcher-1.0-SNAPSHOT-jar-with-dependencies.jar /workdir/agent/launcher.jar
	rm -r launcher/target



.PHONY: agent launcher
