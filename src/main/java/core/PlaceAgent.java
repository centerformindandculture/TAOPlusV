package core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.graph.Message;
import simudyne.core.rng.SeededRandom;
import tau.TAUModel;

import java.util.*;
import java.util.stream.Collectors;

import static core.Utils.sortedCopyBySender;

public class PlaceAgent extends Agent<Globals> {

    private long placeId;
    private PlaceInfo placeInfo;

    public void init() {
        this.placeId = this.getID();
    }

    public long placeId() {
        return this.placeId;
    }

    public PlaceInfo place() {
        return this.placeInfo;
    }

    public static Action<PlaceAgent> initPlaceAgent =
            Action.create(
                    PlaceAgent.class,
                    pla -> {
                        pla.init();
                    }
            );

    public static Action<PlaceAgent> sendSelfToCentralAgent =
            Action.create(
                    PlaceAgent.class,
                    pla -> {
                        pla.send(Messages.PlaceAgentMessage.class,
                                        msg -> msg.setBody(pla.getID()))
                                .to(pla.getGlobals().centralAgentID);
                    }
            );

    @VisibleForTesting
    void setPlaceInfo(PlaceInfo placeInfo) {
        this.placeInfo = placeInfo;
    }

    // Because the number of Places is not known at the start of the simulation, one PlaceAgent
    // is generated initially in VIVIDCoreModel#setup and then that PlaceAgent uses the created
    // places to initialize itself and then spawn and initialize all of the other PlaceAgents
    public static Action<PlaceAgent> receivePlace =
            Action.create(
                    PlaceAgent.class,
                    pla -> {
                        List<PlaceInfo> placeInfoList = pla
                                .getMessagesOfType(Messages.PlaceMessage.class)
                                .stream()
                                .map(msg -> msg.placeInfo)
                                .collect(Collectors.toList());

                        // This initializes the single PlaceAgent that gets generated in VIVIDCoreModel#setup
                        // before spawning all of the other PlaceAgents
                        pla.setPlaceInfo(placeInfoList.get(0));
                        pla.placeInfo.receivePlaceAgent(pla.getID());

                        for (int i = 1; i < placeInfoList.size(); i++) {
                            int finalI = i;
                            pla.spawn(PlaceAgent.class, agent -> {
                                agent.setPlaceInfo(placeInfoList.get(finalI));
                                agent.placeInfo.receivePlaceAgent(agent.getID());
                            });
                        }
                    }
            );

