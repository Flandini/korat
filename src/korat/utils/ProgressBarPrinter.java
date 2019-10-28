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

//**************************************************************************************
// Quantifying the Exploration of the Korat Solver for Imperative Constraints
// Alyas Almaawi, Hayes Converse, Milos Gligoric, Sasa Misailovic, and Sarfraz Khurshid
// http://misailo.web.engr.illinois.edu/papers/jpf19-koratcount.pdf
//**************************************************************************************
public class ProgressBarPrinter implements ITestCaseListener {
  private BigInteger explored, totalToExplore;
  private BigInteger pruned, reached;
  private BigInteger numCV;

  private TestCradle cradle;
  private long numFields;

  private int[] prevCV;
  private int[] prevAccessed;

  private boolean started;

  public ProgressBarPrinter() {
    this.started = false;
    this.totalToExplore = BigInteger.ZERO;
    this.explored = BigInteger.ONE;
    this.pruned = BigInteger.ZERO;
    this.reached = BigInteger.ZERO;
    this.numCV = BigInteger.ZERO;
  }

  public ProgressBarPrinter(TestCradle cradle) {
    this();
    this.cradle = cradle;
  }

  //**************************************************************************************
  //
  // Listener interface
  //
  //**************************************************************************************
  public void notifyNewTestCase(final Object testCase) {
    if (!started) {
      initPrinting();
    }

    this.numCV = this.numCV.add(BigInteger.ONE);

    int[] currentCV = getCurrentCV();
    int[] currentAccessed = getCurrentAccessedFields();

    BigInteger curReached = calculateReachSpace(currentCV, currentAccessed);
    BigInteger curPruned = calculatePruneSpace(currentCV, currentAccessed);

    this.pruned = this.pruned.add(curPruned);
    this.reached = this.reached.add(curReached);

    this.explored = this.explored.add(curReached);
    this.explored = this.explored.add(curPruned);

    printProgress();

    prevCV = currentCV;
    prevAccessed = currentAccessed;
  }

  public void notifyTestFinished(final long numOfExplored, final long numOfGenerated) {
    this.pruned = this.pruned.add(totalToExplore.subtract(explored));
    this.explored = this.totalToExplore;

    printProgress();
    System.out.println(); // Necessary to go to the next line after final progress bar
  }

  //**************************************************************************************
  //
  // Helper methods for fields accessed, candidate vectors, intialization, etc.
  //
  //**************************************************************************************
  private void initPrinting() {
    totalToExplore = getTotalNumberOfChoices();
    started = true;
  }

  private int[] getCurrentAccessedFields() {
    return cradle.getAccessedFields().toArray();
  }

  private int[] getCurrentCV() {
    return cradle.getCandidateVector();
  }

  private int getNumFieldElements(int idx) {
    return cradle.getStateSpace().getFieldDomain(idx).getNumberOfElements();
  }

  private BigInteger getTotalNumberOfChoices() {
    StateSpace space = cradle.getStateSpace();
    int sizeCV = space.getStructureList().length;

    if (sizeCV == 0) {
      throw new RuntimeException("CV's have zero length, can't print progress bar");
    }

    BigInteger numChoices = BigInteger.ONE;

    for (int i = 0; i < sizeCV; i++) {
      numChoices = numChoices.multiply(BigInteger.valueOf(getNumFieldElements(i)));
    }

    return numChoices;
  }

  //**************************************************************************************
  //
  // PruneSpace and ReachSpace calculations
  //
  //**************************************************************************************
  private BigInteger calculatePruneSpace(final int[] cv, final int[] accessedFields) {
    BigInteger choicesSkipped = BigInteger.ZERO;

    if (prevAccessed == null || prevCV == null) {
      return choicesSkipped;
    }

    int[] accessed = prevAccessed;

    int lastAccessed = accessed[accessed.length - 1];

    // accessedIdx is idx into list of field indices
    // fieldIdx is the idx into the CV
    // prevF and curF are indices into field domains
    for (int accessedIdx = accessed.length - 1; accessedIdx >= 0; --accessedIdx) {
      int fieldIdx = accessed[accessedIdx];
      int prevF = prevCV[fieldIdx];
      int curF = cv[fieldIdx];

      if (curF - prevF == 1) {
        break;
      }

      if (curF == 0) {
        long skipped = getNumFieldElements(fieldIdx) - prevF - 1;

        if (skipped > 0) {
          int[] prefix = new int[accessedIdx + 1];
          System.arraycopy(accessed, 0, prefix, 0, accessedIdx + 1);

          BigInteger reached = calculateReachSpace(prevCV, prefix);
          choicesSkipped = choicesSkipped.add(reached.multiply(BigInteger.valueOf(skipped)));
        }
      }
    }

    return choicesSkipped;
  }

