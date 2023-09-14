package core;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import simudyne.core.exec.runner.*;
import simudyne.core.exec.runner.definition.BatchDefinitionsBuilder;
import tau.TAUModel;

import static com.google.common.truth.Truth.assertThat;

@RunWith(Parameterized.class)
public class DeterminismTests {

  // This is just to repeat the test 10 times. Sometimes the test will pass, but fail after a few
  // reruns.
  @Parameterized.Parameters
  public static Object[][] data() {
    return new Object[6][0];
  }

  @Test
  public void testDeterminism() {
    RunnerBackend localBackend = new LocalRunnerBackend();
    ModelRunner modelRunner = localBackend.forModel(TAUModel.class);
    ImmutableMap<String, Object> valueMap =
        ImmutableMap.of("nAgents", 900);
    modelRunner.forRunDefinitionBuilder(
        BatchDefinitionsBuilder.create()
            .withInput("system", valueMap)
            .forRuns(20)
            .forTicks(100)
            .forSeeds(
                1234L, 1234L, 1234L, 1234L, 1234L, 1234L, 1234L, 1234L, 1234L, 1234L, 1234L, 1234L,
                1234L, 1234L, 1234L, 1234L, 1234L, 1234L, 1234L, 1234L));

    MultirunController controller = modelRunner.run();

    RunResult result = controller.awaitResult();

    assertThat(result.get("cumulativeInfections").getStatsAtTick(100L).getN()).isEqualTo(20);
    System.out.println(result.get("cumulativeInfections").getStatsAtTick(100L));
    assertThat(result.get("cumulativeInfections").getStatsAtTick(100L).getStandardDeviation())
        .isEqualTo(0.0);
  }
}
