package core;

import com.google.common.collect.ImmutableMap;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An agent who receives Key,Value string pairs and outputs Value to file Key+".csv". Keys are set up
 * in static and cannot be changed dynamically.
 */
public class OutputWriterAgent extends Agent<Globals> {
  public static final String KEY_TRANSMISSIONS = "transmissions";
  private static final String[] KEYS_FILENAMES = {KEY_TRANSMISSIONS};
  private static final ImmutableMap<String, String> HEADERS =
      ImmutableMap.of(
          "transmissions",
          "SimId,Step,InfectingAgentId,isSymptomatic,stepExposure,stepSymptoms,stepRecover,isAsymptomatic," +
              "AgentType,compSymptomsReport,compQuarantineWhenSymptomatic,complianceMask,complianceIsolating," +
              "isSelfIsolatingBecauseOfSymptoms,isSelfIsolatingBecauseOfContactTracing," +
              "complianceIsolateWhenContactNotified,compliancePhysicalDistancing,contactRate," +
              "probHostsAdditionalEvent,probAttendsAdditionalEvent,maskType,placeType,placeId,newlyInfectedAgentId," +
              "newlyInfectedCompliancePhysicalDistancing,newlyInfectedMaskType\n"
      );
  private static final AtomicLong nextSimId = new AtomicLong(0);

  private static final ImmutableMap<String, BufferedWriter> outputWriters;

  static {
    ImmutableMap.Builder<String, BufferedWriter> writers = new ImmutableMap.Builder<>();
    try {
      for (String filename : KEYS_FILENAMES) {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename + ".csv"));
        writer.write(HEADERS.get(filename));
        writers.put(filename, writer);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    outputWriters = writers.build();
  }

  public static void simEnd() {
    for (String key : OutputWriterAgent.KEYS_FILENAMES) {
      try {
        outputWriters.get(key).flush();
      } catch (IOException e) {
        System.out.println("Error flushing " + key);
        e.printStackTrace();
      }
    }
  }

  private final long thisSimId = nextSimId.incrementAndGet();

  private void write(String key, String value) throws IOException {
    if (!outputWriters.containsKey(key)) {
      throw new IllegalStateException("No writer for key " + key);
    }
    outputWriters.get(key).write(thisSimId + "," + getGlobals().tStep + "," + value + "\n");
  }

  /**
   * Receives {@link core.Messages.OutputWriterStringMessage} from agents who want to write stuff.
   * Currently, we write from {@link Person#infectedSomeoneElseWithCOVID}.
   */
  public static Action<OutputWriterAgent> write =
      new Action<OutputWriterAgent>(OutputWriterAgent.class,
          outputWriterAgent -> {
            outputWriterAgent.getMessagesOfType(Messages.OutputWriterStringMessage.class)
                .forEach(
                    msg -> {
                      try {
                        outputWriterAgent.write(msg.key, msg.value);
                      } catch (IOException e) {
                        e.printStackTrace();
                      }
                    }
                );
          });
}
