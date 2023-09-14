package core;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.rng.SeededRandom;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;



/**
 * The base Person class contains all logic around disease spread, symptom reporting, isolating,
 * spatial location, and base human factors such as compliance.
 *
 * <p>The Person class aims to contain any information that describes any person in any potential
 * simulation.
 */
public class Person extends Agent<Globals> {

  public long personID; // ID of the student agent
  public double age; // Person age
  /**
   * The place the person will be moving to when {@link #executeMovement} is ran.
   */
  protected ImmutableList<PlaceInfo> currentPlaceInfos = ImmutableList.of();
  // infection characteristics
  // Step of getting infected
  public int timeInfected = 0;
  // Number of steps that the person's illness will run
  public int illnessDuration;
  // Number of steps after being infected till symptoms present
  public int symptomOnset;
  // True if person is experiencing/ed an asymptomatic infection
  public boolean isAsymptomatic;
  // Number of steps after being infected that person becomes infectious
  // TODO This is not used at the moment
  public int tInfectious;
  // True when a test is being processed, or person has been tested and
  // tested positive
  public InfectionStatus status; // status of the agent (DEAD, RECOVERED, INFECTED)
  // Non-Pharmaceutical Interventions (Variables):
  public MaskType maskType = MaskType.NONE;
  public DailySchedule dailySchedule = DailySchedule.dummy();
  private boolean infectedFromSusceptibleThisStep = false;

  protected boolean hasBeenTested = false;

  public double complianceMask;
  public double compliancePhysicalDistancing;
  public double conformity;
  public boolean affiliationBoolean;
  public double affiliationSpectrum;
  public int contactRate;
  public double testPositivityRateThreshold;
  public boolean increasedComplianceFromTestPositivityRate = false;

  public int numPeopleInfected = 0;

  public static Action<Person> initPerson =
      Action.create(Person.class, Person::init);

  /**
   * Called at simulation start. Subclasses can override, but should call super.init().
   */
  public void init() {
    PersonInitializationInfo info = initializationInfo();
    // Generate agent characteristics
    this.personID = this.getID(); // Every agent is automatically assigned an ID upon initialisation
    this.age = info.ageSupplier().get(); // Generate age
    // Non-pharmaceutical interventions (NPI): mask wearing
    if (getGlobals().mandateMask) {
      // initialise mask wearing for compliant thiss
      this.complianceMask = info.maskComplianceSupplier().get();
      this.maskType = info.maskTypeSupplier().get();
    }

    this.compliancePhysicalDistancing = info.compliancePhysicalDistancingSupplier().get();
    this.contactRate = info.contactRateSupplier().get();
    this.conformity = info.conformitySupplier().get();
    this.affiliationBoolean = info.affiliationSupplier().get();
    this.affiliationSpectrum = info.affiliationSpectrumGenerator().apply(this).get();
    this.testPositivityRateThreshold = info.testPositivityRateThresholdSupplier().get();

    if (getGlobals().affiliationOverwriteMask) {
      this.complianceMask = this.affiliationSpectrum;
    }
    if (getGlobals().affiliationOverwriteDistancing) {
      this.compliancePhysicalDistancing = this.affiliationSpectrum;
    }

    // Determine person that are initially infected
    this.status = info.initialInfectionStatusSupplier().get();
    if (this.status == InfectionStatus.INFECTED) {
      this.timeInfected = getPrng().discrete(-7, 0).sample();
    }
  }

  /**
   * Provides an {@link PersonInitializationInfo} which will initialize this kind of person.
   *
   * <p>Subclasses should extend this and user {@link
   * PersonInitializationInfo#builderSetWithGlobalDefaults(Globals, SeededRandom)} or {@link
   * PersonInitializationInfo.Builder} directly in order to set up their initialization valus.
   */
  public PersonInitializationInfo initializationInfo() {
    return PersonInitializationInfo.builderSetWithGlobalDefaults(getGlobals(), getPrng()).build();
  }

  /**
   * The simulation will start with when t=0, all people send themselves in a message to the {@link
   * CentralAgent}. This is a heavy operation which occurs only once.
   */
  public static Action<Person> sendSelfToCentralAgentForScheduleCreation =
      Action.create(
          Person.class,
          person -> {
            if (person.getGlobals().tStep != 0) {
              throw new IllegalStateException(
                  "Place and schedule initialization can only be done at step 0.");
            }
            person
                .send(Messages.PersonMessage.class, msg -> msg.person = person)
                .to(person.getGlobals().centralAgentID);
          });

  /**
   * At t=0, after all people send themselves to {@link CentralAgent}, {@link CentralAgent} will
   * send back a schedule and some secondary init function.
   *
   * <p>Note that we receive a {@link Consumer} of a person, since (I think) in the Simudyne sdk,
   * changes to agent's should be ran in their own thread if we want the changes to take. IOW, if we
   * made changes to a Person object in CentralAgent, it would not be guaranteed to actually change
   * the person.
   */
  public static Action<Person> receiveSchedule =
      Action.create(
          Person.class,
          person -> {
            if (person.getGlobals().tStep != 0) {
              throw new IllegalStateException(
                  "Place and schedule initialization can only be done at step 0.");
            }
            person
                .getMessagesOfType(Messages.ScheduleMessage.class)
                .forEach(
                    msg -> {
                      person.dailySchedule = msg.schedule;
                      msg.schedule.secondaryInitialization().accept(person);
                    });
          });

