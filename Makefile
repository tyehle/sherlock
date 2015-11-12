SOURCE=$(wildcard src/*)

sherlock: $(SOURCE)
	javac -cp lib/*:. -d out/production/sherlock/ src/cs/utah/sherlock/*.java

run:
	java -cp lib/*:out/production/sherlock cs.utah.sherlock.Driver

test: sherlock
	./test.sh

clean:
	rm -rfv out/production/sherlock/cs
