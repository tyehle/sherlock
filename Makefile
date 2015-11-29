SOURCE=$(wildcard src/*)

sherlock: $(SOURCE)
	mkdir --parents out/production/sherlock
	javac -cp lib/*:. -d out/production/sherlock/ src/cs/utah/sherlock/*.java

clean:
	rm -rfv out
