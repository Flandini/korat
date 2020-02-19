#!/bin/bash
#javac -cp .:build/:build/korat/testing/ ProgressBarPrinter.java
#java -noverify -cp .:./ProgressBarPrinter.class:lib/commons-cli-1.0.jar:lib/javassist.jar::dist/korat.jar korat.Korat --listeners ProgressBarPrinter --class korat.examples.binarytree.BinaryTree --args 3

#javac -cp .:build/:build/korat/testing/ ProgressBarPrinter.java
ant build createJar && \
java -Xmx1024M -Xms1024M -noverify -cp .:./ProgressBarPrinter.class:lib/commons-cli-1.0.jar:lib/javassist.jar:dist/korat.jar:lib/gson-2.8.6.jar korat.Korat --showProgress --class korat.examples.searchtree.SearchTree --args 8