  /**
   * May be called in child classes to set the current place.
   */
  public void setCurrentPlaces(List<PlaceInfo> currentPlaceInfos) {
    this.currentPlaceInfos = ImmutableList.copyOf(currentPlaceInfos);
  }

  /**
   * May be called in child classes to set the current place.
   */
  public void setCurrentPlaces(PlaceInfo currentPlaceInfo) {
    this.setCurrentPlaces(ImmutableList.of(currentPlaceInfo));
  }

  public ImmutableList<PlaceInfo> getCurrentPlaces() {
    return ImmutableList.<PlaceInfo>builder()
        .addAll(this.currentPlaceInfos)
        .build();
  }

  /**
   * Subclasses should override this method if there is any implementation specific schedule logic.
   */
  public void decideNextLocationIfNotIsolating() {
    setCurrentPlaces(
        ImmutableList.sortedCopyOf(
            Comparator.comparingLong(PlaceInfo::placeId),
            getScheduledPlaces().stream()
                .collect(Collectors.toList())));
  }

  public List<PlaceInfo> getScheduledPlaces() {
    return dailySchedule.placesAtStepMap().get(getGlobals().tStep % dailySchedule.placesAtStepMap().size());
  }

  /**
   * Subclasses shold not override this method unless they are also changing the isolation behavior.
   * This method already takes into account isolation compliance, and the appropriate method to
   * override would be {@link #decideNextLocationIfNotIsolating()}, since we want to include the
   * base iolation logic.
   */
  public void decideNextLocation() {
      decideNextLocationIfNotIsolating();
  }

  /**
   * This will be called at initialization t=0, in case there is more that needs to be ran on init.
   */
  public void initialiseFirstPlace() {
  }

  /**
   * Each time the agent goes to this place, they will wear a mask with this returned likelihood.
   * Add here or override in order to add place/place-type specific mask compliance, e.g. not
   * wearing masks in Suites.
   */
  protected double getLikelihoodOfWearingMaskAtPlace(PlaceInfo placeInfo) {
    return complianceMask;
  }

  /**
   * Determine if agent infected by COVID-19 is asymptomatic, symptomatic or severe. Severity is
   * calculated based on distribution returned by {@link #getInfectionTrajectoryDistribution()}.
   */
  public InfectionCharacteristics infectionSeverity(int stepInfected) {

    // Potential Addition of Inputs: Age of Agent
    // By having the age of an agent as input, we are able to change the proportion of asymptomatic,
    // symptomatic and severe cases
    double severity = getPrng().uniform(0, 1).sample();
    InfectionTrajectoryDistribution trajectoryDistribution = getInfectionTrajectoryDistribution();

    int tInfectious = 0;
    int illnessDuration = 0;
    int symptomsOnset = 0;
    boolean isAsymptomatic = false;

    // asymptomatic cases
    if (severity < trajectoryDistribution.percentageAsymptomaticCases()) {
      tInfectious =
          getPrng()
              .discrete(
                  trajectoryDistribution.infectiousRangeStart(),
                  trajectoryDistribution.infectiousRangeEnd())
              .sample()
              * getGlobals().tOneDay
              + stepInfected;
      illnessDuration =
          getPrng()
              .discrete(
                  trajectoryDistribution.illnessDurationNonSevereRangeStart(),
                  trajectoryDistribution.illnessDurationNonSevereRangeEnd())
              .sample()
              * getGlobals().tOneDay
              + stepInfected;
      symptomsOnset =
              getPrng()
                      .discrete(
                              trajectoryDistribution.symptomsOnsetRangeStart(),
                              trajectoryDistribution.symptomsOnsetRangeEnd())
                      .sample()
                      * getGlobals().tOneDay
                      + stepInfected;
      isAsymptomatic = true;
    }
    // symptomatic cases
    else if (severity >= trajectoryDistribution.percentageAsymptomaticCases()
        && severity
        < trajectoryDistribution.percentageAsymptomaticCases()
        + trajectoryDistribution.percentageNonSevereSymptomaticCases()) {
      tInfectious =
          getPrng()
              .discrete(
                  trajectoryDistribution.infectiousRangeStart(),
                  trajectoryDistribution.infectiousRangeEnd())
              .sample()
              * getGlobals().tOneDay
              + stepInfected;
      illnessDuration =
          getPrng()
              .discrete(
                  trajectoryDistribution.illnessDurationNonSevereRangeStart(),
                  trajectoryDistribution.illnessDurationNonSevereRangeEnd())
              .sample()
              * getGlobals().tOneDay
              + stepInfected;
      symptomsOnset =
          getPrng()
              .discrete(
                  trajectoryDistribution.symptomsOnsetRangeStart(),
                  trajectoryDistribution.symptomsOnsetRangeEnd())
              .sample()
              * getGlobals().tOneDay
              + stepInfected;
    }
    // severe cases
    else if (severity >= (1 - trajectoryDistribution.percentageSevereCases())) {
      tInfectious =
          getPrng()
              .discrete(
                  trajectoryDistribution.infectiousRangeStart(),
                  trajectoryDistribution.infectiousRangeEnd())
              .sample()
              * getGlobals().tOneDay
              + stepInfected;
      illnessDuration =
          getPrng()
              .discrete(
                  trajectoryDistribution.illnessDurationSevereRangeStart(),
                  trajectoryDistribution.illnessDurationSevereRangeEnd())
              .sample()
              * getGlobals().tOneDay
              + stepInfected;
      symptomsOnset =
          getPrng()
              .discrete(
                  trajectoryDistribution.symptomsOnsetRangeStart(),
                  trajectoryDistribution.symptomsOnsetRangeEnd())
              .sample()
              * getGlobals().tOneDay
              + stepInfected;
    } else {
      throw new IllegalArgumentException("The given InfectionTrajectoryDistribution is invalid.");
    }

    return InfectionCharacteristics.create(tInfectious, illnessDuration, symptomsOnset, isAsymptomatic);
  }

