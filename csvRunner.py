import sys, getopt, os, errno, requests, json
import time

headers = {
    "Cache-Control": "no-cache",
    "Content-Type": "application/json"
}
url = "http://localhost:8080/api/simulations/batch"
CSV_OUTPUT_DIR = 'csvOutput'
CSV_HEADERS = "addRandomLatency,additionalPlaceCompRed,agentContactRateRangeEnd,agentContactRateRangeStart,agentInterviewRecall,baseInfectivity,baseOffCampusExternalInfectionRate,baseOnCampusExternalInfectionRate,cancelSportEvents,closeFitnessCenter,complianceModifier,contactNotifiedNumberOfDaysToIsolate,contactTracingNumberOfDaysTraceback,contactTracingProtocol,csvOutputFilename,daysAfterInfectionToDetect,externalDataCounty,externalDataState,facultyStaffAgentAgeEnd,facultyStaffAgentAgeMean,facultyStaffAgentAgeSD,facultyStaffAgentAgeStart,facultyStaffAgentAttendsPartyEnd,facultyStaffAgentAttendsPartyStart,facultyStaffAgentComplianceIsolateWhenContactNotifiedEnd,facultyStaffAgentComplianceIsolateWhenContactNotifiedStart,facultyStaffAgentCompliancePhysicalDistancingStart,facultyStaffAgentCompliancePhysicalDistancingtEnd,facultyStaffAgentIsolationComplianceEnd,facultyStaffAgentIsolationComplianceStart,facultyStaffAgentMaskComplianceEnd,facultyStaffAgentMaskComplianceStart,facultyStaffAgentProbGoesToOptionalPlaceEnd,facultyStaffAgentProbGoesToOptionalPlaceStart,facultyStaffAgentQuarantineWhenSymptomaticComplianceEnd,facultyStaffAgentQuarantineWhenSymptomaticComplianceStart,facultyStaffAgentReportSymptomsComplianceEnd,facultyStaffAgentReportSymptomsComplianceStart,facultyStaffAgentThrowsPartyEnd,facultyStaffAgentThrowsPartyStart,forceAllAgentsToIsolate,hybridClassesEnabled,includeGradStudents,lastStep,mandateMask,marginalPublicTransitExternalInfectionRate,nActiveAgents,nAgents,numStaffToStudenContacts,numToRandomlyInfect,numToVaccinate,otherIllnessDurationEnd,otherIllnessDurationStart,otherIllnessInfectionRate,outputTransmissions,percAsymptomatic,percHomemadeClothMasks,percInitialInfectedQuarantineOrder,percInitiallyInfected,percInitiallyRecovered,percInitiallyVaccinated,percN95Masks,percOffCampusStudentsWhoUsePublicTransit,percSevere,percSurgicalMasks,placeTypeFlatInfectionRate,runID,showDynamicNetworkAsLinks,studentAgentAgeEnd,studentAgentAgeStart,studentAgentAttendsPartyEnd,studentAgentAttendsPartyStart,studentAgentComplianceIsolateWhenContactNotifiedEnd,studentAgentComplianceIsolateWhenContactNotifiedStart,studentAgentCompliancePhysicalDistancingStart,studentAgentCompliancePhysicalDistancingtEnd,studentAgentIsolationComplianceEnd,studentAgentIsolationComplianceStart,studentAgentMaskComplianceEnd,studentAgentMaskComplianceStart,studentAgentProbGoesToOptionalPlaceEnd,studentAgentProbGoesToOptionalPlaceStart,studentAgentQuarantineWhenSymptomaticComplianceEnd,studentAgentQuarantineWhenSymptomaticComplianceStart,studentAgentReportSymptomsComplianceEnd,studentAgentReportSymptomsComplianceStart,studentAgentThrowsPartyEnd,studentAgentThrowsPartyStart,studentFacingStaffTestMultiplier,suppressAgentType,tOneDay,testDelayTStep,testingAvailableForTracing,testingFalseNegativePerc,testingFalsePositivePerc,testingType,testsPerDay,vaccineEfficacy,cumulativeInfections,peakNumInfected,totDeath,numSusceptible,percPeopleCausing80PercInfections,unknownPlaceInfectionRatioStep,bathroomPlaceInfectionRatioStep,buildingPlaceInfectionRatioStep,campusEventPlaceInfectionRatioStep,discCoursePlaceInfectionRatioStep,nonDiscCoursePlaceInfectionRatioStep,diningHallPlaceInfectionRatioStep,floorPlaceInfectionRatioStep,sportEventPlaceInfectionRatioStep,staffToStudentPlaceInfectionRatioStep,studentGroupPlaceInfectionRatioStep,suitePlaceInfectionRatioStep,fitnessPlaceInfectionRatioStep,officePlaceInfectionRatioStep,unknownPlaceInfectionRatioDay ,bathroomPlaceInfectionRatioDay,buildingPlaceInfectionRatioDay,campusEventPlaceInfectionRatioDay,discCoursePlaceInfectionRatioDay,nonDiscCoursePlaceInfectionRatioDay,diningHallPlaceInfectionRatioDay,floorPlaceInfectionRatioDay,sportEventPlaceInfectionRatioDay,staffToStudentPlaceInfectionRatioDay,studentGroupPlaceInfectionRatioDay,suitePlaceInfectionRatioDay,fitnessPlaceInfectionRatioDay,officePlaceInfectionRatioDay,unknownPlaceInfectionPerc,bathroomPlaceInfectionPerc,buildingPlaceInfectionPerc,campusEventPlaceInfectionPerc,discCoursePlaceInfectionPerc,nonDiscCoursePlaceInfectionPerc,diningHallPlaceInfectionPerc,floorPlaceInfectionPerc,sportEventPlaceInfectionPerc,staffToStudentPlaceInfectionPerc,studentGroupPlaceInfectionPerc,suitePlaceInfectionPerc,fitnessPlaceInfectionPerc,officePlaceInfectionPerc,unknownPlaceTrafficPerc,bathroomPlaceTrafficPerc,buildingPlaceTrafficPerc,campusEventPlaceTrafficPerc,discCoursePlaceTrafficPerc,nonDiscCoursePlaceTrafficPerc,diningHallPlaceTrafficPerc,floorPlaceTrafficPerc,sportEventPlaceTrafficPerc,staffToStudentPlaceTrafficPerc,studentGroupPlaceTrafficPerc,suitePlaceTrafficPerc,fitnessPlaceTrafficPerc,officePlaceTrafficPerc"


