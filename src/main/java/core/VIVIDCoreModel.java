package core;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.Group;
import simudyne.core.abm.Split;
import simudyne.core.annotations.ModelSettings;
import simudyne.core.annotations.Variable;
import simudyne.core.schema.FieldType;
import simudyne.core.schema.SchemaField;
import simudyne.core.schema.SchemaRecord;
import simudyne.core.values.ValueRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.IntStream;

@ModelSettings(timeUnit = "DAYS")
// T must be Globals in implementation. This is a workaround as the SDK doesn't allow longer chains
// of inheritance.
public abstract class VIVIDCoreModel<T> extends AgentBasedModel<Globals> {

  @Variable
  public long peakNumInfected = -1;
  @Variable
  public long cumulativeInfections = 0;
  @Variable
  public long cumulativeDeath = 0;
  @Variable
  public double percPeopleCausing80PercInfections = 0;

  protected long getPeakNumInfected() {
    return peakNumInfected;
  }

  protected long getCumulativeInfections() {
    return cumulativeInfections;
  }

  protected double getPercPeopleCausing80PercInfections() {
    return percPeopleCausing80PercInfections;
  }

  protected abstract void registerPeopleAgentTypes();

  protected abstract void setModules();

  protected abstract void updatePerLocationInfectionData();

  @Override
  public void init() {
    registerPeopleAgentTypes();
    registerAgentTypes(CentralAgent.class, PlaceAgent.class, OutputWriterAgent.class);

    registerLinkTypes(
        Links.PersonToPersonLink.class,
        Links.CentralAgentLink.class,
        Links.SocialLink.class); // Initialisaing these objects so we can generate the graph.

    // Create four long accumulators
    createLongAccumulator("totSusceptible", "Total Susceptible");
    createLongAccumulator("totInfected", "Total Infected");
    createLongAccumulator("totQuarantineInfected", "Total Quarantine Infected");
    createLongAccumulator("totQuarantineSusceptible", "Total Quarantine Susceptible");
    createLongAccumulator("totDead", "Total Dead");
    createLongAccumulator("totRecovered", "Total Recovered");
    createLongAccumulator("totDetectedCases", "Total Detected Cases");
    createDoubleAccumulator("testPositivity", "core.Test Positivity (Pct)");
    createLongAccumulator("numInfectionsThisStep", "Number of people infected on this step");
    createLongAccumulator("numPosTestsThisStep", "Number of positive test results this step");
    createLongAccumulator("numTestsThisStep", "Total number of test results this step");
    createLongAccumulator("numExtInfectionsThisStep", "Total number of external infections this step");
    createLongAccumulator("currentInfected", "Total number of current infected agents");
    createLongAccumulator("currentInfectious", "Total number of current infectious agents");
    createLongAccumulator("posTestsReturnedThisStep", "Total number of positive tests returned this step");
    createLongAccumulator("totTestsReturnedThisStep", "Total number of tests returned this step");

    setModules();
    if (getGlobals().modules == null) {
      throw new IllegalStateException("Global modules must be set.");
    }

    getContext()
        .getChannels()
        .createOutputChannel()
        .setId("infection-output")
        .setSchema(InfectionStatistics.getSchema())
        .addLabel("simudyne:parquet")
        .build();

    SchemaRecord timeSeriesRecord = new SchemaRecord("TimeSeriesOutputs")
            .add(new SchemaField("testsPerDay", FieldType.Long))
            .add(new SchemaField("totaltestsgiven", FieldType.Long))
            .add(new SchemaField("positivetestsgiven", FieldType.Long))
            .add(new SchemaField("totaltestsreturned", FieldType.Long))
            .add(new SchemaField("positivetestsreturned", FieldType.Long))
            .add(new SchemaField("extinfections", FieldType.Long))
            .add(new SchemaField("numNewInfections", FieldType.Long))
            .add(new SchemaField("numInfected", FieldType.Long))
            .add(new SchemaField("numInfectious", FieldType.Long))
            .add(new SchemaField("runID", FieldType.String));

    getContext()
            .getChannels()
            .createOutputChannel()
            .setId("timeseriesoutputs")
            .setSchema(timeSeriesRecord)
            .addLabel("simudyne:parquet")
            .setEnabled(true)
            .build();
  }

  protected abstract List<Group<? extends Person>> generatePeople();

  protected abstract void setupPlaces();

  @Override
  public void setup() {

    setupPlaces();
    List<Group<? extends Person>> personGroups = generatePeople();

    // Initialise Central Agent
    Group<CentralAgent> centralAgentGroup =
        generateGroup(
            CentralAgent.class,
            1,
            ca -> {
              getGlobals().centralAgentID = ca.getID();
            });

    for (Group<? extends Person> personGroup : personGroups) {
      centralAgentGroup.fullyConnected(
          personGroup,
          Links.CentralAgentLink.class); // uni-directional link: Central agent to the student
      personGroup.fullyConnected(centralAgentGroup, Links.CentralAgentLink.class);
    }

    // This generates a single PlaceAgent that will spawn all of the other PlaceAgents
    // in PlaceAgent#receivePlace
    generateGroup(PlaceAgent.class, 1);

    generateGroup(OutputWriterAgent.class, 1, outputWriterAgent -> getGlobals().outputWriterAgentID =
        outputWriterAgent.getID());

    super.setup();
  }

  protected abstract String constructCSVOutput();

