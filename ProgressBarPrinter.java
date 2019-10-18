import java.lang.Math;

public class ProgressBarPrinter implements ITestCaseListener {
  long totalToExplore = 0;
  long explored = 0;

  @Override
  public void notifyNewTestCase(final Object testCase) {
    return;
  }

  @Override
  public void notifyTestFinished(final long numOfExplored, final long numOfGenerated) {
    return;
  }

  private void print() {
    final Integer progress = calculateProgress();

    System.out.print("\r");
    System.out.print("[");
    printTicks(progress);
    printTicks(100 - progress);
    System.out.print("]");
  }

  private void printSpaces(int numLeft) {
    System.out.print(" ");
    printSpaces(numLeft - 1);
  }

  private void printTicks(int numLeft) {
    System.out.print("=");
    printTicks(numLeft - 1);
  }

  private Integer calculateProgress() {
    double percentProgress = 100 * ((double) explored) / ((double) totalToExplore);
    Integer numberTicks = Math.floor(percentProgress);
    return percentProgress;
  }
}