def print_usage(arg = None):
    print('csvRunner.py -m <batch|scenario> -i <inputfile> --scenarioIntputFile=<scenarioInputFile> -s <numsteps> -r <replications> -o <outputFileName> --scenarioOutputDir=<scenarioOutputDirectory> -p <port> -a <hostAddress> -ra <retryAttempts>')
    sys.exit(arg)
    
def get_run_id(response, url, poll=1, print_status=False):
    r_json = response.json()
    run_id = r_json['id']
    status = requests.get(url+'/'+run_id)
    if status.status_code == 200:
        while status.json()['progress'] != 1.0 and status.status_code == 200:
            time.sleep(poll)
            status = requests.get(url+'/'+run_id)
            if print_status:
                print(status.json()['progress'])
        return r_json['id']

def get_val_with_correct_type(val):
    try:
        return int(val)
    except:
        pass
        
    try:
        return float(val)
    except:
        pass
    
    if val.upper() == "TRUE" or val.upper() == "FALSE":
        return val == "TRUE"
    return str(val)

"""
Args:
    hostAddress - ip address of running simulation server
    port - port of running simulation server
    inputfile - csv containing rows of starting values for all parameters
    outputfile - name of a csv file to output values
    numSteps - number of simulation steps in a single run
    numSReps - number of sequential repititions for each line of input (the script will send a
        separate API call for each repitition)
    numPReps - number of parallel repititions for each line of input (the script will send a single
        API for each set of parallel reps and the simulation server will handle parallelization)
    retryAttempts - number of retries if the script gets an error from the server
"""
def run_batch_api(hostAddress, port, inputfile, outputfile, numSteps, numSReps, numPReps, retryAttempts):
    url = "http://"+hostAddress+":"+port+"/api/simulations/batch"
    params = []
    first = True
    with open(inputfile) as inputs:
        for line in inputs:
            for i in range(int(numSReps)):
                if (first):
                    params = [x.strip() for x in line.split(',')]
                    first = False
                    break
                else:
                    paramVals = [x.strip() for x in line.split(',')]
                    inputs = {params[j] : get_val_with_correct_type(paramVals[j]) for j in range(len(params))}
                    inputs["lastStep"] = int(numSteps)
                    inputs["csvOutputFilename"] = outputfile
                    data = {
                                "input" : 
                                    {
                                        "system" : inputs
                                    },
                                "ticks": int(numSteps),
                                "seeds": [1234 for _ in range(int(numPReps))],
                                "runs": int(numPReps),
                                "name": "TAU"}
                    numRetries = -1
                    succeeded = False
                    while numRetries < retryAttempts:
                        r = requests.post(url, headers=headers, data=json.dumps(data))
                        if r.status_code == 200:
                            succeeded = True
                            break
                        else:
                            print("ERROR: Response not 200: " + str(r) +". Retrying")
                            numRetries += 1
                    if not succeeded:
                        print("ERROR: No successful post even after retries.")

