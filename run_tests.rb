#!/usr/bin/ruby

command = "java -Xmx1024M -Xms1024M -noverify -cp "
command << ".:./ProgressBarPrinter.class:lib/commons-cli-1.0.jar:"
command << "lib/commons-math3-3.6.1.jar:lib/javassist.jar:dist/korat.jar:"
command << "lib/gson-2.8.6.jar korat.Korat --showProgress "
command << "--class " # korat.examples.binarytree.BinaryTree --args "

classes = [
  "korat.examples.binarytree.BinaryTree",
  "korat.examples.searchtree.SearchTree",
  # "korat.examples.sortedlist.SortedList", 2 args finitization only
  "korat.examples.singlylinkedlist.SinglyLinkedList",
  "korat.examples.redblacktree.RedBlackTree",
  "korat.examples.heaparray.HeapArray",
  "korat.examples.fibheap.FibonacciHeap",
  "korat.examples.doublylinkedlist.DoublyLinkedList",
  "korat.examples.disjset.DisjSet",
  "korat.examples.dag.DAG",
  "korat.examples.binheap.BinomialHeap"]

# Max sizes. Those not appearing here should be ran up to 10 times
class_to_size_mappings = {
  "korat.examples.dag.DAG": 7,
  "korat.examples.disjset.DisjSet": 6,
  "korat.examples.fibheap.FibonacciHeap": 7
}

def run_one(fq_classname, command, class_to_size_mappings, classes)
  current_command = command
  current_command << fq_classname
  current_command << " --args"

  finitization_bound = class_to_size_mappings[fq_classname] || 10

  (1..finitization_bound).each do |i|
    this = "#{current_command} #{i} >> run_results.txt 2>&1"
    STDERR.puts this
    results = `#{this}`
    STDERR.puts results
  end
end

classes.each { |fq_classname| run_one(fq_classname, command, class_to_size_mappings, classes) }
