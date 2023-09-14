package servlet;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import core.Globals;
import simudyne.core.annotations.Input;
import simudyne.core.exec.runner.ModelRunner;
import simudyne.core.exec.runner.RunResult;
import simudyne.core.exec.runner.RunnerBackend;
import simudyne.core.exec.runner.definition.BatchDefinitionsBuilder;
import tau.TAUModel;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

@Path("/")
public class TAOServlet {

  public static final ImmutableSet<String> EXOGENOUS_ANALYSIS_EXCLUDE_SYSTEM_INPUTS =
      ImmutableSet.<String>builder()
          .add("overallExternalInfectionRate")
          .add("percInitiallyInfected")
          .add("percInitiallyRecovered")
          .build();

  public static final ImmutableSet<String> EXOGENOUS_ANALYSIS_META_INPUTS =
      ImmutableSet.<String>builder()
          .add("exogenousInfectivityStep")
          .add("percInfectedBreakPoint")
          .add("numRuns")
          .add("numTicks")
          .build();
  public static final ImmutableSet<String> INT_INPUTS;

  static {
    ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
    Class<?> clazz = Globals.class;
    for (Field field : clazz.getDeclaredFields()) {
      if (field.isAnnotationPresent(Input.class) && field.getType().equals(Integer.TYPE)) {
        builder.add(field.getName());
      }
    }
    INT_INPUTS = builder.build();
  }

  // A small step for searching exogenous infectivity values
  public static final double DEFAULT_EXOGENOUS_INFECTIVITY_STEP = 0.0001;
  // The percentage of cumulative infections from exogenous infect + outbreaks we are searching for
  public static final double DEFAULT_PERC_INFECTED_BREAK_POINT = 0.25;
  // 30 should ensure a good level of confidence in the output
  public static final int DEFAULT_NUM_RUNS = 30;
  // Number of days in a semester if tOneDay == 1
  public static final int DEFAULT_NUM_TICKS = 120;