"""
Params:
    hostAddress,port: [str],[str] the address and port of the model
    initParams: [list[str]] the names of the initial parameters (they will be 
                    mapped to the initial inputs for step 0)
    initInputs: [list[any]] the values of the initial parameters
    scenarioInputFile: [str] the filename of the scenario input file. see 
                        the scenario input file section for a description of 
                        the file format
    scenarioOutputDir: [str] the directory to which Simudyne will direct 
                        Parquet output
    csvOutputFile: [str] the filename of the csv TAU will write output to
    numSteps: [int] the number of simulation steps to run
    numPReps: [int] the number of parallel runs for each scenario, sent as a
            parameter to the REST API. this input will be ignored if seeds or
            seedMap is passed in.
    retryAttempts: [int] the number of times to try resending the API call if 
                    it fails
    batchName: [string] if running scenarios as part of a batch run, this 
                batch name will be added to the scenario name
    seeds: [list[int]] a list of seeds to use. if specified, each scenario will 
            run len(seeds) repititions (and ignore numReps and seedMap)
    seedMap: [dict[int: list[int]]] a dict of scenario id to a list of seeds. 
            will be ignored if seeds is passed in. otherwise, each scenario 
            will use their list of seeds from the map. the lengths of the lists 
            can be different and each scenario will run a number of repititions 
            equal to the length of their seed list
            
Scenario Input File:
    The scenario input file is a csv that holds simulation inputs per step.
    The column format is:
        'scenario': [optional] if included, you can define multiple scenarios 
            in the same input file by putting a scenario ID in the 'scenario'
            column
        'step': [optional] if included, the row will be applied to the step 
            specified in this column
        <parameter>: include a column for any parameter you want to change per 
            step. you do not need to include any parameter that isn't changing, 
            and you do not need to have a value in every row for every parameter
    
    If there is no 'scenario' column, the whole file will be for a single 
    scenario. If there is no 'step' column, the first line will be applied to 
    step 1, the second to step 2, etc.
    
    There are no column order requirements.

"""
def run_scenario_api(hostAddress,port,initParams,initInputs,scenarioInputFile,scenarioOutputDir,csvOutputFile,numSteps,numPReps,retryAttempts,batchName='',seeds=None,seedMap=None):
    url = "http://"+hostAddress+":"+port+"/api/simulations/scenario"
    scenarioParams = []
    first = True
    data = {
            "modelName": "TAU",
            "output": {"uri": scenarioOutputDir},
            }
    scenarios = {}
   
    initData = {initParams[j] : get_val_with_correct_type(initInputs[j]) for j in range(len(initParams))}
    initData["lastStep"] = int(numSteps)
    initData["csvOutputFilename"] = csvOutputFile
    
    with open(scenarioInputFile, encoding='utf-8-sig') as scenarioInputs:
        stepLoc = -1
        scenarioLoc = -1
        stepCount = 0
        for line in scenarioInputs:
            if (first):
                scenarioParams = [x.strip() for x in line.split(',')]
                try:
                    stepLoc = scenarioParams.index("step")
                except ValueError:
                    print("Didn't find step param in scenario file")
                try:
                    scenarioLoc = scenarioParams.index("scenario")
                except ValueError:
                    print("Didn't find scenario number param in scenario file")
                first = False
            else:
                scenarioInputs = [x.strip() for x in line.split(',')]
                if (scenarioInputs[scenarioLoc] if scenarioLoc != -1 else 'default') not in scenarios:
                    scenarios[scenarioInputs[scenarioLoc] if scenarioLoc != -1 else 'default'] = {}
                temp_inputs = {scenarioParams[j]: get_val_with_correct_type(scenarioInputs[j]) for j in range(len(scenarioParams)) if j!=scenarioLoc and j!=stepLoc and scenarioInputs[j] != ''}
                temp_system_inputs = {'system': temp_inputs}
                scenarios[scenarioInputs[scenarioLoc] if scenarioLoc != -1 else 'default'][scenarioInputs[stepLoc] if stepLoc != -1 else stepCount] = temp_system_inputs
                stepCount += 1 if stepLoc == -1 else 0


        for id,val in scenarios.items():
            scenario = {}
            scenario["name"] = "TAU_batch_"+str(batchName)+"_scenario_"+str(id)+"_"+scenarioInputFile.split('.')[0]

            if seeds is not None:
                scenario["seeds"] = seeds
            elif seedMap is not None:
                scenario["seeds"] = seedMap[id]
            else:
                scenario["runs"] = numPReps
            scenario["scenarioData"] = {"0": {"system": initData}}
            scenario["scenarioData"].update(val)
            if str(numSteps) not in scenario["scenarioData"]:
                scenario["scenarioData"][int(numSteps)] = {}
            data["scenarios"] = [scenario]
        
            # with open("json_data.txt", 'w') as json_file:
            #     json.dump(data,json_file)
    
            numRetries = -1
            succeeded = False
            while numRetries < retryAttempts:
                try:
                    r = requests.post(url, headers=headers, data=json.dumps(data))
                    if r.status_code == 200:
                        succeeded = True
                        get_run_id(response=r, url=url, poll=10, print_status=False)
                        break
                    else:
                        print("ERROR: Response not 200: " + str(r) + ". Retrying")
                        numRetries += 1
                except Exception:
                    print("Exception during POST")
                    numRetries += 1
    
            if not succeeded:
                print("ERROR: Not successful post even after retries.")
                
    