    /**
     * Receives {@link Messages.IAmHereMsg} from {@link Person#executeMovement}
     * Generates contacts and infections from the people present
     * Sends {@link Messages.InfectionMsg} to {@link Person#infectedByCOVID}
     * Send {@link Messages.YouInfectedSomeoneMsg} to {@link Person#infectedSomeoneElseWithCOVID}
     * Send {@link Messages.PlaceInfections} to {@link CentralAgent#processPlaceInfectionRates}
     */
    public static Action<PlaceAgent> generateContactsAndInfect =
            Action.create(
                    PlaceAgent.class,
                    pla -> {
                        ImmutableList<Long> peoplePresent = ImmutableList.of();
                        if (pla.hasMessagesOfType(Messages.IAmHereMsg.class)) {
                            PlaceInfo pl = pla.place();
                            List<Messages.IAmHereMsg> msgs = pla.getMessagesOfType(Messages.IAmHereMsg.class);
                            ImmutableList.Builder<Long> builder = new ImmutableList.Builder<>();
                            msgs.stream().map(msg -> msg.getSender()).sorted().distinct().forEach(builder::add);
                            peoplePresent = builder.build();

                            int totalInPlace = peoplePresent.size();
                            Collection<ContactEventInfo> contacts = pla.getWhoToInfect(
                                    sortedCopyBySender(msgs),
                                    pla.getGlobals(), pla.getPrng());

                            contacts.stream()
                                    .filter(ContactEventInfo::resultedInTransmission)
                                    .forEachOrdered(
                                            transmission -> {
                                                pla.send(Messages.InfectionMsg.class).to(transmission.infected());
                                                final boolean outputTransmissions = pla.getGlobals().outputTransmissions;
                                                transmission.infectedBy().ifPresent(infectedBy -> {
                                                    pla.send(Messages.YouInfectedSomeoneMsg.class, msg -> {
                                                        if (outputTransmissions) {
                                                            msg.newlyInfectedAgentId = transmission.infected();
                                                            msg.newlyInfectedMaskType =
                                                                    transmission.infectedTransmiissibilityInfo().get().wearsMask();
                                                            msg.newlyInfectedCompliancePhysicalDistancing =
                                                                    transmission.infectedTransmiissibilityInfo().get().physicalDistCompliance();
                                                            msg.infectedByMaskType =
                                                                    transmission.infectedByTransmiissibilityInfo().get().wearsMask();
                                                            msg.placeId = transmission.placeId();
                                                            msg.placeType = transmission.placeType();
                                                        }
                                                    }).to(infectedBy);
                                                });
                                            }
                                    );


                            Collection<ValueChangeContactEvent> valueChangeContacts = pla.getValueChangeContacts(
                                    sortedCopyBySender(msgs),
                                    pla.getGlobals(), pla.getPrng());

                            valueChangeContacts.forEach(contact -> {
                                pla.send(Messages.InfoExchangeMsg.class,
                                        m -> m.newAffiliationSpectrum = contact.alterNewAffiliationValue())
                                        .to(contact.alterId());
                            });

                            int numStartedInfected = (int) msgs.stream()
                                    .filter(msg -> msg.transmissibilityInfo.status() == Person.InfectionStatus.INFECTED)
                                    .count();
                            int numGotInfected = (int) contacts.stream()
                                    .filter(ContactEventInfo::resultedInTransmission)
                                    .count();

                            pla.send(
                                            Messages.PlaceInfections.class,
                                            msg -> {
                                                msg.placeType = pl.placeType();
                                                msg.numGotInfected = numGotInfected;
                                                msg.numStartedInfected = numStartedInfected;
                                                msg.totalInPlace = totalInPlace;
                                            })
                                    .to(pla.getGlobals().centralAgentID);

                        }
                    }
            );

    public List<Messages.IAmHereMsg> processConformity(List<Messages.IAmHereMsg> msgs, SeededRandom random, Globals globals) {
        Random r = new Random(1234L);

        List<Messages.IAmHereMsg> toReturn = new ArrayList<>();
        List<Messages.IAmHereMsg> msgsCopy = new ArrayList<>(msgs);
        Collections.shuffle(msgsCopy, r);

        // Simulate agent entrance order
        int agentCount = msgs.size();
        int numWearingMask = 0;
        double distancingComplianceSum = 0;
        for (Messages.IAmHereMsg msg : msgsCopy) {
            numWearingMask += msg.transmissibilityInfo.wearsMask() != Person.MaskType.NONE ? 1 : 0;
            distancingComplianceSum += msg.transmissibilityInfo.physicalDistCompliance();
        }
        boolean majorityWearingMask = numWearingMask > agentCount / 2;
        double avgCompliance = distancingComplianceSum / agentCount;
        for (Messages.IAmHereMsg msg : msgsCopy) {
            boolean isWearingMask = msg.transmissibilityInfo.wearsMask() != Person.MaskType.NONE;
            double physicalDistanceComp = msg.transmissibilityInfo.physicalDistCompliance();
            Person.PersonTransmissibilityInfo.Builder transmissibilityInfo = msg.transmissibilityInfo.toBuilder();
            if (globals.conformityMaskEnabled &&
                    majorityWearingMask == (msg.transmissibilityInfo.wearsMask() == Person.MaskType.NONE)) {
                double coinflip = random.uniform(0, 1).sample();
                if (coinflip < msg.transmissibilityInfo.conformityScore()) {
                    transmissibilityInfo.wearsMask(isWearingMask ? Person.MaskType.NONE : Person.MaskType.N95);
                }
            }
            if (globals.conformityDistancingEnabled) {
                double amtToChage = msg.transmissibilityInfo.conformityScore() * (avgCompliance - physicalDistanceComp);
                transmissibilityInfo.physicalDistCompliance(physicalDistanceComp + amtToChage);
            }
            msg.transmissibilityInfo = transmissibilityInfo.build();
            toReturn.add(msg);
        }
        return toReturn;
    }

