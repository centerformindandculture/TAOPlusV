package tau.anylogic_code;

import core.Person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DiningHall
 */
public class DiningHall extends ConnectionOfAgents implements Serializable {

  private final List<Building> assignedBuildings = new ArrayList<>();

  /**
   * Default constructor
   */
  public DiningHall(long uniqueId) {
    super(uniqueId);
  }

  public void addBuilding(Building b) {
    assignedBuildings.add(b);
  }

  @Override
  public List<Person> getPeople() {
    List<Person> people =
        assignedBuildings.stream()
            .map(Building::getPeople)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    // traceln("Dining hall has people " + people.size());
    return people;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  /**
   * This number is here for model snapshot storing purpose<br>
   * It needs to be changed when this class gets changed
   */
  private static final long serialVersionUID = 1L;
}
