package korat.utils;

import java.lang.Math;
import java.util.ArrayList;

import korat.testing.ITestCaseListener;
import korat.testing.impl.TestCradle;

import korat.utils.IIntList;

import korat.finitization.impl.StateSpace;
import korat.finitization.impl.FieldDomain;

public class ProgressBarPrinter implements ITestCaseListener {
  private long totalToExplore;
  private long explored;

  private TestCradle cradle;
  private long numFields;

  private int[] prevCV;
  private int[] prevAccessed;

  private boolean started;

  public ProgressBarPrinter() {
    this.started = false;
    this.totalToExplore = 0;
    this.explored = 0;
  }

  public ProgressBarPrinter(TestCradle cradle) {
    this();
    this.cradle = cradle;
  }

  public void notifyNewTestCase(final Object testCase) {
    if (!started) {
      initPrinting();
    }

    int[] currentCV = getCurrentCV();
    int[] currentAccessed = getCurrentAccessedFields();

    explored += calculateReachSpace(currentCV, currentAccessed);

    printProgressBar();

    prevCV = currentCV;
    prevAccessed = currentAccessed;
  }

  public void notifyTestFinished(final long numOfExplored, final long numOfGenerated) {
    System.out.println(); // Necessary to go to the next line after final progress bar
  }

  private void initPrinting() {
    totalToExplore = getTotalNumberOfChoices();
    System.out.println("Num choices: " + totalToExplore);
    started = true;
  }

  private int[] getCurrentAccessedFields() {
    return cradle.getAccessedFields().toArray();
  }

  private int[] getCurrentCV() {
    return cradle.getCandidateVector();
  }

  private long getTotalNumberOfChoices() {
    StateSpace space = cradle.getStateSpace();
    int sizeCV = space.getStructureList().length;

    if (sizeCV == 0) {
      return -1;
    }

    long numChoices = space.getFieldDomain(0).getNumberOfElements();

    for (int i = 1; i < sizeCV; ++i) {
      numChoices *= space.getFieldDomain(i).getNumberOfElements();
    }

    return numChoices;
  }

  private long calculateReachSpace(final int[] cv, final int[] accessedFields) {
    long choicesSkipped = 1;
    ArrayList<Integer> accessed = new ArrayList<Integer>(accessedFields.length);

    for (int field : accessedFields)
      accessed.add(field);

    for (int cvIdx : cv) {
      if (!accessed.contains((Integer) cvIdx)) {
        choicesSkipped *= cradle.getStateSpace().getFieldDomain(cvIdx).getNumberOfElements();
      }
    }

    return choicesSkipped;
  }

  // Print related
  private void printProgressBar() {
    final long progress = calculateProgress();
    assert progress <= 100 : "NumLeft incorrect";

    System.out.print("\r");
    System.out.print("[");
    printTicks(progress);
    printSpaces(100 - progress);
    System.out.print("]");
  }

  private void printSpaces(long numLeft) {
    for (int i = 0; i < numLeft; ++i) {
      System.out.print(" ");
    }
  }

  private void printTicks(long numLeft) {
    for (int i = 0; i < numLeft; ++i) {
      System.out.print("=");
    }
  }

  private long calculateProgress() {
    double percentProgress = 100.0 * explored / totalToExplore;
    return ((long) Math.floor(100 * percentProgress));
  }
}
