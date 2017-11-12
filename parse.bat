setlocal
set C=bin
set P=saka1029.terrain
set O=-server -Djava.util.logging.config.file=logging.properties
java -cp %C% %O% %P%.Parse
endlocal
