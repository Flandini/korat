package korat.utils;

import java.lang.Math;
import java.lang.ArithmeticException;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.ArrayList;

import korat.testing.ITestCaseListener;
import korat.testing.impl.TestCradle;

import korat.utils.IIntList;

import korat.finitization.impl.StateSpace;
import korat.finitization.impl.FieldDomain;

public class ProgressBarPrinter implements ITestCaseListener {
  //private long totalToExplore;
  //private long explored;
  private BigInteger totalToExplore;
  private BigInteger explored;

  private TestCradle cradle;
  private long numFields;

  private int[] prevCV;
  private int[] prevAccessed;

  private boolean started;

  public ProgressBarPrinter() {
    this.started = false;
    this.totalToExplore = BigInteger.ZERO;
    this.explored = BigInteger.ONE;
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

    explored = explored.add(BigInteger.valueOf(calculateReachSpace(currentCV, currentAccessed)));
    explored = explored.add(BigInteger.valueOf(calculatePruneSpace(currentCV, currentAccessed)));

    assert explored.compareTo(totalToExplore) != 1 : "explored states exceeded maximum number of states";

    printProgressBar();

    prevCV = currentCV;
    prevAccessed = currentAccessed;
  }

  public void notifyTestFinished(final long numOfExplored, final long numOfGenerated) {
    System.out.println(); // Necessary to go to the next line after final progress bar
  }

  private void initPrinting() {
    totalToExplore = BigInteger.valueOf(getTotalNumberOfChoices());
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

    long numChoices = 1;

    for (int i = 0; i < sizeCV; ++i) {
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

          int[] prefix = new int[accessedIdx];
          System.arraycopy(accessed, 0, prefix, 0, accessedIdx);

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
    ArrayList<Integer> accessed = new ArrayList<Integer>(accessedFields.length);

    for (int i = 0; i < accessedFields.length; ++i)
      accessed.add(accessedFields[i]);

    for (int i = 0; i < cv.length; ++i) {
      if (!accessed.contains((Integer) i)) {
        choicesSkipped *= getNumFieldElements(i);
      }
    }

    return choicesSkipped;
  }

  /*
   * Printing related members and functions
   */
  static final int maxTurns = 4;
  static final String[] turns = {"\\", "|", "/", "-"};
  static final long cvsPerPrint = 1;
  int currentTurnNumber = 0;
  int cvsSinceLastPrint = 0;

  private void printProgressBar() {
    cvsSinceLastPrint++;
    cvsSinceLastPrint %= cvsPerPrint;

    if (cvsSinceLastPrint != 0) {
      return;
    }

    final long progress = calculateProgress();
    assert (progress <= 100.0) : "percent states covered > 100%";
    //System.out.println("Progress: " + progress);

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
    //System.out.println();
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
    BigDecimal oneHundred = new BigDecimal(100);
    BigDecimal percentProgress;

    try {
      percentProgress = oneHundred.multiply(new BigDecimal(explored)).divide(new BigDecimal(totalToExplore), 3, BigDecimal.ROUND_HALF_EVEN);
    } catch (ArithmeticException ae) {
      percentProgress = new BigDecimal(0);
    }

    return (percentProgress.longValue());
  }

  private void printIntArray(int[] cv) {
    for (int i = 0; i < cv.length; ++i) {
      System.out.print(cv[i]);
    }
    System.out.println();
  }
}
