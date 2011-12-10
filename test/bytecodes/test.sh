#!/bin/bash

echo "Test de generacion de bytecodes validos...";

for i in `find | grep ~` 
do
	rm $i
done
cp -r ../../bin/system .

# Compilar los modulos que no tienen main
for i in `find | grep gluck | grep only_obj | grep -v .svn` 
do
	echo $i
	java -jar ../../dcc.jar -C ${i:2:${#i}} | grep -v Generated | grep -v DEBUG
done

# Iterar sobre los archivos y comparar con la salida esperada.
for i in `find | grep gluck | grep success | grep -v .svn | grep -v only_obj` 
do
	echo $i
	java -jar ../../dcc.jar -C ${i:2:${#i}} | grep -v Generated | grep -v DEBUG
	#(java -jar ../../dcc.jar -C ${i:2:${#i}} 1>&1) > /dev/null
	java ${i:2:${#i}-8} > ${i:2:${#i}-8}.output
	diff ${i:2:${#i}-8}.output ${i:0:${#i}-6}.txt
	sleep 0.01;
done

echo "Test de generacion de bytecodes invalidos...";

for i in `find | grep gluck | grep fail | grep -v .svn | grep -v only_obj`
do
	echo $i
	java -jar ../../dcc.jar -C ${i:2:${#i}} | grep -v Generated | grep -v DEBUG
	java ${i:2:${#i}-8} 2> ${i:2:${#i}-8}.output

	excepciones=`cat ${i:2:${#i}-8}.output | grep Exception`;
	if [ "$excepciones" = "" ]; then
		echo -e "ERROR"
	fi
done

# Borrar todos los archivos generados
for i in `find | grep .class` 
do
	rm $i
done
for i in `find | grep .j$` 
do
	rm $i
done
for i in `find | grep .output` 
do
	rm $i
done
(rm -fr system 2>&1) > /dev/null