  private BigInteger calculateReachSpace(final int[] cv, final int[] accessedFields) {
    BigInteger choicesSkipped = BigInteger.ONE;

    ArrayList<Integer> accessed = new ArrayList<Integer>(accessedFields.length);

    for (int i = 0; i < accessedFields.length; ++i)
      accessed.add(accessedFields[i]);

    for (int i = 0; i < cv.length; ++i) {
      if (!accessed.contains((Integer) i)) {
        choicesSkipped = choicesSkipped.multiply(BigInteger.valueOf(getNumFieldElements(i)));
      }
    }

    return choicesSkipped;
  }

  //**************************************************************************************
  //
  // Printing related members and functions
  //
  //**************************************************************************************
  static final int maxTurns = 4;
  static final String[] turns = {"\\", "|", "/", "-"};
  long numCvPerPrint = 1;
  long numCvSinceLastPrint = 0;
  int currentTurnNumber = 0;

  private void printProgress() {
    numCvSinceLastPrint++;
    numCvSinceLastPrint %= numCvPerPrint;

    if (numCvSinceLastPrint != 0) {
      return;
    }

    final long progress = calculateProgress();

    System.out.print("\r");
    printPercentCovered(progress);
    printProgressBar(progress);
    printStatistics();
    printTurnstile();
  }

  private void printPercentCovered(final long percentProgress) {
    System.out.print(" ");
    printPercentage(percentProgress);
    System.out.print("  ");

  }

  private void printProgressBar(final long percentProgress) {
    System.out.print("[");
    printTicks(percentProgress);
    printSpaces(100 - percentProgress);
    System.out.print("]    ");

    System.out.print(explored + " / " + totalToExplore + " ");
  }

  private void printStatistics() {
    long percentPruned = roundedFraction(pruned, explored);
    long percentReached = roundedFraction(reached, explored);

    System.out.print("   ");

    System.out.print("Percent reached:");
    printPercentage(percentReached);

    System.out.print(" / ");

    System.out.print("Percent pruned:");
    printPercentage(percentPruned);
  }

  private void printTurnstile() {
    System.out.print("  ");
    System.out.print(turns[currentTurnNumber++]);
    currentTurnNumber = currentTurnNumber % maxTurns;
    System.out.print("  ");
  }

  private void printPercentage(final long percentage) {
    // Need to place hold space for the 100s place
    if (percentage < 100.0) {
      System.out.print(" ");
    }

    // Need to place hold space for the 10s place
    if (percentage < 10.0) {
      System.out.print(" ");
    }

    System.out.print(percentage + "%");
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

  // Round to first decimal place. Couldn't find a Math.* func for this
  private float roundFloat(final float in) {
    return (float) (Math.round(in * 10) / 10.0);
  }

  private long calculateProgress() {
    return roundedFraction(explored, totalToExplore);
  }

  private long roundedFraction(final BigInteger part, final BigInteger whole) {
    BigDecimal oneHundred = new BigDecimal(100);
    BigDecimal percentProgress;

    percentProgress = oneHundred.multiply((new BigDecimal(part)).divide(new BigDecimal(whole), 3, BigDecimal.ROUND_HALF_EVEN));

    return percentProgress.longValue();
  }

  //**************************************************************************************
  //
  // For debugging only
  //
  //**************************************************************************************
  private void printIntArray(int[] cv) {
    for (int i = 0; i < cv.length; ++i) {
      System.out.print(cv[i]);
    }
    System.out.println();
  }
}
