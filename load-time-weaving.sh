#!/bin/bash

function ensure-dir-exists {
    COUNTER=1
    CURRENT_DIR=
    while :
    do
        DIR=`echo "$1" | cut -d'/' -f $COUNTER`
        test "$DIR" = "" && break
        CURRENT_DIR=${CURRENT_DIR}${DIR}/
        mkdir $CURRENT_DIR 2>/dev/null
        COUNTER=`expr $COUNTER + 1`
    done
}

# Prepare
echo "Preparing the environment..."
rm -rf ./target 2>/dev/null
JAR_DIR=./target/classes/post-compile-time
ensure-dir-exists $JAR_DIR

CLASSES_DIR=./target/classes/pure
ensure-dir-exists $CLASSES_DIR

CLASSPATH=./src/main/java
for i in 'aspectjtools.jar' 'aspectjrt.jar'
do
    CLASSPATH=$CLASSPATH:./src/main/resources/$i
done

# Compile the sources
echo "Compiling..."
javac -classpath $CLASSPATH -g -d $CLASSES_DIR src/main/java/edu/cseusf/poco/poco_demo/WeavingAJ.java

echo "Weaving aspect..."
java -cp $CLASSPATH org.aspectj.tools.ajc.Main -source 1.5 -inpath $CLASSES_DIR -aspectpath ./src/main/java -outjar $JAR_DIR/test.jar

# Run the example and check that aspect logic is applied
echo "Running the sample..."
java -cp $CLASSPATH:$JAR_DIR/test.jar com.aspectj.TestTarget