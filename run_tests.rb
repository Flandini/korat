#!/usr/bin/ruby

require 'json'

command = "java -Xmx1024M -Xms1024M -noverify -cp "
command << ".:./ProgressBarPrinter.class:lib/commons-cli-1.0.jar:"
command << "lib/commons-math3-3.6.1.jar:lib/javassist.jar:dist/korat.jar:"
command << "lib/gson-2.8.6.jar korat.Korat "
command << "--class "

classes = {
  "korat.examples.binarytree.BinaryTree": 10,
  "korat.examples.searchtree.SearchTree": 10,
  # "korat.examples.sortedlist.SortedList", 2 args finitization only
  "korat.examples.singlylinkedlist.SinglyLinkedList": 10,
  "korat.examples.redblacktree.RedBlackTree": 10,
  "korat.examples.heaparray.HeapArray": 10,
  "korat.examples.fibheap.FibonacciHeap": 7,
  "korat.examples.doublylinkedlist.DoublyLinkedList": 10,
  "korat.examples.disjset.DisjSet": 6,
  "korat.examples.dag.DAG": 7,
  "korat.examples.binheap.BinomialHeap": 10
}

def run_one(fq_classname, command, max_fin_bound)
  current_command = "#{command}#{fq_classname} --args #{max_fin_bound-1}"
  verbose_run_shell_command(current_command)
  
  current_command = "#{command}#{fq_classname} --args #{max_fin_bound-2}"
  verbose_run_shell_command current_command
  
  # current_command = "#{command}#{fq_classname} --args #{max_fin_bound}"
  # verbose_run_shell_command current_command
end

def verbose_run_shell_command(txt_cmd)
  STDERR.puts txt_cmd
  results = `#{txt_cmd}`
  STDERR.puts results
end

classes.each { |fq_classname, max_fin_bound| run_one(fq_classname, command, max_fin_bound) }