  /**
   * The infection trajectory distribution for this agent. Override this for testing.
   */
  // TODO This should maybe go in Modules
  public InfectionTrajectoryDistribution getInfectionTrajectoryDistribution() {
    return getGlobals().getInfectionTrajectoryDistribution(this);
  }

  /**
   * Method to check if an agent should die.
   */
  public boolean checkDeath() {
    if(isAsymptomatic) {
      return false;
    }
    double pAgeDeathThres = getGlobals().getProbabilityOfDeathGivenSevereIllness(this);

    // Normalise pAgeDeathThres, (22- 4) = Expected value of (illness duration - symptoms onset) for
    // severely ill person.
    // Rationale: for asymptomatic agents, they won't die because asymptomaticness is checked first
    // pAgeDeathThres (symptomOnset > illnessDuration)
    // Rationale: symptomatic (but not severe) person has a lower probability of dying.
    double expectedIllnessDurationSevere =
        (getGlobals().getInfectionTrajectoryDistribution(this).illnessDurationSevereRangeStart()
            + getGlobals()
            .getInfectionTrajectoryDistribution(this)
            .illnessDurationSevereRangeEnd())
            / 2.0
            * getGlobals().tOneDay;
    double expectedSymptomsOnsetSevere =
        (getGlobals().getInfectionTrajectoryDistribution(this).symptomsOnsetRangeStart()
            + getGlobals().getInfectionTrajectoryDistribution(this).symptomsOnsetRangeEnd())
            / 2.0
            * getGlobals().tOneDay;
    pAgeDeathThres =
        (pAgeDeathThres / (expectedIllnessDurationSevere - expectedSymptomsOnsetSevere))
            * (illnessDuration - symptomOnset);

    // Random probability for death
    double pKilled = getPrng().uniform(0, 1).sample();

    return pKilled < pAgeDeathThres;
  }

  public void die() {
    status = InfectionStatus.DEAD; // change status to dead

    // Sever connections with other connected agents by sending message to connected agents
    getLinks(Links.PersonToPersonLink.class)
        .forEach(
            link -> {
              send(Messages.RIPmsg.class).to(link.getTo());
              link.remove();
            });

    send(Messages.RIPmsg.class).to(getGlobals().centralAgentID);
  }

  /**
   * Method to update statistics in the console.
   *
   * <p>Note: Updating accumlators can mess up tests, so it is better to update accumplators in this method only. You
   * can use class member flags to flag that a certain accumlator needs to be updated.
   */
  public void updateAccumulators() {
    if (status == InfectionStatus.SUSCEPTIBLE) {
      getLongAccumulator("totSusceptible").add(1);
      getGlobals().numSusceptible++;
    } else if (status == InfectionStatus.INFECTED) {
      getLongAccumulator("totInfected").add(1);
      getGlobals().numInfected++;
    } else if (status == InfectionStatus.DEAD) {
      getLongAccumulator("totDead").add(1);
      getGlobals().numDead++;
    } else if (status == InfectionStatus.RECOVERED) {
      getLongAccumulator("totRecovered").add(1);
      getGlobals().numRecovered++;
    }
    if (infectedFromSusceptibleThisStep) {
      getLongAccumulator("numInfectionsThisStep").add(1);
      infectedFromSusceptibleThisStep = false;
    }
  }

  // Used in the initialisation step t=0
  public static Action<Person> setupInitialInfectionState =
      ActionFactory.createSuppressibleAction(
          Person.class,
          person -> {

            // Determine if agent infected by COVID is asymptomatic, symptomatic or severe
            if (person.status == InfectionStatus.INFECTED) {
              person.setInfected();
            }
          });

  // core.Person decides where to move next
  public static Action<Person> setInitialLocation =
      Action.create(
          Person.class,
          person -> {
            person.initialiseFirstPlace();
          });

  // Person decides where to move next
  public static Action<Person> movePerson =
      ActionFactory.createSuppressibleAction(
          Person.class,
          person -> {
            if (person.status != InfectionStatus.DEAD) {
              person.decideNextLocation();
            }
          });


