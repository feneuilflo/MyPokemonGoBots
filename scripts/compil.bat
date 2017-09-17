cd ..
dir /s /B *.java > sources.txt
javac.exe -cp "PokeGOAPI-library-all-0.4.1.jar" -d ./target @sources.txt

jar.exe cvf bot.jar -C target .

pause