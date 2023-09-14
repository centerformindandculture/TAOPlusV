package analytics;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import simudyne.core.exec.runner.ModelRunner;
import simudyne.core.exec.runner.RunResult;
import simudyne.core.exec.runner.RunnerBackend;
import simudyne.core.exec.runner.definition.BatchDefinitionsBuilder;
import tau.TAUModel;

import java.time.Duration;

import static com.google.common.truth.Truth.assertThat;

/**
 * This test files is for checking that changes did not affect the output of the model unless we expected them to.
 *
 * <p>Any change that affects the output of the model should be documented here with a short explanation. We should aim
 * to have to change this file as little as possible, but there will be times when it is expected for there to be a
 * change in outputs. For example, model paradigms which slightly change random number draws, or new features to the
 * model.
 * Changes in outputs:
 * <ul>
 *   <li>
 *     12/7/2020 Initial outputs after decentralization of model into places.
 *   </li>
 *   <li>
 *     12/7/2020 Add non-infectious exposed period. This had the following effect on outputs: reducing cumulative
 *     infected mean, increasing cumulative infected standard deviation, decreasing peak infected mean, decreasing peak
 *     infected standard deviation, decreasing death mean.
 *   </li>
 *   <li>
 *     12/15/2020 Add student parties. This had the following effent on outputs:
 *     Cumulative Mean 592.9333333333333 -> 766.8666666666667
 *     Cumulative StdDev 39.90845847024493 -> 38.36731127812637
 *     Peak Mean 173.33333333333334 -> 173.83333333333334
 *     Peak StdDev 14.728188626911493 -> 16.630017175708744
 *     Dead Mean 6.733333333333333 -> 8.433333333333334
 *     Dead StdDev 2.899861272940285 -> 2.0956989342433006
 *   </li>
 *   <li>
 *     12/16/2020 Update contact tracing. New contact tracing is more realistic in most ways, except in this iteration,
 *     it allows for many people to quarantine from a single positive test result. Previously only up to 6 people per
 *     place would quarantine, but now a whole class could be told to quarantine if someone tests positive. This
 *     causes improved outputs accross the board.
 *     cumulative infections and peak infections and death.
 *     Cumulative Mean 766.8666666666667 -> 682.0666666666667
 *     Cumulative StdDev 38.36731127812637 -> 30.503740113624428
 *     Peak Mean 173.83333333333334 -> 170.16666666666666
 *     Peak StdDev 16.630017175708744 -> 15.015126472611069
 *     Dead Mean 8.433333333333334 -> 7.4
 *     Dead StdDev 2.0956989342433006 -> 2.5677039253535177
 *   </li>
 *   <li>
 *     12/18/2020 Slight fix in contact tracing
 *     Cumulative Mean 766.8666666666667 -> 678.6333333333333
 *     Cumulative StdDev 38.36731127812637 -> 30.411526096863835
 *     Peak Mean 173.83333333333334 -> 170.3
 *     Peak StdDev 16.630017175708744 -> 14.939302481062818
 *     Dead Mean 8.433333333333334 -> 7.2
 *     Dead StdDev 2.0956989342433006 -> 2.771778963173228
 *   </li>
 *   <li>
 *    1/4/2021 Omit some placetypes from contact tracing
 *    Cumulative Mean 678.6333333333333 -> 689.6666666666666
 *    Cumulative StdDev 30.411526096863835 -> 38.06286391215725
 *    Peak Mean 170.3 -> 169.66666666666666
 *    Peak StdDev 14.939302481062818 -> 19.032337876820893
 *    Dead Mean 7.2 -> 8.333333333333334
 *    Dead StdDev 2.771778963173228 -> 3.1767835971291
 *  </li>
 *  <li>
 *    1/15/2021 Add test inaccuracy and longer positive period after recovery
 *    Cumulative Mean 689.6666666666666 -> 672.1333333333333
 *    Cumulative StdDev 38.06286391215725 -> 39.159033249781224
 *    Peak Mean 169.66666666666666 -> 170.7
 *    Peak StdDev 19.032337876820893 -> 16.07986532796582
 *    Dead Mean 8.333333333333334 -> 7.533333333333333
 *    Dead StdDev 3.1767835971291 -> 3.4713937709000513
 *  </li>
 *  <li>
 *   1/15/2021 Adding OutputWriterAgent in model setup seems to have changed randomization
 *   so there is a slight difference here but it doesn't affect the expected outcome
 *   Cumulative Mean 672.1333333333333 -> 678.8666666666667
 *   Cumulative StdDev  39.159033249781224 -> 39.819189043625116
 *   Peak Mean 170.7 -> 174.76666666666668
 *   Peak StdDev 16.07986532796582 -> 16.796311855702815
 *   Dead Mean 7.533333333333333 -> 8.2
 *   Dead StdDev 3.4713937709000513 -> 3.2842361207062627
 * </li>
 * <li>
 *  *   1/22/2021 A certain percentage of students attend sport events
 *  *   Cumulative Mean 678.8666666666667 -> 631.2333333333333
 *  *   Cumulative StdDev  39.819189043625116 -> 47.32695035479317
 *  *   Peak Mean 174.76666666666668 -> 163.76666666666668
 *  *   Peak StdDev 16.796311855702815 -> 18.26940788224446
 *  *   Dead Mean 8.2 -> 7.066666666666666
 *  *   Dead StdDev 3.2842361207062627 -> 2.753472207123634
 *  * </li>
 * </ul>
 */
