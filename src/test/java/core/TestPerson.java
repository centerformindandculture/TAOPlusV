package core;

public final class TestPerson extends Person {

  PlaceInfo firstPlaceInfo;
  PlaceInfo nextPlaceInfo;

  double testingMultiplier = 1.0;

  PersonInitializationInfo.Builder initializationBuilder = PersonInitializationInfo.dummyBuilder();

  InfectionTrajectoryDistribution infectionTrajectoryDistribution =
      InfectionTrajectoryDistribution.dummyBuilder().build();

  public PlaceInfo getNextPlace() {
    return nextPlaceInfo;
  }

  public void setNextPlace(PlaceInfo nextPlaceInfo) {
    this.nextPlaceInfo = nextPlaceInfo;
  }

  public PlaceInfo getFirstPlace() {
    return firstPlaceInfo;
  }

  public void setFirstPlace(PlaceInfo firstPlaceInfo) {
    this.firstPlaceInfo = firstPlaceInfo;
  }

  public PersonInitializationInfo.Builder getInitializationBuilder() {
    return initializationBuilder;
  }

  public void setInitializationBuilder(PersonInitializationInfo.Builder initializationBuilder) {
    this.initializationBuilder = initializationBuilder;
  }

  @Override
  public double getTestSelectionMultiplier() {
    return testingMultiplier;
  }

  public void setTestingMultiplier(double testingMultiplier) {
    this.testingMultiplier = testingMultiplier;
  }

  @Override
  public PersonInitializationInfo initializationInfo() {
    return initializationBuilder.build();
  }

  @Override
  public InfectionTrajectoryDistribution getInfectionTrajectoryDistribution() {
    return infectionTrajectoryDistribution;
  }

  public void setInfectionTrajectoryDistribution(
      InfectionTrajectoryDistribution infectionTrajectoryDistribution) {
    this.infectionTrajectoryDistribution = infectionTrajectoryDistribution;
  }

  @Override
  public void decideNextLocation() {
    setCurrentPlaces(nextPlaceInfo);
  }

  @Override
  public void initialiseFirstPlace() {
    setCurrentPlaces(firstPlaceInfo);
  }
}