  /**
   * Reports to {@link CentralAgent} where the agent will be going on this step/
   */
  public static Action<Person> executeMovement =
      ActionFactory.createSuppressibleAction(
          Person.class,
          person -> {
            if (person.status != InfectionStatus.DEAD) {
              person.getCurrentPlaces().forEach(
                  place -> {
                    boolean willWearMask =
                        person.getLikelihoodOfWearingMaskAtPlace(place)
                            > person.getPrng().uniform(0, 1).sample();
                    PersonTransmissibilityInfo transmissibilityInfo =
                        PersonTransmissibilityInfo.create(person, willWearMask);

                    person
                        .send(
                            Messages.IAmHereMsg.class,
                            msg -> {
                              msg.transmissibilityInfo = transmissibilityInfo;
                            })
                        .to(place.placeId());
                  });
            }
          });

  /**
   * Action to handle infection message from infected student to susceptible student
   */
  public static Action<Person> infectedByCOVID =
      ActionFactory.createSuppressibleAction(
          Person.class,
          person -> {
            if (person.hasMessagesOfType(Messages.InfectionMsg.class)) {
              // check if person is susceptible
              if (person.status == InfectionStatus.SUSCEPTIBLE) {
                person.setInfected();
              }
            }
          });

  public static Action<Person> infoExchange =
          ActionFactory.createSuppressibleAction(
                  Person.class,
                  person -> {
                    if (person.hasMessagesOfType(Messages.InfoExchangeMsg.class)) {
                      person.affiliationSpectrum = person.getMessagesOfType(Messages.InfoExchangeMsg.class).get(0).newAffiliationSpectrum;
                      if (person.getGlobals().infoExchangeDistancing) {
                        person.compliancePhysicalDistancing = person.affiliationSpectrum;
                      }
                      if (person.getGlobals().infoExchangeMask) {
                        person.complianceMask = person.affiliationSpectrum;
                      }
                    }
                  });

  public static Action<Person> getTestPositivity =
          ActionFactory.createSuppressibleAction(
                  Person.class,
                  person -> {
                    if (person.hasMessagesOfType(Messages.TestPositivityRateMsg.class)) {
                      double testPositivity = person.getMessagesOfType(Messages.TestPositivityRateMsg.class)
                              .get(0).testPositivityRate;
                      if (testPositivity >= person.testPositivityRateThreshold
                              && !person.increasedComplianceFromTestPositivityRate) {
                        if (person.getGlobals().testPositivityRateObservanceMask) {
                          person.complianceMask = 1 - ((1 - person.complianceMask) / 2);
                        }
                        if (person.getGlobals().testPositivityRateObservanceDistancing) {
                          person.compliancePhysicalDistancing = 1 - ((1 - person.compliancePhysicalDistancing) / 2);
                        }
                        person.increasedComplianceFromTestPositivityRate = true;
                      } else if (testPositivity < person.testPositivityRateThreshold
                              && person.increasedComplianceFromTestPositivityRate) {
                        if (person.getGlobals().testPositivityRateObservanceMask) {
                          person.complianceMask = 1 - ((1 - person.complianceMask) * 2);
                        }
                        if (person.getGlobals().testPositivityRateObservanceDistancing) {
                          person.compliancePhysicalDistancing = 1 - ((1 - person.compliancePhysicalDistancing) * 2);
                        }
                        person.increasedComplianceFromTestPositivityRate = false;
                      }
                    }
                  });

  /**
   * Receives {@link Messages.YouInfectedSomeoneMsg} from {@link PlaceAgent#generateContactsAndInfect}
   * Increments numPeopleInfected counter by the number of messages each step
   */
  public static Action<Person> infectedSomeoneElseWithCOVID =
      ActionFactory.createSuppressibleAction(
          Person.class,
          person -> {
            List<Messages.YouInfectedSomeoneMsg> youInfectedMsgs =
                person.getMessagesOfType(Messages.YouInfectedSomeoneMsg.class);
            person.numPeopleInfected += youInfectedMsgs.size();

            if (person.getGlobals().outputTransmissions) {
              List<String> transmissionInfos = person.generateTransmissionInfos(youInfectedMsgs);
              for (String transmissionInfo : transmissionInfos) {
                person.send(Messages.OutputWriterStringMessage.class, msg -> {
                  msg.key = OutputWriterAgent.KEY_TRANSMISSIONS;
                  msg.value = transmissionInfo;
                }).to(person.getGlobals().outputWriterAgentID);
              }
            }
          }
      );

  /*
    The full output we are building:
    "InfectingAgentId,isSymptomatic,stepExposure,stepSymptoms,stepRecover,isAsymptomatic," +
    "AgentType,comSymptomsReport,compQuarantineWhenSymptomatic,complianceMask,complianceIsolating," +
    "isSelfIsolatingBecauseOfSymptoms,isSelfIsolatingBecauseOfContactTracing," +
    "complianceIsolateWhenContactNotified,compliancePhysicalDistancing,contactRate," +
    "probHostsAdditionalEvent,probAttendsAdditionalEvent,maskType,placeType,placeId,newlyInfectedAgentId," +
    "newlyInfectedCompliancePhysicalDistancing,newlyInfectedMaskType\n"
   */