  // Operations for each time step
  @Override
  public void step() {

    super.step();

    if (getGlobals().tStep == 0) {
      run(
          Person.sendSelfToCentralAgentForScheduleCreation,
          CentralAgent.initializeConnectionOfAgents);

      run(
          PlaceAgent.sendSelfToCentralAgent,
          CentralAgent.initializePlacesAndAssignSchedules,
          Split.create(Person.receiveSchedule, PlaceAgent.receivePlace));

      run(Split.create(Person.initPerson, PlaceAgent.initPlaceAgent));
      // This must happen after Person.receiveSchedule, as some secondary initialization occurs
      // there
      run(
          Person.sendTestSelectionMultiplierToCentralAgent,
          CentralAgent.receiveTestSelectionMultipliers);

      run(Person.setInitialLocation);
      run(Person.setupInitialInfectionState); // Get infection state & properties for initial agents
      // infected
    }

    getGlobals().resetInfectionStatistics();

    // Infection step is performed first (based on existing links), because the new added link wont
    // be activated in the current time step.
    run(
        Person.executeMovement,
        PlaceAgent.generateContactsAndInfect,
        Split.create(
            Split.create(Person.infectedByCOVID),
            Person.infoExchange,
            Person.infectedSomeoneElseWithCOVID
        ),
        OutputWriterAgent.write);

    // Testing stage: People can reports symptoms and request a test from the central agent
    // Central agent then processes and returns the test results with a predefined lag time
    run(CentralAgent.doRandomizedTesting, Person.getTested, CentralAgent.processInfectionStatus);
    run(CentralAgent.releaseTestResults, Person.getTestPositivity);

    // Counts the current number of infected and infectious agents
    run(Person.countInfected);

    // Steps to determine if an agent succumbs to the disease
    run(Person.recoverOrDieOrStep, CentralAgent.agentsDied);

    // Central agent updates infection statistics for console & parquet outputs
    run(CentralAgent.updateInfectionStatistics);

    // Agent decides where to move next
    run(Person.movePerson);

    run(Person.resetForNextStep);

    updatePerLocationInfectionData();

    // Update outputs
    if (peakNumInfected < getLongAccumulator("totInfected").value()) {
      peakNumInfected = getLongAccumulator("totInfected").value();
    }
    long numNewInfections = getLongAccumulator("numInfectionsThisStep").value();
    cumulativeInfections += numNewInfections;
    cumulativeDeath = getLongAccumulator("totDead").value();

    long posTestsGiv = getLongAccumulator("numPosTestsThisStep").value();
    long totTestsGiv = getLongAccumulator("numTestsThisStep").value();
    long extInfections = getLongAccumulator("numExtInfectionsThisStep").value();

    long posTestsRet = getLongAccumulator("posTestsReturnedThisStep").value();
    long totTestsRet = getLongAccumulator("totTestsReturnedThisStep").value();

    long currentInfected = getLongAccumulator("currentInfected").value();
    long currentInfectious = getLongAccumulator("currentInfectious").value();

    getContext()
            .getChannels()
            .getOutputChannelWriterById("timeseriesoutputs")
            .write(new ValueRecord("TimeSeriesOutputs")
                    .addField("testsPerDay", Long.valueOf(getGlobals().testsPerDay))
                    .addField("totaltestsgiven", Long.valueOf(totTestsGiv))
                    .addField("positivetestsgiven", Long.valueOf(posTestsGiv))
                    .addField("totaltestsreturned", Long.valueOf(totTestsRet))
                    .addField("positivetestsreturned", Long.valueOf(posTestsRet))
                    .addField("extinfections", Long.valueOf(extInfections))
                    .addField("numNewInfections", Long.valueOf(numNewInfections))
                    .addField("numInfected", Long.valueOf(currentInfected))
                    .addField("numInfectious", Long.valueOf(currentInfectious))
                    .addField("runID", getGlobals().runID));

    getGlobals().tStep++;
    
    // Write outputs on last step
    if (getGlobals().tStep == getGlobals().lastStep) {
      run(Person.sendNumPeopleInfected, CentralAgent.collectPersonInfectionStats);

      percPeopleCausing80PercInfections = calcPercPeopleCausing80PercInfections();
      try {
        Files.write(
            Paths.get("csvOutput/", getGlobals().csvOutputFilename),
            constructCSVOutput().getBytes(),
            StandardOpenOption.APPEND);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void dispose() {
    OutputWriterAgent.simEnd();
  }

  // Will return NaN if there were no infections in the simulation.
  private double calcPercPeopleCausing80PercInfections() {
    int totalInfectionsForThisCalculation =
        IntStream.range(0, getGlobals().personInfectionHist.size())
            .map(i -> i * getGlobals().personInfectionHist.get(i))
            .sum();
    double eightyPercInfections = totalInfectionsForThisCalculation * 0.8;
    double numAgentsCausing80PercInfections = 0;
    int infectionCount = 0;

    for (int i = getGlobals().personInfectionHist.size() - 1; i >= 0; i--) {
      int agentsInBucket = getGlobals().personInfectionHist.get(i);
      if (i * agentsInBucket + infectionCount < eightyPercInfections) {
        infectionCount += i * agentsInBucket;
        numAgentsCausing80PercInfections += agentsInBucket;
      } else {
        double remainingInfectionsToEightyPerc = eightyPercInfections - infectionCount;
        double numAgentsToCauseRemainingInfections = remainingInfectionsToEightyPerc / (double) i;
        numAgentsCausing80PercInfections += numAgentsToCauseRemainingInfections;
        break;
      }
    }

    return numAgentsCausing80PercInfections / (double) getGlobals().nAgents;
  }
}