def main(argv):

    try:
        os.makedirs(CSV_OUTPUT_DIR)
    except OSError as e:
        if e.errno != errno.EEXIST:
            raise

    mode = 'batch'
    inputfile = ''
    scenarioInputFile = ''
    numSteps = "-1"
    numSReps = "1"
    numPReps = "1"
    outputFile = "csvOutput.csv"
    scenarioOutputDir = "scenarioOutput"
    port = "8080" 
    hostAddress = "localhost"
    retryAttempts = 10
    try:
        opts, args = getopt.getopt(argv,"hm:i:s:r:o:p:a:t:",["mode=","ifile=","scenarioInputFile=","steps=","sequentialReps=","outputFileName=","scenarioOutputDir=","port","hostAddress","retryAttempts=","parallelReps="])
    except getopt.GetoptError:
        print_usage(2)
    for opt, arg in opts:
        if opt == '-h':
            print_usage()
        elif opt in ("-m", "--mode"):
            mode = arg
        elif opt in ("-i", "--ifile"):
            inputfile = arg
        elif opt in ("","--scenarioInputFile"):
            scenarioInputFile = arg
        elif opt in ("-s", "--steps"):
            numSteps = arg
        elif opt in ("-r", "--sequentialReps"):
            numSReps = arg
        elif opt in ("-o", "--outputFileName"):
            outputFile = arg
        elif opt in ("","--scenarioOutputDir"):
            scenarioOutputDir = arg
        elif opt in ("-p", "--port"):
            port = arg
        elif opt in ("-a", "--hostAddress"):
            hostAddress = arg
        elif opt in ("-t", "--retryAttempts"):
            try:
                retryAttempts = int(arg)
            except:
                print("Retry attempts not an int. Defaulting to 10.")
                retryAttempts = 10
        elif opt in ("", "--parallelReps"):
            numPReps = arg
    
    if inputfile == '':
        print("Input file name is required")
        print_usage(2)
    if numSteps == "-1":
        print("Number of steps is required")
        print_usage(2)
    
    # Create empty file. An error is thrown later if the
    # output file does not already exist.
    with open(CSV_OUTPUT_DIR + "/" + outputFile, "w") as f:
        f.write(CSV_HEADERS + "\n")

    
    if mode == 'batch':
        run_batch_api(hostAddress=hostAddress,
                      port=port,
                      inputfile=inputfile,
                      outputfile=outputFile,
                      numSteps=numSteps,
                      numSReps=numSReps,
                      numPReps=numPReps,
                      retryAttempts=retryAttempts)
    elif mode == 'scenario':
        if scenarioInputFile == '':
            print("scenarioInputFile is required for scenario runs")
            print_usage(2)
        seeds = None
        seedMap = None
        first = True
        initParams = []
        batchNum = 1
        with open(inputfile) as initInputs:
            for line in initInputs:
                for i in range(int(numSReps)):
                    if first:
                        first = False
                        initParams = [x.strip() for x in line.split(',')]
                        break
                    else:
                        initInputs = [x.strip() for x in line.split(',')]
                        run_scenario_api(hostAddress=hostAddress,
                                        port=port,
                                        initParams=initParams,
                                        initInputs=initInputs,
                                        scenarioInputFile=scenarioInputFile,
                                        scenarioOutputDir=scenarioOutputDir,
                                        csvOutputFile=outputFile,
                                        numSteps=numSteps,
                                        numPReps=numPReps,
                                        retryAttempts=retryAttempts,
                                        batchName=batchNum,
                                        seeds=seeds,
                                        seedMap=seedMap
                                        )
                        batchNum += 1
    else:
        print("Mode needs to be 'batch' or 'scenario'")
        print_usage(2)


if __name__ == "__main__":
   main(sys.argv[1:])