  private List<String> generateTransmissionInfos(List<Messages.YouInfectedSomeoneMsg> msgs) {
    String agentInfo = generateThisAgentTransmissionInfo();

    List<String> transmissionInfos = new ArrayList<>();
    for (Messages.YouInfectedSomeoneMsg msg : msgs) {
      StringBuilder sb = new StringBuilder();
      sb.append(agentInfo);
      sb.append(msg.infectedByMaskType.ordinal());
      sb.append(',');
      sb.append(msg.placeType);
      sb.append(',');
      sb.append(msg.placeId);
      sb.append(',');
      sb.append(msg.newlyInfectedAgentId);
      sb.append(',');
      sb.append(msg.newlyInfectedMaskType.ordinal());
      sb.append(',');
      sb.append(msg.newlyInfectedCompliancePhysicalDistancing);
      transmissionInfos.add(sb.toString());
    }
    return transmissionInfos;
  }

  private String generateThisAgentTransmissionInfo() {
    StringBuilder sb = new StringBuilder();

    sb.append(this.personID);
    sb.append(',');
    sb.append(this.isSymptomatic());
    sb.append(',');
    sb.append(this.timeInfected);
    sb.append(',');
    sb.append(this.symptomOnset);
    sb.append(',');
    sb.append(this.illnessDuration);
    sb.append(',');
    sb.append(this.isAsymptomatic);
    sb.append(',');
    sb.append(this.getClass().toString());
    sb.append(',');
    sb.append(this.complianceMask);
    sb.append(',');
    sb.append(this.compliancePhysicalDistancing);
    sb.append(',');
    sb.append(this.contactRate);
    sb.append(',');

    return sb.toString();
  }

  /**
   * Sends {@link Messages.NumPeopleInfectedMsg} to {@link CentralAgent#collectPersonInfectionStats}
   * These messages are used to compile a histogram of how many other people
   * each Person agent directly infected
   */
  public static Action<Person> sendNumPeopleInfected =
          ActionFactory.createSuppressibleAction(
                  Person.class,
                  person -> person.send(Messages.NumPeopleInfectedMsg.class,
                                  msg -> msg.setBody(person.numPeopleInfected))
                          .to(person.getGlobals().centralAgentID)
          );

  /**
   * Everything that needs to be done when a person is getting infected. The characteristics of this
   * infection are drawn, and infection tracking values are set.
   */
  public void setInfected() {
    if(this.status == InfectionStatus.SUPPRESSED) {
      throw new IllegalStateException("A suppressed person should not be set to infected.");
    }

    // change status to infected
    this.status = InfectionStatus.INFECTED;
    this.infectedFromSusceptibleThisStep = true;

    // record time infected
    if (this.getGlobals().tStep > 0) {
      this.timeInfected = this.getGlobals().tStep;
    }
    // (1) Determine if infection is asymptomatic, symptomatic or severe
    // (2) Get illness duration, symptoms onset and infectious period
    InfectionCharacteristics infectionCharacteristics = this.infectionSeverity(this.timeInfected);
    this.tInfectious = infectionCharacteristics.tInfectious();
    this.illnessDuration = infectionCharacteristics.illnessDuration();
    this.symptomOnset = infectionCharacteristics.symptomsOnset();
    this.isAsymptomatic = infectionCharacteristics.isAsymptomatic();
  }

  /**
   * Returns true if the person has symptoms.
   */
  public boolean isSymptomatic() {
    if(status == InfectionStatus.INFECTED &&
            symptomOnset <= getGlobals().tStep &&
            !this.isAsymptomatic) {
      return true;
    }
    return false;
  }

  public static Action<Person> countInfected =
          ActionFactory.createSuppressibleAction(
                  Person.class,
                  p -> {
                    if(p.status == InfectionStatus.INFECTED) {
                      p.getLongAccumulator("currentInfected").add(1);
                      if(p.isInfectious()) {
                        p.getLongAccumulator("currentInfectious").add(1);
                      }
                    }
                  });

  /**
   * Returns true if the person is infectious.
   */
  public boolean isInfectious() {
    return status == InfectionStatus.INFECTED && tInfectious <= getGlobals().tStep;
  }

  /**
   * The person will decide whether to self quarantine the first time they are symptomatic.
   */
  public boolean isFirstTimeSymptomatic() {
    if(!this.isSymptomatic()) {
      return false;
    }
    boolean infectedPreviouslyCovid =
        status == InfectionStatus.INFECTED && symptomOnset < getGlobals().tStep && !this.isAsymptomatic;

    if (infectedPreviouslyCovid) {
      return false;
    }

    boolean infectedByCovidThisStep =
        status == InfectionStatus.INFECTED && symptomOnset == getGlobals().tStep && !this.isAsymptomatic;

    return infectedByCovidThisStep;
  }

  /**
   * Person gets a message that they were tested and sends their sample with the full truth to
   * {@link CentralAgent}.
   */
  public static Action<Person> getTested =
      ActionFactory.createSuppressibleAction(
          Person.class,
          Person::administerTestIfOrdered);