    public Collection<ContactEventInfo> getWhoToInfect(
            List<Messages.IAmHereMsg> occupantsOrig, Globals globals, SeededRandom random) {

        List<Messages.IAmHereMsg> occupants = processConformity(occupantsOrig, random, globals);
        HashMap<Long, ContactEventInfo> toInfect = new HashMap<>();

        if (occupants.size() <= 1) {
            return ImmutableList.of();
        }

        final Optional<Messages.IAmHereMsg> centerOccupant =
                occupants.stream().filter(msg -> msg.getSender() == this.place().center()).findFirst();
        if (this.place().networkType() == PlaceInfo.NetworkType.FULLY_CONNECTED_DEPENDENT_ON_CENTER
                || this.place().networkType() == PlaceInfo.NetworkType.STAR) {
            // The center agent is a no-show, so the event technically does not happen.
            // No infections.
            if (!centerOccupant.isPresent()) {
                return ImmutableList.of();
            }
        }

        double baseInfectionRate =
                globals.getInfectionRate(this.place().placeType())
                        / globals.tOneDay;

        if (this.place().networkType() == PlaceInfo.NetworkType.STAR) {
            assert centerOccupant.isPresent();
            List<Messages.IAmHereMsg> contactedAgents =
                    DefaultModulesImpl.sample(
                            DefaultModulesImpl.getAllExcept(occupants, centerOccupant.get()),
                            globals.numStaffToStudenContacts,
                            random);
            for (Messages.IAmHereMsg occupant : contactedAgents) {
                if (occupant.getSender() == centerOccupant.get().getSender()) {
                    continue;
                }
                Messages.IAmHereMsg infected;
                Messages.IAmHereMsg infectee;
                if (centerOccupant.get().transmissibilityInfo.isInfectious()) {
                    infected = centerOccupant.get();
                    infectee = occupant;
                } else if (occupant.transmissibilityInfo.isInfectious()) {
                    infected = occupant;
                    infectee = centerOccupant.get();
                } else {
                    continue;
                }
                toInfect.put(
                        infectee.getSender(),
                        ContactEventInfo.create(
                                // TODO I think infected and infectee need to be swapped in these first two parameters
                                infected.getSender(),
                                Optional.of(infectee.getSender()),
                                this.placeId(),
                                DefaultModulesImpl.willInfect(infected, infectee, baseInfectionRate, random),
                                this.placeInfo.placeType(),
                                globals.outputTransmissions ? infectee.transmissibilityInfo : null,
                                globals.outputTransmissions ? infected.transmissibilityInfo : null));
            }
        }

        if (this.place().networkType() == PlaceInfo.NetworkType.FULLY_CONNECTED
                || this.place().networkType() == PlaceInfo.NetworkType.FULLY_CONNECTED_DEPENDENT_ON_CENTER) {
            for (int i = 0; i < occupants.size(); i++) {
                if (occupants.get(i).transmissibilityInfo.isInfectious()) {
                    List<Messages.IAmHereMsg> contactedAgents =
                            DefaultModulesImpl.sample(
                                    DefaultModulesImpl.getAllExcept(occupants, occupants.get(i)),
                                    occupants.get(i).transmissibilityInfo.contactRate(),
                                    random);
                    for (Messages.IAmHereMsg otherAgent : contactedAgents) {
                        toInfect.put(
                                otherAgent.getSender(),
                                ContactEventInfo.create(
                                        otherAgent.getSender(),
                                        Optional.of(occupants.get(i).getSender()),
                                        this.placeId(),
                                        DefaultModulesImpl.willInfect(occupants.get(i), otherAgent, baseInfectionRate, random),
                                        this.placeInfo.placeType(),
                                        globals.outputTransmissions ? otherAgent.transmissibilityInfo : null,
                                        globals.outputTransmissions ? occupants.get(i).transmissibilityInfo : null));
                    }
                }
            }
        }
        return ImmutableList.sortedCopyOf(
                Comparator.comparingLong(ContactEventInfo::infected), toInfect.values());
    }

