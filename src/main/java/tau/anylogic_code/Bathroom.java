package tau.anylogic_code;

import core.Person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Bathroom
 */
public class Bathroom extends ConnectionOfAgents implements Serializable {

  public List<ConnectionOfAgents> usesBathroom = new ArrayList<ConnectionOfAgents>();

  /**
   * Default constructor
   */
  public Bathroom(int frequency, long uniqueId) {
    super(frequency, uniqueId);
  }

  public Bathroom(long uniqueId) {
    super(uniqueId);
  }

  public void addUsesBathroom(ConnectionOfAgents coa) {
    usesBathroom.add(coa);
  }

  public void addAllUsesBathroom(Collection<? extends ConnectionOfAgents> coas) {
    usesBathroom.addAll(coas);
  }

  @Override
  public List<Person> getPeople() {
    people.clear();
    for (ConnectionOfAgents coa : usesBathroom) {
      addPeople(coa.getPeople());
    }
    return super.getPeople();
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
