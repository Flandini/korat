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
    this.explored = 1;
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
    explored += calculatePruneSpace(currentCV, currentAccessed);

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

    long numChoices = getNumFieldElements(0);

    for (int i = 1; i < sizeCV; ++i) {
      numChoices *= getNumFieldElements(i);
    }

    return numChoices;
  }

  private long calculatePruneSpace(final int[] cv, final int[] accessedFields) {
    if (prevAccessed == null || prevCV == null) {
      return 0;
    }
    //int[] accessed = accessedFields.length >= prevAccessed.length ? accessedFields : prevAccessed;
    int[] accessed = prevAccessed;

    int lastAccessed = accessed[accessed.length - 1];
    long choicesSkipped = 0;

    for (int accessedIdx = accessed.length - 1; accessedIdx >= 0; --accessedIdx) {
      if (cv[accessedIdx] - prevCV[accessedIdx] == 1)
        break;

      if (cv[accessedIdx] == 0) {
        printIntArray(prevCV);
        printIntArray(cv);
        long numFieldsSkipped = getNumFieldElements(accessedIdx) - prevCV[accessedIdx] - 1;

        if (numFieldsSkipped > 0) {
          System.out.println("Isomorphism break!");
          int[] prefix = new int[accessedIdx];
          System.arraycopy(accessed, 0, prefix, 0, accessedIdx);

          long reached = calculateReachSpace(prevCV, prefix);
          System.out.println("Reached: " + reached);
          printIntArray(prevCV);
          printIntArray(prefix);
          choicesSkipped += (reached * numFieldsSkipped);
        }
      }
    }
    System.out.println("Skipped: " + choicesSkipped + " choices.");

    return choicesSkipped;
  }

  private int getNumFieldElements(int idx) {
    return cradle.getStateSpace().getFieldDomain(idx).getNumberOfElements();
  }

  private long calculateReachSpace(final int[] cv, final int[] accessedFields) {
    long choicesSkipped = 1;

    for (int i = 0; i < cv.length; ++i) {
      if (!listContainsInt(accessedFields, i)) {
        choicesSkipped *= getNumFieldElements(i);
      }
    }

    return choicesSkipped;
  }

  private boolean listContainsInt(int[] list, int idx) {
    for (int i = 0; i < list.length; ++i) {
      if (list[i] == idx) {
        return true;
      }
    }
    return false;
  }

  // Print related
  private void printProgressBar() {
    final long progress = calculateProgress();
    assert (progress <= 100.0) : "NumLeft incorrect";

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
    return ((long) Math.floor(percentProgress));
  }

  private void printIntArray(int[] cv) {
    for (int i = 0; i < cv.length; ++i) {
      System.out.print(cv[i]);
    }
    System.out.println();
  }
}
