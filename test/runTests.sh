# Ejecutamos el programa con archivos validos. Ninguno tendria que fallar.

echo "Testeos de archivos validos...";

for i in valid/*.gluck
do
	echo $i;
        java -jar ../dcc.jar -S $i
done

# Ejecutamos el programa con archivos invalidos.  Todos deberian fallar.

echo "Testeos de archivos invalidos...";
for i in invalid/*.gluck
do
	echo $i;
        java -jar ../dcc.jar -S $i;
done

# Ejecutamos los testeos de etapa1

echo "Testeos de la etapa1...";
for i in etapa1/*.gluck
do
	echo $i;
        java -jar ../dcc.jar -S $i;
done

# Ejecutamos tests de dependencias

echo "Testeos de dependencias NO recursivas...";
for i in dependencies/*.gluck
do
	echo $i;
        java -jar ../dcc.jar -D $i;
done


echo "Testeos de dependencias recursivas...";
for i in dependencies/*.gluck
do
	echo $i;
        java -jar ../dcc.jar -D -r $i;
done

echo "Testeos de location...";
for i in locations/*.gluck
do
	echo $i;
        java -jar ../dcc.jar -D -r $i;
done