  private double currentTestingAccuracy() {
    if(getGlobals().testingType == 0) {
      return 1.0;
    }
    else if(getGlobals().testingType == 1) {
      if(this.status == InfectionStatus.INFECTED) {
        return 1.0 - getGlobals().testingFalseNegativePerc;
      }
      else {
        return 1.0 - getGlobals().testingFalsePositivePerc;
      }
    }
    else {
      if (this.status == InfectionStatus.SUSCEPTIBLE) {
        return 1.0 - getGlobals().testingFalsePositivePerc;
      }
      // This may be changed later
      else if (this.status == InfectionStatus.RECOVERED) {
        return 1.0 - getGlobals().testingFalsePositivePerc;
      }
      /**
       * Currently an inverted parabola with a vertex at (h, k)
       * h = symptom onset time + 3 Days =
       *            person#symptomOnset - person#timeInfected + (3 * globals#tOneDay)
       * k = max test accuracy = 1 - globals#testingFalseNegativePerc
       *
       * Equation before h is ACCURACY = a(TIME - h)^2 + k
       * a = -k/h^2 so that ACCURACY = 0 at tStep = person#timeInfected
       *
       * Equation after h is ACCURACY = b(TIME - h)^2 + k
       * b = -k/(d^2 - 2dh + h^2)
       * d = person#illnessDuration - person#timeInfected +
       *                      (globals#daysAfterInfectionToDetect * globals#tOneDay)
       * So that ACCURACY = 0 at tStep = person#illnessDuration + globals#daysAfterInfectionToDetect
       */
      else if (this.status == InfectionStatus.INFECTED) {
        double h = this.symptomOnset - this.timeInfected + (3*getGlobals().tOneDay);
        double k = 1 - getGlobals().testingFalseNegativePerc;
        double time = getGlobals().tStep - this.timeInfected;
        if (time < h) {
          double a = -k / (h * h);
          return a * (time - h) * (time - h) + k;
        } else {
          double d = this.illnessDuration - this.timeInfected +
                  (getGlobals().daysAfterInfectionToDetect * getGlobals().tOneDay);
          double b = -k / (d * d - 2 * d * h + h * h);
          return b * (time - h) * (time - h) + k;
        }
      } else {
        return 1.0;
      }
    }
  }

  private void administerTestIfOrdered() {
    if (!this.hasBeenTested && this.hasMessagesOfType(Messages.TestAdministeredMsg.class)) {
      this.send(
          Messages.InfectionStatusMsg.class,
          msg -> {
            msg.infectedStatus = this.status;
            msg.testAccuracy = this.currentTestingAccuracy();
          })
          .to(this.getGlobals().centralAgentID);

      // Condition to restrict this from requesting multiple tests before results are
      // returned.
      this.hasBeenTested = true;
    }
  }

  /**
   * If it is time to recover or die based on trajector, do so.
   */
  public static Action<Person> recoverOrDieOrStep =
      ActionFactory.createSuppressibleAction(
          Person.class,
          person -> {

            // person has a probability of dying between symptoms onset and end of illness
            // (determined by illnessDuration)
            // For severe cases, the illness duration is much longer, hence, having a higher
            // probability of dying
            if ((person.status == InfectionStatus.INFECTED)
                && (person.isSymptomatic())
                && (person.getGlobals().tStep < person.illnessDuration)) {

              // check to see if the person will die in this step (age dependent)
              if (person.checkDeath()) {
                person.die();
              }

              // check if student is infected (not dead or recovered) and their illness duration is
              // over
            } else if ((person.status == InfectionStatus.INFECTED)
                && person.illnessDuration == person.getGlobals().tStep) {

              // change status to recovered
              person.status = InfectionStatus.RECOVERED;
            }

            // update accumulators for console
            person.updateAccumulators();
          });

  public static Action<Person> externalInfections =
      ActionFactory.createSuppressibleAction(
          Person.class,
          person -> {
            if (person.status == InfectionStatus.SUSCEPTIBLE) {
              double pExternalInfection = person.getPrng().uniform(0, 1).sample();

              if (pExternalInfection < person.getGlobals().getExternalInfectionRate(person)) {
                person.setInfected();
                person.getLongAccumulator("numExtInfectionsThisStep").add(1);
              }
            }
          });

  public void resetForNextStep() {
    this.hasBeenTested = false;
  }
  public static Action<Person> resetForNextStep =
          ActionFactory.createSuppressibleAction(
                  Person.class,
                  Person::resetForNextStep
          );

  public String getName() {
    return this.getClass().getName() + "_" + personID;
  }



  public double getTestSelectionMultiplier() {
    return 1.0;
  }

  public static Action<Person> sendTestSelectionMultiplierToCentralAgent =
          ActionFactory.createSuppressibleAction(
                  Person.class,
                  person -> {
                    person
                            .send(
                                    Messages.TestSelectionMultiplierMessage.class,
                                    msg -> msg.setBody(person.getTestSelectionMultiplier()))
                            .to(person.getGlobals().centralAgentID);
                  });

  // Enum for tracking status of agent
  public enum InfectionStatus {
    SUSCEPTIBLE,
    INFECTED,
    RECOVERED,
    DEAD,
    SUPPRESSED
  }

