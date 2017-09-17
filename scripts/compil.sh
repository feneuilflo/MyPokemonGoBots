#!/bin/bash

cd ..

libPokeGoAPI="PokeGOAPI-library-all-0.4.1.jar"
if [ -f "./lib/$libPokeGoAPI" ]
then
	echo "./lib/$libPokeGoAPI found."
else
	echo "./lib/$libPokeGoAPI not found -> exit"
	exit
fi


# find .java files and list in file source.txt
find -name "*.java" > sources.txt

# compile .java files in .class files (output folder "target" must exist, and PokeGOAPI-library-all-0.4.1.jar must be in the current folder)
javac.exe -cp "PokeGOAPI-library-all-0.4.1.jar" -d ./target @sources.txt

# create jar from .class files
jar.exe cvf bot.jar -C target .