# The Artificial University (TAU) using Simudyne SDK

## Contributing to the core model
All non-trivial contributions should be made through a pull request and a code review. This means making your own local branch and commiting your changes to it, then pushing the branch to this repo, and then initiating a pull request. Add relevant reviewers to the code review (ghodulik95).

# Code maintenance
We will do our best to follow the [Google Java style guide](https://google.github.io/styleguide/javaguide.html).

I do not have any automatic presubmit processes setup, so for now we are trusting that you will run all tests locally; please add a line to each commit formatted like so

`Test: Tests pass`

indicating that you ran the tests on the most recent version of the commit, and they passed. Please also add detail about new tests you added.

We will regularly coordinate to make a code reformat commit using the google-java-format in intelliJ. It's in everybody's best interest to make your code as compliant as possible, so these reformats will be less intrusive.

# Setting up the project in intelliJ
The project pom.xml file should run with Maven, and *should* import easily into intelliJ. That being said, I have had some problems around libraries not importing when trying to build *from* intelliJ, and I have to add them manually. Each of the libraries that need adding are in the pom.xml. I have had to occasionally reset intelliJ, and that included re-adding JUnit, Truth, and AutoValue.

## Dependencies
Follow the directions at https://portal.simudyne.com/docs/overview/requirements to set up java and Maven. Note, you do not need to separately install Maven if you have it through intelliJ, but it's still a good idea to have, so that you can cross-check Maven outside the intelliJ environment.

## Building and Running
To build and run the project, use `mvn package exec:java`.

Use `mvn clean install` to remove previously compiled Java sources and resources, so that your build will start in a clean state. It will also compile, test, and package the project.

To sync the intelliJ project settings with the POM project settings, right click on the TAUSimudyne project in the Project tab and go to Maven->Update Project.

If you are getting an error about Maven being unable to find the Simudyne libraries, you may have a problem with your VPN. Try disabling the VPN or adding your proxy settings to the Maven settings file: https://maven.apache.org/guides/mini/guide-proxies.html.

# Building and running the CLI (Command-Line Interface) model
The model can be built and ran in command line, taking a .csv or .json file for input parameters.

Build the CLI model with the command `mvn -f pomCLI.xml clean compile package`

Run the model with the command `java -jar target/MCPandemicModel-1.0-SNAPSHOT.jar --model-name TAU --input-path input-dir`
where 'input-dir' is the path to a directory of input csv or json files.

See the [Simudyne documentation](https://portal.simudyne.com/docs/reference/run_mode/cli_run#csv-input-files) on the format of csv or json inputs.

# Running the model from a csv file of inputs
The csvRunner.py script will run the parameters in a csv file in the model and
track outputs.

## The input csv file
A skeleton input file is provided with csvInputSkeleton.csv. This includes all the headers that are allowed in an input csv file. Note that columns can be removed, and this will cause the model to use the default value. Each row of the csv file will be interpreted as one parameter setting to be used. The script allows for setting a number of replications, replicating a model run with a given row of parameters, so you do not need to include multiple identical rows in order to replicate model runs.

## Using the script
First, the model server will need to be running in order for the script to execute successfully. You will also need python installed. You will need to have the Requests library installed to python.

Calls to the script take the form

`python csvRunner.py -i <inputfile> -s <numsteps> -r <replications> -o <outputFileName>`

where

- inputFile is the input csv file
- numSteps is the number of steps each simulation will run for
- replications is the number of replications each parameter setting will be repeated for
- outputFileName is the name of the file where results will be written to

Results are written to the directory csvOutput/. The output will take a similar form to the input csv, with the outputs added as new columns. The outputs include the cumulative infection count, peak number of infections, total deaths, and number of susceptible people remaining at simulation end.

# Deployment
We export our backend as .war (Web Application Resource), which would usually be deployed on an Apache Tomcat server.

Our front end is a simple static web page which connects to the deployed web service through its REST API.

At this time, we just support local deployment and interaction through the static front end.

## Install Apache Tomcat

- Download and install [Apache Tomcat](http://tomcat.apache.org/index.html)
  - I used version [8.5](https://tomcat.apache.org/download-80.cgi), but I don't think that we have any version specific code.
  - Make sure to download the right bit version.
  - You can download a zip or a Windows installer file.
    - If you install the zip, extract it to a directory where you want your install to live.
      - You need to then add that directory to you environment variables (in windows) under the name %CATALINA_HOME%

## Setting up the back end
You first must build the .war file, which can easily be done by running the commmand

`mvn -f pomServlet.xml clean compile package`

The .war file will be built in target/TAOServlet-0.0.1.war.

Then, copy this file to %CATALINA_HOME%/webapps.

Then, start Tomcat with the command

`%CATALINA_HOME%/bin/catalina.bat start`

Viola, the back-end setup is complete.

## Using the front-end
The front end is just static files. Just open src/main/frontend in your browser.
At the time of writing this, it is functional for the exogenous infection use case.

## Using the REST API
To call the REST API directly, use the URL

`http://localhost:8080/TAOServlet-0.0.1/rest/defaultModelRunSingleOutput`

for an example call. We will add more paths as we develop the application.

## State and External Data
To use external data, first you must have an SSH key registered with Github. If you do not,
follow the directions below. Make a copy of the "state.txt" file (perhaps mystate.txt), and 
update it with the path to your SSH key, and your SSH key password. Update the Globals class 
with the name of your state file (Globals#stateFile).

## SSH Key
You must have a correctly formatted SSH token to use external data. To create one, 
use `ssh-keygen -t rsa`. Check the key. If it starts with 
-----BEGIN OPENSSH PRIVATE KEY-----, instead of -----BEGIN RSA PRIVATE KEY-----, 
you must add -m PEM to the command: `ssh-keygen -t rsa -m PEM`.

To add your key to Github, follow the directions at [Adding a new SSH Key to Github](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/adding-a-new-ssh-key-to-your-github-account)
