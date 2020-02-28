#!/usr/bin/bash

for i in {1..10}; do
  java -Xmx1024M -Xms1024M -noverify -cp .:./ProgressBarPrinter.class:lib/commons-cli-1.0.jar:lib/commons-math3-3.6.1.jar:lib/javassist.jar:dist/korat.jar:lib/gson-2.8.6.jar korat.Korat --class korat.examples.singlylinkedlist.SinglyLinkedList --args ${i};
done
