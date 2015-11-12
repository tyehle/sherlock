SOURCE=$(wildcard src/*)

sherlock: $(SOURCE)
	mkdir --parents out/production/sherlock
	javac -cp lib/*:. -d out/production/sherlock/ src/cs/utah/sherlock/*.java

run:
	java -cp lib/*:out/production/sherlock cs.utah.sherlock.Driver

test: sherlock
	./test.sh

clean:
	rm -rfv out
