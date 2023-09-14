package core;

import simudyne.core.rng.SeededRandom;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Modules {

  /**
   * Return a trajectory distribution for a given person.
   *
   * <p>In future versions, we might have different distributions for people with different
   * characterstics, i.e. age, pre-existing conditions.
   */
  InfectionTrajectoryDistribution getInfectionTrajectoryDistribution(
      Person person, Globals globals);

  long createConnectionOfAgents(List<Person> allPeople, Globals globals);

  Map<Long, Person.DailySchedule> createPlacesAndPersonDailySchedules(Globals globals);

  double getExternalInfectionRate(Person person, Globals globals);

  Set<Long> getAgentsToTest(
      Set<Long> symptomaticAgentsToday,
      Map<Long, Double> testSelectionMultipliers,
      SeededRandom random,
      Globals globals);

  Set<Long> getAgentsToTest(
          Set<Long> symptomaticAgentsToday,
          Map<Long, Double> testSelectionMultipliers,
          SeededRandom random,
          long numTestsToRun);

  Set<Integer> getPlaceTypesOmittedFromContactTracing();
}