  /**
   * All Person initialization factors that can be present in any implementation. Simulation can
   * provide different instances of these infos for different agent types.
   */
  @AutoValue
  public abstract static class PersonInitializationInfo {
    public abstract Supplier<Double> ageSupplier();

    public abstract Supplier<Double> maskComplianceSupplier();

    public abstract Supplier<InfectionStatus> initialInfectionStatusSupplier();

    public abstract Supplier<Double> compliancePhysicalDistancingSupplier();

    public abstract Supplier<Integer> contactRateSupplier();

    public abstract Supplier<MaskType> maskTypeSupplier();

    public abstract Supplier<Double> conformitySupplier();

    public abstract Supplier<Boolean> affiliationSupplier();

    public abstract Function<Person, Supplier<Double>> affiliationSpectrumGenerator();

    public abstract Supplier<Double> testPositivityRateThresholdSupplier();

    public static Builder builderSetWithGlobalDefaults(
        final Globals globals, final SeededRandom random) {
      return new AutoValue_Person_PersonInitializationInfo.Builder()
          .ageSupplier(uniform(globals.defaultAgentAgeStart, globals.defaultAgentAgeEnd, random))
          .maskComplianceSupplier(
              uniform(
                  globals.defaultAgentMaskComplianceStart,
                  globals.defaultAgentMaskComplianceEnd,
                  random))
          .initialInfectionStatusSupplier(
              () -> {
                if (random.uniform(0, 1).sample() < globals.percInitiallyRecovered) {
                  return InfectionStatus.RECOVERED;
                } else if (random.uniform(0, 1).sample()
                    < (globals.percInitiallyInfected / (1 - globals.percInitiallyRecovered))) {
                  return InfectionStatus.INFECTED;
                }
                return InfectionStatus.SUSCEPTIBLE;
              })
          .compliancePhysicalDistancingSupplier(
              uniform(
                  globals.defaultAgentCompliancePhysicalDistancingStart,
                  globals.defaultAgentCompliancePhysicalDistancingtEnd,
                  random))
          .contactRateSupplier(
              () ->
                  random
                      .discrete(
                          globals.agentContactRateRangeStart, globals.agentContactRateRangeEnd)
                      .sample())
          .maskTypeSupplier(
                  () -> MaskType.N95
          )
          .conformitySupplier(uniform(globals.agentConformityStart, globals.agentContactRateRangeEnd, random))
              .affiliationSupplier(coinFlip(globals.affiliationProp, random))
              .affiliationSpectrumGenerator((Person p) -> {
                if (p.affiliationBoolean) {
                  return truncNormal(0, 1, globals.rightAffiliationComplianceMean, globals.affiliatonStdev, random);
                } else {
                  return truncNormal(0, 1, globals.leftAffiliationComplianceMean, globals.affiliatonStdev, random);
                }
              })
              .testPositivityRateThresholdSupplier(
                      uniform(
                              globals.testPositivityRateThresholdStart,
                              globals.getTestPositivityRateThresholdEnd,
                              random));
    }

    public static Builder dummyBuilder() {
      return new AutoValue_Person_PersonInitializationInfo.Builder()
          .ageSupplier(() -> 0.0)
          .maskComplianceSupplier(() -> 0.0)
          .initialInfectionStatusSupplier(() -> InfectionStatus.SUSCEPTIBLE)
          .compliancePhysicalDistancingSupplier(() -> 0.0)
          .contactRateSupplier(() -> 0)
          .maskTypeSupplier(() -> MaskType.NONE)
          .conformitySupplier(() -> 0.0)
          .testPositivityRateThresholdSupplier(() -> 0.0);
    }

    // Quick helper method for slightly cleaner code
    public static Supplier<Double> uniform(
        final double start, final double end, final SeededRandom r) {
      if (start == end) {
        return () -> end;
      }
      return () -> r.uniform(start, end).sample();
    }

    public static Supplier<Double> truncNormal(
        final double start,
        final double end,
        final double mean,
        final double sd,
        final SeededRandom r) {
      if (start == end) {
        return () -> end;
      }
      return () -> {
        double d = 0.0;
        do {
          d = r.normal(mean, sd).sample();
        } while (d < start || d >= end);
        return d;
      };
    }

    public static <T> Supplier<T> distribution(
        final double[] distribution, final T[] values, final SeededRandom seededRandom) {

      return () -> {
        double draw = seededRandom.uniform(0, 1).sample();

        double cumulativeSum = 1 - (Arrays.stream(distribution).sum());
        if (cumulativeSum >= draw) {
          return values[0];
        }

        for (int i = 0; i < distribution.length; i++) {
          cumulativeSum += distribution[i];
          if (cumulativeSum >= draw) {
            return values[i];
          }
        }
        throw new IllegalStateException("Given invalid distribution: " + distribution);
      };
    }

    public static Supplier<Boolean> coinFlip(final double trueChance, final SeededRandom seededRandom) {
      return () -> {
        double coin = seededRandom.uniform(0, 1).sample();
        return coin <= trueChance;
      };
    }

