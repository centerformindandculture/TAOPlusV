package core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import simudyne.core.graph.Message;

public class Messages {

  public static class GoHomeMsg extends Message {
  }

  public static class InfectionMsg extends Message {
  }

  public static class InfoExchangeMsg extends Message {
    java.lang.Double newAffiliationSpectrum;
  }

  public static class TestPositivityRateMsg extends Message {
    java.lang.Double testPositivityRate;
  }

  public static class YouInfectedSomeoneMsg extends Message {
    /*
     * These fields are only filled when outputTransmissions is enabled.
     */
    java.lang.Long newlyInfectedAgentId;
    java.lang.Double newlyInfectedCompliancePhysicalDistancing;
    Person.MaskType newlyInfectedMaskType;
    Person.MaskType infectedByMaskType;
    java.lang.Long placeId;
    java.lang.Integer placeType;
  }

  public static class NumPeopleInfectedMsg extends Message.Integer {
  }

  public static class SymptomaticMsg extends Message {
  }

  public static class TestAdministeredMsg extends Message {
  }

  public static class InfectionStatusMsg extends Message implements Copyable {
    public Person.InfectionStatus infectedStatus;
    public double testAccuracy;

    @Override
    public void copyInto(Message msg) {
      if (!(msg instanceof InfectionStatusMsg)) {
        throw new IllegalArgumentException("Message must be InfectionStatusMessage.");
      }
      ((InfectionStatusMsg) msg).infectedStatus = this.infectedStatus;
      ((InfectionStatusMsg) msg).testAccuracy = this.testAccuracy;
    }
  }

  public static class RIPmsg extends Message {
  }

  public static class IAmHereMsg extends Message implements Copyable {
    public Person.PersonTransmissibilityInfo transmissibilityInfo;

    @Override
    public void copyInto(Message message) {
      if (!(message instanceof IAmHereMsg)) {
        throw new IllegalArgumentException("Message must be an IAmHereMsg");
      }
      IAmHereMsg msg = (IAmHereMsg) message;
      msg.transmissibilityInfo = this.transmissibilityInfo;
    }
  }

  public static class PlaceInfections extends Message {
    int placeType;
    int numGotInfected;
    int numStartedInfected;
    int totalInPlace;
  }

  public static class OccupancyMsg extends Message implements Copyable {
    /**
     * A list of occupants of an agent. The last element represents the occupants of the current step, and every
     * previous element represents the occupants in each earlier step.
     */
    ImmutableList<ImmutableList<java.lang.Long>> peoplePresent;

    @Override
    public void copyInto(Message msg) {
      if (!(msg instanceof OccupancyMsg)) {
        throw new IllegalArgumentException("Message must be occupancy message");
      }
      OccupancyMsg occupancyMsg = (OccupancyMsg) msg;
      occupancyMsg.peoplePresent = this.peoplePresent;
    }
  }

  public static class QuarantineOrderMsg extends Message {
    java.lang.Long exposureTime = null;
  }

  public static class QuarantineReleaseMsg extends Message.Empty {
  }

  public static class RequestOccupancyMsg extends Message.Empty {
  }

  public static class StartInterviewMsg extends Message.Empty {
  }

  public static class InterviewResultsMsg extends Message implements Copyable {
    /**
     * A list of contacts of an agent. The last element represents the contacts of the current step, and every
     * previous element represents the contacts in each earlier step.
     */
    ImmutableList<ImmutableSet<java.lang.Long>> contacts;

    @Override
    public void copyInto(Message msg) {
      if (!(msg instanceof InterviewResultsMsg)) {
        throw new IllegalArgumentException("Message must be InterviewResultsMsg");
      }
      InterviewResultsMsg interviewResultsMsg = (InterviewResultsMsg) msg;
      interviewResultsMsg.contacts = this.contacts;
    }
  }

  /**
   * Only to be used in intialization since sending a whole person is heavy... presumably.
   */
  public static class PersonMessage extends Message implements Copyable {
    public Person person;

    @Override
    public void copyInto(Message msg) {
      if (!(msg instanceof PersonMessage)) {
        throw new IllegalArgumentException("Message must be PersonMessage");
      }
      PersonMessage personMessage = (PersonMessage) msg;
      personMessage.person = this.person;
    }
  }

  /**
   * Same note as PersonMessage
   **/
  public static class PlaceMessage extends Message implements Copyable {
    public PlaceInfo placeInfo;

    @Override
    public void copyInto(Message msg) {
      if (!(msg instanceof PlaceMessage)) {
        throw new IllegalArgumentException("Message must be PlaceMessage");
      }
      ((PlaceMessage) msg).placeInfo = this.placeInfo;
    }
  }

  public static class PlaceInfoMessage extends Message.Object<PlaceInfo> {
  }

  public static class PlaceAgentMessage extends Message.Long {
  }

  public static class ScheduleMessage extends Message implements Copyable {
    public Person.DailySchedule schedule;

    @Override
    public void copyInto(Message msg) {
      if (!(msg instanceof ScheduleMessage)) {
        throw new IllegalArgumentException("Message must be ScheduleMessage");
      }
      ((ScheduleMessage) msg).schedule = this.schedule;
    }
  }

  public static class TestSelectionMultiplierMessage extends Message.Double {
  }

  public static class OutputWriterStringMessage extends Message {
    /**
     * One of {@link OutputWriterAgent#KEYS_FILENAMES}
     */
    public String key;
    /**
     * A comma delimited list to be written to a csv corresponding to key
     */
    public String value;
  }

  public static class SupressionStatusMessage extends Message {
    public boolean isSuppressed;
  }

  /**
   * Having all non-empty messages implement this interface allows for us to write proper integration tests.
   *
   * <p>{@link #copyInto(Message)} is ONLY meant to be used in tests.
   */
  interface Copyable {
    /**
     * Copies the {@link Copyable} into {@code msg}. This method mutates {@code msg} and is intended for testing only.
     */
    void copyInto(Message msg);
  }

  public static class ReportForVaccineMsg extends Message.Empty {}

  public static class VaccineAdministeredMsg extends Message.Empty {}
}
