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

    long prevExplored = explored;
    explored += calculateReachSpace(currentCV, currentAccessed);
    explored += calculatePruneSpace(currentCV, currentAccessed);

    assert explored <= totalToExplore : "explored states exceeded maximum number of states";
    assert explored > prevExplored : "states covered overflowed";

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

    int[] accessed = prevAccessed;

    int lastAccessed = accessed[accessed.length - 1];
    long choicesSkipped = 0;

    // accessedIdx is idx into list of field indices
    // fieldIdx is the idx into the CV
    // prevF and curF are indices into field domains
    for (int accessedIdx = accessed.length - 1; accessedIdx >= 0; --accessedIdx) {
      int fieldIdx = accessed[accessedIdx];
      int prevF = prevCV[fieldIdx];
      int curF = cv[fieldIdx];

      if (curF - prevF == 1)
        break;

      if (curF == 0) {
        long skipped = getNumFieldElements(fieldIdx) - prevF - 1;

        if (skipped > 0) {

          int[] prefix = new int[accessedIdx + 1];
          System.arraycopy(accessed, 0, prefix, 0, accessedIdx + 1);

          long reached = calculateReachSpace(prevCV, prefix);
          choicesSkipped += (reached * skipped);
        }
      }
    }

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

  /*
   * Printing related members and functions
   */
  static final int maxTurns = 4;
  static final String[] turns = {"\\", "|", "/", "-"};
  static final long cvsPerPrint = 10000;
  int currentTurnNumber = 0;
  int cvsSinceLastPrint = 0;

  private void printProgressBar() {
    cvsSinceLastPrint++;
    cvsSinceLastPrint %= cvsPerPrint;

    if (cvsSinceLastPrint != 0) {
      return;
    }

    final long progress = calculateProgress();
    assert (progress <= 100.0) : "NumLeft incorrect";

    System.out.print("\r");
    System.out.print("[");
    printTicks(progress);
    printSpaces(100 - progress);
    System.out.print("]");

    System.out.print("    ");
    System.out.print(explored + " / " + totalToExplore);
    System.out.print("    ");

    System.out.print(turns[currentTurnNumber++]);
    currentTurnNumber = currentTurnNumber % maxTurns;
    System.out.println();
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