    public Optional<ValueChangeContactEvent> getValueExchangeContact(Messages.IAmHereMsg agent1, Messages.IAmHereMsg agent2, Globals globals, SeededRandom random) {
        double coin = random.uniform(0, 1).sample();
        if (coin > globals.infoExchangeLikelihood) {
            return Optional.empty();
        }

        boolean agent1Wins = random.uniform(0, 1).sample() < 0.5;
        Messages.IAmHereMsg winnerAgent = agent1Wins ? agent1 : agent2;
        Messages.IAmHereMsg loserAgent = agent1Wins ? agent2 : agent1;

        double newAffiliationValue = winnerAgent.transmissibilityInfo.affiliationSpectrum();
        return Optional.of(
                ValueChangeContactEvent.create(loserAgent.getSender(),
                        newAffiliationValue));
    }

    public Collection<ValueChangeContactEvent> getValueChangeContacts(
            List<Messages.IAmHereMsg> occupantsOrig, Globals globals, SeededRandom random) {

        List<Messages.IAmHereMsg> occupants = processConformity(occupantsOrig, random, globals);
        HashMap<Long, List<ValueChangeContactEvent>> toReturn = new HashMap<>();

        if (occupants.size() <= 1) {
            return ImmutableList.of();
        }

        final Optional<Messages.IAmHereMsg> centerOccupant =
                occupants.stream().filter(msg -> msg.getSender() == this.place().center()).findFirst();
        if (this.place().networkType() == PlaceInfo.NetworkType.FULLY_CONNECTED_DEPENDENT_ON_CENTER
                || this.place().networkType() == PlaceInfo.NetworkType.STAR) {
            // The center agent is a no-show, so the event technically does not happen.
            // No infections.
            if (!centerOccupant.isPresent()) {
                return ImmutableList.of();
            }
        }


        if (this.place().networkType() == PlaceInfo.NetworkType.STAR) {
            assert centerOccupant.isPresent();
            List<Messages.IAmHereMsg> contactedAgents =
                    DefaultModulesImpl.sample(
                            DefaultModulesImpl.getAllExcept(occupants, centerOccupant.get()),
                            globals.infoExchangeContactRate,
                            random);
            for (Messages.IAmHereMsg occupant : contactedAgents) {
                if (occupant.getSender() == centerOccupant.get().getSender()) {
                    continue;
                }
                Messages.IAmHereMsg agent1 = centerOccupant.get();
                Messages.IAmHereMsg agent2 = occupant;
                this.getValueExchangeContact(agent1, agent2, globals, random).ifPresent(
                        contact -> {
                            if (!toReturn.containsKey(contact.alterId())) {
                                toReturn.put(contact.alterId(), new ArrayList<>());
                            }
                            toReturn.get(contact.alterId()).add(contact);
                        }
                );
            }
        }

        if (this.place().networkType() == PlaceInfo.NetworkType.FULLY_CONNECTED
                || this.place().networkType() == PlaceInfo.NetworkType.FULLY_CONNECTED_DEPENDENT_ON_CENTER) {
            for (int i = 0; i < occupants.size(); i++) {
                List<Messages.IAmHereMsg> contactedAgents =
                        DefaultModulesImpl.sample(
                                DefaultModulesImpl.getAllExcept(occupants, occupants.get(i)),
                                globals.infoExchangeContactRate,
                                random);
                for (Messages.IAmHereMsg otherAgent : contactedAgents) {
                    this.getValueExchangeContact(occupants.get(i), otherAgent, globals, random).ifPresent(
                            contact -> {
                                if (!toReturn.containsKey(contact.alterId())) {
                                    toReturn.put(contact.alterId(), new ArrayList<>());
                                }
                                toReturn.get(contact.alterId()).add(contact);
                            }
                    );
                }
            }
        }

        ImmutableList.Builder<ValueChangeContactEvent> builder = new ImmutableList.Builder<>();
        for (Map.Entry<Long, List<ValueChangeContactEvent>> entry : toReturn.entrySet()) {
            if (entry.getValue().size() == 1) {
                builder.add(entry.getValue().get(0));
                continue;
            }

            OptionalDouble averageValueAffiliation = entry.getValue().stream()
                    .mapToDouble(ValueChangeContactEvent::alterNewAffiliationValue)
                    .average();
            if (!averageValueAffiliation.isPresent()) {
                continue; // Shouldn't happen
            }
            builder.add(ValueChangeContactEvent.create(entry.getKey(), averageValueAffiliation.getAsDouble()));
        }

        return ImmutableList.sortedCopyOf(
                Comparator.comparingLong(ValueChangeContactEvent::alterId), builder.build());
    }
}
