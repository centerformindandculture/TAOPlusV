package servlet;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.junit.Test;

import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public class TAOServletTest {

  private final TAOServlet servlet = new TAOServlet();
  private final Gson gson = new Gson();

  @Test
  public void testOutput() {
    String result = servlet.exogenousInfectivityAnalysis("{}");

    assertThat(result).isNotEmpty();
    @SuppressWarnings("unchecked")
    Map<String, String> outputMap = gson.fromJson(result, Map.class);
    assertThat(outputMap).containsKey("exogenousInfectionResult");
    try {
      Double.parseDouble(outputMap.get("exogenousInfectionResult"));
    } catch (NumberFormatException e) {
      fail("Output is in improper format.");
    }
  }

  /**
   * We don't have a solid set of expected outputs for certain inputs, so we will not test this
   * exhaustively.
   */
  @Test
  public void testInputAffectsOutput() {
    // A breakpoint of 0.3 should yield a much higher exog infection rate than 0.05
    String highBreakPointResult =
        servlet.exogenousInfectivityAnalysis(
            gson.toJson(ImmutableMap.of("percInfectedBreakPoint", 0.3)));
    String lowBreakPointResult =
        servlet.exogenousInfectivityAnalysis(
            gson.toJson(ImmutableMap.of("percInfectedBreakPoint", 0.05)));

    assertThat(lowBreakPointResult).isNotEmpty();
    assertThat(highBreakPointResult).isNotEmpty();
    @SuppressWarnings("unchecked")
    Map<String, String> lowBreakPointOutputMap = gson.fromJson(lowBreakPointResult, Map.class);
    @SuppressWarnings("unchecked")
    Map<String, String> highBreakPointOutputMap = gson.fromJson(highBreakPointResult, Map.class);
    try {
      double highBreakPointExogInfectRate =
          Double.parseDouble(highBreakPointOutputMap.get("exogenousInfectionResult"));
      double lowBreakPointExogInfectRate =
          Double.parseDouble(lowBreakPointOutputMap.get("exogenousInfectionResult"));
      assertThat(lowBreakPointExogInfectRate).isLessThan(highBreakPointExogInfectRate);
    } catch (NumberFormatException e) {
      fail("Output is in improper format.");
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidInputExogeniousInfectivityStep() {
    servlet.exogenousInfectivityAnalysis(
        gson.toJson(ImmutableMap.of("exogenousInfectivityStep", "notANumber")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidInputPercInfectedBreakPoint() {
    servlet.exogenousInfectivityAnalysis(
        gson.toJson(ImmutableMap.of("percInfectedBreakPoint", "notANumber")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidInputNumRuns() {
    servlet.exogenousInfectivityAnalysis(gson.toJson(ImmutableMap.of("numRuns", "notANumber")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidInputNumTicks() {
    servlet.exogenousInfectivityAnalysis(gson.toJson(ImmutableMap.of("numTicks", "notANumber")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidInputNAgents() {
    servlet.exogenousInfectivityAnalysis(gson.toJson(ImmutableMap.of("nAgents", "notANumber")));
  }

  public static void main(String[] args) {
    TAOServletTest test = new TAOServletTest();
    try {
      test.testOutput();
      test.testInputAffectsOutput();
      test.testInvalidInputExogeniousInfectivityStep();
      test.testInvalidInputPercInfectedBreakPoint();
      test.testInvalidInputNumRuns();
      test.testInvalidInputNumTicks();
      test.testInvalidInputNAgents();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
}