    public static Builder builder() {
      return new AutoValue_Person_PersonInitializationInfo.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder ageSupplier(Supplier<Double> ageSupplier);

      public abstract Builder maskComplianceSupplier(Supplier<Double> maskComplianceSupplier);

      public abstract Builder initialInfectionStatusSupplier(
          Supplier<InfectionStatus> initialInfectionStatusSupplier);

      public abstract Builder compliancePhysicalDistancingSupplier(
          Supplier<Double> compliancePhysicalDistancingSupplier);

      public abstract Builder contactRateSupplier(Supplier<Integer> contactRateSupplier);

      public abstract Builder maskTypeSupplier(Supplier<MaskType> maskTypeSupplier);

      public abstract Builder conformitySupplier(Supplier<Double> conformitySupplier);

      public abstract Builder affiliationSupplier(Supplier<Boolean> affiliationSupplier);

      public abstract Builder affiliationSpectrumGenerator(Function<Person, Supplier<Double>> affiliationSpectrumGenerator);

      public abstract Builder testPositivityRateThresholdSupplier(Supplier<Double> testPositivityRateThresholdSupplier);

      public abstract PersonInitializationInfo build();
    }
  }

  /**
   * Minimally relevant info about a person who occupied a space, in order to contribute to
   * infection spread calculation.
   */
  @AutoValue
  public abstract static class PersonTransmissibilityInfo {
    public abstract InfectionStatus status();

    public abstract boolean isInfectious();

    public abstract boolean isSymptomatic();

    public abstract MaskType wearsMask();

    public abstract double physicalDistCompliance();

    public abstract int contactRate();

    public abstract double conformityScore();

    public abstract double affiliationSpectrum();

    public abstract Builder toBuilder();

    public static PersonTransmissibilityInfo create(InfectionStatus status, boolean isInfectious, boolean isSymptomatic, MaskType wearsMask, double physicalDistCompliance, int contactRate, double conformityScore, double affiliationSpectrum) {
      return builder()
              .status(status)
              .isInfectious(isInfectious)
              .isSymptomatic(isSymptomatic)
              .wearsMask(wearsMask)
              .physicalDistCompliance(physicalDistCompliance)
              .contactRate(contactRate)
              .conformityScore(conformityScore)
              .affiliationSpectrum(affiliationSpectrum)
              .build();
    }

    public static PersonTransmissibilityInfo dummy() {
      return create(InfectionStatus.SUSCEPTIBLE, false, false, MaskType.NONE, 0, 0, 0, 0);
    }

    public static PersonTransmissibilityInfo dummyInfected() {
      return create(InfectionStatus.INFECTED, true, true, MaskType.NONE, 0, 0, 0, 0);
    }

    public static PersonTransmissibilityInfo create(Person person, boolean willWearMask) {
      return create(
          person.status,
          person.isInfectious(),
          person.isSymptomatic(),
          willWearMask ? person.maskType : MaskType.NONE,
          person.compliancePhysicalDistancing,
          person.contactRate,
          person.conformity,
          person.affiliationSpectrum
      );
    }

    public static Builder builder() {
      return new AutoValue_Person_PersonTransmissibilityInfo.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder status(InfectionStatus status);

      public abstract Builder isInfectious(boolean isInfectious);

      public abstract Builder isSymptomatic(boolean isSymptomatic);

      public abstract Builder wearsMask(MaskType wearsMask);

      public abstract Builder physicalDistCompliance(double physicalDistCompliance);

      public abstract Builder contactRate(int contactRate);

      public abstract Builder conformityScore(double conformityScore);

      public abstract Builder affiliationSpectrum(double affiliationSpectrum);

      public abstract PersonTransmissibilityInfo build();
    }
  }

  // TODO Check if these mask type names correctly align with data, and add reference
  public enum MaskType {
    NONE,
    //HOMEMADE_CLOTH,
    //SURGICAL,
    N95
  }

  /**
   * Secondary initialization information after {@link CentralAgent} runs secondary init.
   */
  // TODO Make this name better. DailySchedule alone no longer seems accurate
  @AutoValue
  public abstract static class DailySchedule {
    /**
     * The places the person will go to at different tSteps.
     */
    public abstract ImmutableMap<Integer, List<PlaceInfo>> placesAtStepMap();

    /**
     * The places the person goes to when isolating.
     */
    public abstract ImmutableList<PlaceInfo> isolationPlaces();

    /**
     * A {@link Consumer} to consume the person after secondary init. This may set some additional
     * values which were only available after the initial initialization.
     */
    public abstract Consumer<Person> secondaryInitialization();

    public static DailySchedule create(
        ImmutableMap<Integer, List<PlaceInfo>> placesAtStepMap,
        ImmutableList<PlaceInfo> isolationPlaceInfos,
        Consumer<Person> secondaryInitialization) {
      return new AutoValue_Person_DailySchedule(
          placesAtStepMap, isolationPlaceInfos, secondaryInitialization);
    }

    public static DailySchedule create(
        ImmutableMap<Integer, List<PlaceInfo>> placesAtStepMap, ImmutableList<PlaceInfo> isolationPlaceInfos) {
      return new AutoValue_Person_DailySchedule(placesAtStepMap, isolationPlaceInfos, p -> {
      });
    }

    public static DailySchedule dummy() {
      return create(ImmutableMap.of(), ImmutableList.of(), person -> {
      });
    }
  }
}
