package korat.utils;

import java.lang.Math;

import korat.testing.ITestCaseListener;
import korat.testing.impl.TestCradle;

import korat.utils.IIntList;

import korat.finitization.impl.StateSpace;
import korat.finitization.impl.FieldDomain;

public class ProgressBarPrinter implements ITestCaseListener {
  long totalToExplore;
  long explored;

  private TestCradle cradle;
  private long numFields;

  private int[] prevCV;
  private int[] prevAccessed;

  private boolean started;

  public ProgressBarPrinter() {
    this.started = false;
    this.totalToExplore = 0;
    this.explored = 0;
    this.cradle = TestCradle.getInstance();
  }

  public void notifyNewTestCase(final Object testCase) {
    if (!started) {
      initPrinting();
    }
  }

  public void notifyTestFinished(final long numOfExplored, final long numOfGenerated) {
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

  private void print() {
    final long progress = calculateProgress();

    System.out.print("\r");
    System.out.print("[");
    printTicks(progress);
    printTicks(100 - progress);
    System.out.print("]");
  }

  private void printSpaces(long numLeft) {
    System.out.print(" ");
    printSpaces(numLeft - 1);
  }

  private void printTicks(long numLeft) {
    System.out.print("=");
    printTicks(numLeft - 1);
  }

  private long calculateProgress() {
    double percentProgress = 100 * ((double) explored) / ((double) totalToExplore);
    long numberTicks = (long) percentProgress;
    return numberTicks;
  }
}
