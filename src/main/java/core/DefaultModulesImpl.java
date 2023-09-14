package core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import simudyne.core.rng.SeededRandom;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultModulesImpl implements Modules {
  // TODO(#10) Reference source for these numbers.
  public static InfectionTrajectoryDistribution overallInfectionTrajectoryDistribution = null;

  private static synchronized void setOverallInfectionTrajectoryDistribution(Globals globals) {
    overallInfectionTrajectoryDistribution =
        InfectionTrajectoryDistribution.dummyBuilder()
            .percentageAsymptomaticCases(globals.percAsymptomatic)
            .percentageNonSevereSymptomaticCases(0.45)
            .percentageSevereCases(globals.percSevere)
            .infectiousRangeStart(2)
            .infectiousRangeEnd(3)
            .illnessDurationNonSevereRangeStart(7)
            .illnessDurationNonSevereRangeEnd(14)
            .symptomsOnsetRangeStart(3)
            .symptomsOnsetRangeEnd(5)
            .illnessDurationSevereRangeStart(14)
            .illnessDurationSevereRangeEnd(30)
            .build();
  }

  public static DefaultModulesImpl getInstance() {
    return new DefaultModulesImpl();
  }

  @VisibleForTesting
  public DefaultModulesImpl() {
  }

  @Override
  public InfectionTrajectoryDistribution getInfectionTrajectoryDistribution(
      Person person, Globals globals) {
    if (overallInfectionTrajectoryDistribution == null) {
      setOverallInfectionTrajectoryDistribution(globals);
    }
    return overallInfectionTrajectoryDistribution;
  }

  @Override
  public long createConnectionOfAgents(List<Person> allPeople, Globals globals) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<Long, Person.DailySchedule> createPlacesAndPersonDailySchedules(Globals globals) {
    throw new UnsupportedOperationException();
  }

  // TODO Think through this external infection rate and how we can consider things like
  //      taking public transit, mask wearing, external jobs etc.
  @Override
  public double getExternalInfectionRate(Person person, Globals globals) {
    if (globals.overallExternalInfectionRateFromData != 0) {
      return globals.overallExternalInfectionRateFromData * globals.baseInfectivity;
    }
    return globals.baseOnCampusExternalInfectionRate;
  }

  @Override
  public Set<Long> getAgentsToTest(
      Set<Long> symptomaticAgentsToday,
      Map<Long, Double> testSelectionMultipliers,
      SeededRandom random,
      Globals globals) {
    return getAgentsToTest(
            symptomaticAgentsToday,
            testSelectionMultipliers,
            random,
            globals.testsPerDay / globals.tOneDay);
  }

  @Override
  public Set<Long> getAgentsToTest(
          Set<Long> symptomaticAgentsToday,
          Map<Long, Double> testSelectionMultipliers,
          SeededRandom random,
          long numTestsToRun) {
    Set<Long> agentsToTest = new HashSet<>();

    double sumAllMultipliers = testSelectionMultipliers.values().stream().reduce(0.0, Double::sum);
    double sumOfAgentsToTest = 0;

    while (agentsToTest.size() < numTestsToRun
            && agentsToTest.size() < testSelectionMultipliers.size()) {
      double selection = random.uniform(0, sumAllMultipliers - sumOfAgentsToTest).sample();
      double curSum = 0.0;
      FIND_AGENT_TO_TEST_LOOP:
      for (Map.Entry<Long, Double> entry : testSelectionMultipliers.entrySet()) {
        if (agentsToTest.contains(entry.getKey())) {
          continue;
        }
        curSum += entry.getValue();
        if (selection < curSum) {
          agentsToTest.add(entry.getKey());
          sumOfAgentsToTest += entry.getValue();
          break FIND_AGENT_TO_TEST_LOOP;
        }
      }
    }
    return agentsToTest;
  }

  @Override
  public Set<Integer> getPlaceTypesOmittedFromContactTracing() {
    return ImmutableSet.of();
  }

  public static List<Messages.IAmHereMsg> sample(
      List<Messages.IAmHereMsg> all, int num, SeededRandom random) {
    ImmutableList.Builder<Messages.IAmHereMsg> builder = ImmutableList.builder();

    if (all.size() == 1) {
      builder.add(all.get(0));
      return builder.build();
    }

    for (int i = 0; i < num; i++) {
      builder.add(all.get(random.discrete(0, all.size() - 1).sample()));
    }
    return builder.build();
  }

  public static List<Messages.IAmHereMsg> getAllExcept(
      List<Messages.IAmHereMsg> all, Messages.IAmHereMsg except) {
    ImmutableList.Builder<Messages.IAmHereMsg> builder = ImmutableList.builder();
    for (Messages.IAmHereMsg msg : all) {
      if (except.getSender() != msg.getSender()) {
        builder.add(msg);
      }
    }
    return builder.build();
  }

  public static boolean willInfect(
      Messages.IAmHereMsg infected,
      Messages.IAmHereMsg infectee,
      double baseInfectionRate,
      SeededRandom random) {
    if (!infected.transmissibilityInfo.isInfectious()) {
      return false;
    }
    if (infectee.transmissibilityInfo.status() != Person.InfectionStatus.SUSCEPTIBLE) {
      return false;
    }

    double physicalDistancingStrength =
        infected.transmissibilityInfo.physicalDistCompliance()
            * infectee.transmissibilityInfo.physicalDistCompliance();
    double pPhysicalDistancingSuccess = random.uniform(0, 1).sample();
    if (pPhysicalDistancingSuccess < physicalDistancingStrength) {
      return false;
    }

    // generate random uniform probability of infection between 0-1
    double pInfectOut = random.uniform(0, 1).sample();

    // NPI (Mask wearing): Reduce infectionRate if the infected agent is wearing
    // a mask
    double outTransmissionLikelihood =
        getOutTransmissionLikelihood(baseInfectionRate, infected.transmissibilityInfo, random);

    // if random prob less than infection rate -> student will infect another
    // student
    if (pInfectOut < outTransmissionLikelihood) {
      double inTransmissionLikelihood =
          getInTransmissionLikelihood(infectee.transmissibilityInfo, random);
      double pInfectIn = random.uniform(0, 1).sample();
      return pInfectIn < inTransmissionLikelihood;
    }
    return false;
  }

  // Reduce incoming infection chance through NPI's, vaccination, etc.
  public static double getInTransmissionLikelihood(Person.PersonTransmissibilityInfo inTransmissibility, SeededRandom random) {
    double inTransmissionChance = 1.0;

    // Inward efficiency: protecting the wearer against catching the disease
    double maskInwardEfficiency = 0;

    Person.MaskType maskType = inTransmissibility.wearsMask();

    // NPI: Mask wearing
    /*if (maskType == Person.MaskType.HOMEMADE_CLOTH) {
      maskInwardEfficiency = random.uniform(0.2, 0.8).sample();
    } else if (maskType == Person.MaskType.SURGICAL) {
      maskInwardEfficiency = random.uniform(0.7, 0.9).sample();
    } else */
    if (maskType == Person.MaskType.N95) {
      maskInwardEfficiency = random.uniform(0.95, 1).sample();
    }
    inTransmissionChance *= (1 - maskInwardEfficiency);

    return inTransmissionChance;
  }

  // Reduce outgoing infection chance through NPI's, vaccination, etc.
  public static double getOutTransmissionLikelihood(
      double baseInfectivity, Person.PersonTransmissibilityInfo outTransmissibility, SeededRandom random) {
    double outInfectivity = baseInfectivity;

    // Outward efficency: protecting the wearer from transmiting the disease
    double maskOutwardEfficiency = 0;
    Person.MaskType maskType = outTransmissibility.wearsMask();

    /*if (maskType == Person.MaskType.HOMEMADE_CLOTH) {
      maskOutwardEfficiency = random.uniform(0, 0.8).sample();
    } else if (maskType == Person.MaskType.SURGICAL) {
      maskOutwardEfficiency = random.uniform(0.5, 0.9).sample();
    } else*/
    if (maskType == Person.MaskType.N95) {
      maskOutwardEfficiency = random.uniform(0.7, 1).sample();
    }
    outInfectivity *= (1 - maskOutwardEfficiency);

    return outInfectivity;
  }

}
