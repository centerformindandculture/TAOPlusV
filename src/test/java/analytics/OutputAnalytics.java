package analytics;

import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;
import simudyne.core.exec.runner.ModelRunner;
import simudyne.core.exec.runner.RunResult;
import simudyne.core.exec.runner.RunnerBackend;
import simudyne.core.exec.runner.definition.BatchDefinitionsBuilder;
import tau.TAUModel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This test is not really a test. This test performs batch runs and then writes the output variable means and the
 * standard deviations to a file in the project directory. You can easily adjust the runs at the top of the test file.
 * Default parameters are used for the runs other than nAgents.
 */
public class OutputAnalytics {

  @Ignore
  @Test
  public void generateOutputAnalytics() {
    RunnerBackend runnerBackend = RunnerBackend.create();

    long[] nAgentsArr = {2000, 4000};
    int nRuns = 30;
    int nTicks = 100;
    String outputFileName = "OutputAnalyticsOutput.csv";

    File f = new File(outputFileName);
    try (BufferedWriter output = new BufferedWriter(new FileWriter(f))) {
      output.append(
          "nAgents,nRuns,cumulativeInfectionsMean,cumulativeInfectionsStdDev,peakNumInfectedMean,"
              + "peakNumInfectedStdDev,"
              + "cumulativeDeathMean,cumulativeDeathStdDev\n");

      for (long nAgents : nAgentsArr) {
        ModelRunner modelRunner = runnerBackend.forModel(TAUModel.class);
        modelRunner.forRunDefinitionBuilder(
            BatchDefinitionsBuilder.create()
                .forRuns(nRuns)
                .forTicks(nTicks)
                .forGeneratorSeed(1234L)
                .withInput("system", ImmutableMap.<String, Object>of("nAgents", nAgents)));

        RunResult result = modelRunner.run().awaitResult();

        double cumulativeMean = result.get("cumulativeInfections").getStatsAtTick(nTicks).getMean();
        double cumulativeStdDev =
            result.get("cumulativeInfections").getStatsAtTick(nTicks).getStandardDeviation();

        double peakMean = result.get("peakNumInfected").getStatsAtTick(nTicks).getMean();
        double peakStdDev =
            result.get("peakNumInfected").getStatsAtTick(nTicks).getStandardDeviation();

        double deadMean = result.get("cumulativeDeath").getStatsAtTick(nTicks).getMean();
        double deadStdDev =
            result.get("cumulativeDeath").getStatsAtTick(nTicks).getStandardDeviation();

        output.append(
            String.format(
                "%d,%d,%f,%f,%f,%f,%f,%f\n",
                nAgents,
                nRuns,
                cumulativeMean,
                cumulativeStdDev,
                peakMean,
                peakStdDev,
                deadMean,
                deadStdDev));
        output.flush();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }


  }
}