public class OutputTests {
  @Test
  public void checkOutputsAreExpected() {
    RunnerBackend runnerBackend = RunnerBackend.create();

    long nAgents = 2000;
    int nRuns = 30;
    int nTicks = 100;

    ModelRunner modelRunner = runnerBackend.forModel(TAUModel.class);
    modelRunner.forRunDefinitionBuilder(
        BatchDefinitionsBuilder.create()
            .forRuns(nRuns)
            .forTicks(nTicks)
            .forGeneratorSeed(1234L)
            .withInput("system", ImmutableMap.<String, Object>of("nAgents", nAgents)));


    long startTimeMillis = System.currentTimeMillis();
    RunResult result = modelRunner.run().awaitResult();
    Duration runTime = Duration.ofMillis(System.currentTimeMillis() - startTimeMillis);
    System.out.println(String.format("%d runs of %d agents took %s to run", nRuns, nAgents,
        runTime.toString()
            .substring(2)
            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
            .toLowerCase()));

    double cumulativeMean = result.get("cumulativeInfections").getStatsAtTick(nTicks).getMean();
    double cumulativeStdDev =
        result.get("cumulativeInfections").getStatsAtTick(nTicks).getStandardDeviation();

    double peakMean = result.get("peakNumInfected").getStatsAtTick(nTicks).getMean();
    double peakStdDev =
        result.get("peakNumInfected").getStatsAtTick(nTicks).getStandardDeviation();

    double deadMean = result.get("cumulativeDeath").getStatsAtTick(nTicks).getMean();
    double deadStdDev =
        result.get("cumulativeDeath").getStatsAtTick(nTicks).getStandardDeviation();

    double acceptableDelta = 0.0001;
    assertThat(cumulativeMean).isWithin(acceptableDelta).of(631.2333333333333);
    assertThat(cumulativeStdDev).isWithin(acceptableDelta).of(47.32695035479317);
    assertThat(peakMean).isWithin(acceptableDelta).of(163.76666666666668);
    assertThat(peakStdDev).isWithin(acceptableDelta).of(18.26940788224446);
    assertThat(deadMean).isWithin(acceptableDelta).of(7.066666666666666);
    assertThat(deadStdDev).isWithin(acceptableDelta).of(2.753472207123634);
  }

  /**
   * This main method is here to let us use IntelliJ's debugger.
   * To use it, make a new run configuration with the main class pointed here
   */
  public static void main(String[] args) {
    OutputTests outputTests = new OutputTests();
    outputTests.checkOutputsAreExpected();
  }

}