  public static ImmutableMap<String, Object> removeKeys(
      Map<String, Object> map, Set<String> keysToRemove) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (!keysToRemove.contains(entry.getKey())) {
        builder.put(entry);
      }
    }
    return builder.build();
  }

  public static ImmutableMap<String, Object> castInts(
      Map<String, Object> map, Set<String> shouldBeInts) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (!shouldBeInts.contains(entry.getKey())) {
        builder.put(entry);
      } else {
        builder.put(entry.getKey(), ((Double) entry.getValue()).intValue());
      }
    }
    return builder.build();
  }

  public static double findExogenousInfectionRateForWhichCumulativeInfectionPassesBreakPoint(
      Map<String, Object> inputsFromClient,
      double exogenousInfectivityStep,
      double percInfectedBreakPoint,
      int numRuns,
      int numTicks) {
    int nAgents = parseIntIfPresentOrDefault(inputsFromClient, "nAgents", Globals.DEFAULT_N_AGENTS);
    ImmutableMap<String, Object> inputs =
        castInts(
            removeKeys(inputsFromClient, EXOGENOUS_ANALYSIS_EXCLUDE_SYSTEM_INPUTS), INT_INPUTS);

    RunnerBackend runnerBackend = RunnerBackend.create();

    // Expand
    int multiplier = 1;
    while (true) {
      double exogenousInfectionRate = multiplier * exogenousInfectivityStep;
      ImmutableMap<String, Object> allInputs =
          ImmutableMap.<String, Object>builder()
              .putAll(inputs)
              .put("baseOnCampusExternalInfectionRate", exogenousInfectionRate)
              .put("baseOffCampusExternalInfectionRate", exogenousInfectionRate)
              .put("percInitiallyInfected", 0.0)
              .put("percInitiallyRecovered", 0.0)
              .build();
      ModelRunner modelRunner = runnerBackend.forModel(TAUModel.class);
      modelRunner.forRunDefinitionBuilder(
          BatchDefinitionsBuilder.create()
              .forRuns(numRuns)
              .forTicks(numTicks)
              .withInput("system", allInputs));

      RunResult result = modelRunner.run().awaitResult();
      double cumulativeInfections =
          result.get("cumulativeInfections").getStatsAtTick(numTicks).getMean();
      if (cumulativeInfections / nAgents > percInfectedBreakPoint) {
        break;
      }

      multiplier *= 2;
    }

    // Binary search
    int left = multiplier / 2;
    int right = multiplier;
    while (left != right) {
      double exogenousInfectionRate = ((left + right) / 2.0) * exogenousInfectivityStep;
      ImmutableMap<String, Object> allInputs =
          ImmutableMap.<String, Object>builder()
              .putAll(inputs)
              .put("baseOnCampusExternalInfectionRate", exogenousInfectionRate)
              .put("baseOffCampusExternalInfectionRate", exogenousInfectionRate)
              .put("percInitiallyInfected", 0.0)
              .put("percInitiallyRecovered", 0.0)
              .build();
      ModelRunner modelRunner = runnerBackend.forModel(TAUModel.class);
      modelRunner.forRunDefinitionBuilder(
          BatchDefinitionsBuilder.create()
              .forRuns(numRuns)
              .forTicks(120)
              .withInput("system", allInputs));

      RunResult result = modelRunner.run().awaitResult();
      double cumulativeInfections =
          result.get("cumulativeInfections").getStatsAtTick(numTicks).getMean();
      if (cumulativeInfections / nAgents > percInfectedBreakPoint) {
        right = ((left + right) / 2);
      } else {
        left = ((left + right) / 2) + 1;
      }
    }
    return right * exogenousInfectivityStep;
  }

  public static double parseDoubleIfPresentOrDefault(
      Map<String, Object> map, String keyToParse, double defaultVal)
      throws IllegalArgumentException {
    if (map.containsKey(keyToParse)) {
      Object shouldBeDouble = map.get(keyToParse);
      if (shouldBeDouble instanceof Double) {
        return (double) shouldBeDouble;
      }
      if (shouldBeDouble instanceof String) {
        try {
          return Double.parseDouble((String) shouldBeDouble);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(
              String.format(
                  "Improper format given for argument %s. Given value %s",
                  keyToParse, shouldBeDouble.toString()));
        }
      }
      throw new IllegalArgumentException(
          String.format(
              "Argument given for parameter %s is neither a Number or String", keyToParse));
    }
    return defaultVal;
  }

  public static int parseIntIfPresentOrDefault(
      Map<String, Object> map, String keyToParse, int defaultVal) throws IllegalArgumentException {
    return ((Double) parseDoubleIfPresentOrDefault(map, keyToParse, defaultVal)).intValue();
  }

  @Path("/exogenousInfection/")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public String exogenousInfectivityAnalysis(String json) {
    Gson gson = new Gson();
    @SuppressWarnings("unchecked")
    Map<String, Object> inputs = gson.fromJson(json, Map.class);
    double exogenousInfectivityStep =
        parseDoubleIfPresentOrDefault(
            inputs, "exogenousInfectivityStep", DEFAULT_EXOGENOUS_INFECTIVITY_STEP);
    double percInfectedBreakPoint =
        parseDoubleIfPresentOrDefault(
            inputs, "percInfectedBreakPoint", DEFAULT_PERC_INFECTED_BREAK_POINT);
    int numRuns = parseIntIfPresentOrDefault(inputs, "numRuns", DEFAULT_NUM_RUNS);
    int numTicks = parseIntIfPresentOrDefault(inputs, "numTicks", DEFAULT_NUM_TICKS);

    double breakPointExogenousInfectionRate =
        findExogenousInfectionRateForWhichCumulativeInfectionPassesBreakPoint(
            removeKeys(inputs, EXOGENOUS_ANALYSIS_META_INPUTS),
            exogenousInfectivityStep,
            percInfectedBreakPoint,
            numRuns,
            numTicks);
    return gson.toJson(
        ImmutableMap.<String, String>builder()
            .put("exogenousInfectionResult", String.valueOf(breakPointExogenousInfectionRate))
            .build());
  }

  @Path("/exogenousInfection/")
  @OPTIONS
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response exogenousInfectivityAnalysis_opts(String json) {
    return Response.ok().build();
  }

  /**
   * A simple call to confirm that everyhting is running and the server is capable of running the
   * model.
   */
  @Path("/testAPI/")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public String testAPI(String json) {
    @SuppressWarnings("unchecked")
    Map<String, String> inputs = new Gson().fromJson(json, Map.class);

    RunnerBackend runnerBackend = RunnerBackend.create();
    ModelRunner modelRunner = runnerBackend.forModel(TAUModel.class);
    modelRunner.forRunDefinitionBuilder(
        BatchDefinitionsBuilder.create().forRuns(1).forTicks(100).withInput("system", inputs));

    RunResult result = modelRunner.run().awaitResult();

    return new Gson()
        .toJson(
            ImmutableMap.of(
                "simResult", result.get("cumulativeInfections").getAggregatedStats().toString()));
  }

  @Path("/defaultModelRunSingleOutput/")
  @OPTIONS
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response testAPI_opts(String json) {
    return Response.ok().build();
  }
}
