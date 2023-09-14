# -*- coding: utf-8 -*-
"""
Created on Fri Feb 12 14:26:49 2021

@author: GMAAYAN
"""
import os, sys, getopt
from pathlib import Path
import pandas as pd
import numpy as np

'''
    Utilities to convert the parquet output from Simudyne runs into pandas 
    dataframes and then into CSV files.
    
    The parquet files output from Simudyne are a single column. The column name 
    is the name of the SchemaRecord used to create the output channel.
        e.g. 
        ScemaRecord output = new SchemaRecord(<The Column Name>);
        getContext().getChannels().createOutputChannel().setSchema(output);
    Each row of the column is a dict of the form 
        {<Field Name>: <Field Value>, ...}
    
    The keys in the dict are the SchemaField names that are set in the 
    SchemaRecord. Simudyne also automatically adds a 'tick' field and a 'time' 
    field. (Note: When creating your SchemaRecord, you don't need to add a 
            'step' field since Simudyne automatically adds the 'tick' field.)
    
    I save the name of the column so that it can be used as the name of the 
    csv output file, and then create a pandas dataframe with a column for each 
    key in the dict. I use the 'tick' field as the index for the dataframe.

'''

pos_rate = {}
ext_infect = {}
num_infected = {}
num_infectious = {}
new_infected = {}
tot_given = {}
tot_returned = {}
pos_given = {}
pos_returned = {}
First = True




'''
    Convert a pandas dataframe to a csv
    
    name: the name of the dataframe, used as the name of the csv file
    df: the dataframe
    output_dir: the starting output directory for csv's
    run_id: the run_id of the df, potentially added to the filename or used in 
            the file structure
    add_run_id_to_name: whether to prepend the run_id to the csv filename
    sep_csv_by_run_id: whether to place df's from the same run in a folder 
                       under output_dir
'''
def df_to_csv(name, df, output_dir, run_id=None, add_run_id_to_name=False, 
              sep_csv_by_run_id=True):
    output_filename = name
    if run_id is not None and add_run_id_to_name:
        output_filename = run_id + '_' + output_filename
    output_filename = output_filename + '.csv'
    
    output_directory = output_dir 
    if run_id is not None and sep_csv_by_run_id:
        output_directory = os.path.join(output_directory,run_id)
    if not os.path.exists(output_directory):
        Path(output_directory).mkdir(parents=True)
    
    df.to_csv(os.path.join(os.getcwd(),output_directory,output_filename))


'''
    Convert a parquet file to a pandas dataframe
    Since Simudyne automatically adds a "tick" column, we use that as the index
    
    f: the filepath of the parquet file
    returns - 
        name: the name of the dataframe
        df: the dataframe
'''
def parquet_to_df(f):
    if os.stat(f).st_size == 0:
        return "", None
    first_df = pd.read_parquet(f, engine='pyarrow')
    if len(first_df) == 0:
        return "", None
    name = first_df.columns[0]
    df = pd.DataFrame(first_df[name].tolist())
    
    df = df.set_index('tick')
    
    return name, df

'''
    Pull out the batch name and scenario name from the parquet filename.
    Note: Only works with the output from Simudyne, if using this with other
        parquet files, you will need to edit the "get_all_parquet_files()" 
        function.
    
    f: the filename
    returns -
        A dict of the form
            {
                "batch": batch_name
                "scenario": scenario_name
            }
'''
def parse_filename(f):
    parts = f.split("_")
    ret = {}
    for i in range(len(parts)):
        if parts[i] == "batch":
            ret["batch"] = parts[i+1]
        elif parts[i] == "scenario":
            ret["scenario"] = parts[i+1]
    return ret

"""
    Calculates the test positivity
    
    Currently calculates it as Positive Tests Returned / Total Tests Given
    Make changes here to change the positivity rate calculation
"""
def calc_pos_rate(tSeriesDF):
    tests_given = tSeriesDF["totaltestsgiven"].to_numpy().astype(float)
    pos_returned = tSeriesDF["positivetestsreturned"].to_numpy().astype(float)
    
    pos_rate = np.divide(pos_returned, tests_given, out=np.zeros_like(pos_returned), where=tests_given!=0)
    
    return pos_rate

'''
    Find all of the parquet files in a directory (recursively) and convert 
    them to dataframes. Also pulls out the run_id of the parquet file.
    
    directory: starting directory to search in, will search in all nested 
               directories too
    returns - 
        tables: dict in the form { dataframe name: [(dataframe, run_id), ...] }
'''
def get_all_parquet_files(directory):
    files = [f.__str__() for f in Path(directory).rglob('*.parquet')]
    tables = {}
    for f in files:
        name, df = parquet_to_df(f)
        parsed_filename = parse_filename(f)
        if df is None:
            continue
        (head, _) = os.path.split(f) # pull off parquet file
        (head, _) = os.path.split(head) # pull off output name directory
        (head, _) = os.path.split(head) # pull off 'runs' directory
        (_, run_id) = os.path.split(head) # pull out run ID
        
        name_extension = "_s_" + parsed_filename["scenario"]
        full_name = name+name_extension
        if full_name not in tables:
            tables[full_name] = []
        tables[full_name].append((df, run_id))
        if name == "TimeSeriesOutputs":
            global First
            if First:
                #pos_rate["tick"] = df["tick"]
                First = False
            if "runID" in df.columns:
                n = df["runID"][0]
                addition = 1
                n_full = n + name_extension + '_' + str(addition)
                while n_full in pos_rate:
                    addition = addition + 1
                    n_full = n + name_extension + '_' + str(addition)
                pos_rate[n_full] = calc_pos_rate(df)
                ext_infect[n_full] = df["extinfections"]
                num_infected[n_full] = df["numInfected"]
                num_infectious[n_full] = df["numInfectious"]
                new_infected[n_full] = df["numNewInfections"]
                tot_given[n_full] = df["totaltestsgiven"]
                tot_returned[n_full] = df["totaltestsreturned"]
                pos_given[n_full] = df["positivetestsgiven"]
                pos_returned[n_full] = df["positivetestsreturned"]
    
    return tables


'''
    Convert all parquet files from a particular run to dataframes.
    
    run_dir: the base parquet output directory (e.g. "OutputData")
    run_id: the run_id to pull parquets from
    returns - see the return of get_all_parquet_files()
'''
def get_run_parquet_files(run_dir, run_id):
    directory = os.path.join(os.getcwd(),run_dir,run_id)
    return get_all_parquet_files(directory)


'''
    Get a list of all the dataframes with a particular name from the dict 
    described above.
    
    tables: the dict described above
    table_name: the name of the tables to pull out (will check for table_name,
                                                    table_name.upper(), and 
                                                    table_name.lower())
    returns - 
        tables[n]: the list of tables matching table_name in tables
'''
def get_named_table_from_tables(tables, table_name):
    n = None
    if table_name in tables:
        n = table_name
    elif table_name.lower() in tables:
        n = table_name.lower()
    elif table_name.upper() in tables:
        n = table_name.upper()
    
    if n is not None:
        return tables[n]

'''
    Get a specified run's table from a list of tables
    
    tables: list of tables in the form [(df, run_id), (df, run_id), ...]
    run_id: the run_id to search for
    returns -
        df: the datafrom matching the run_id or None if not found
'''
def get_run_from_list_of_tables(tables, run_id):
    for (df, r_id) in tables:
        if r_id == run_id:
            return df


'''
    Convert all parquet tables with a specific name to dataframes.
    
    directory: the starting directory to search in (will search recursively)
    table_name: the name of the tables to pull out
    returns - the dict described above
'''
def get_named_parquet_files(directory, table_name):
    tables = get_all_parquet_files(directory)
    return get_named_table_from_tables(tables, table_name)


'''
    Convert the parquet table with a specific name and run_id to a dataframe
    
    directory: the starting directory to search in (will search recursively)
    table_name: the name of the table to pull out
    run_id: the run_id of the table to pull out
    returns - the dataframe matching the table_name and run_id or None if not 
              found
'''
def get_named_run_parquet_files(directory, table_name, run_id):
    tables_with_name = get_named_parquet_files(directory, table_name)
    return get_run_from_list_of_tables(tables_with_name, run_id)

'''
    Convert all parquet files to csv files. The search for parquet files starts
    in the current working directory and searches recursively. Outputs CSV files 
    in a similar file structure to Simudyne's output structure
    
    output_dir: the directory to output the CSV files
'''
def convert_all_parquet_to_csv(output_dir):
    tables = get_all_parquet_files(os.getcwd())
    for name,named_tables in tables.items():
        for (table, run_id) in named_tables:
            df_to_csv(name=name,df=table,output_dir=output_dir,run_id=run_id,
                      add_run_id_to_name=False,sep_csv_by_run_id=True)


'''
    Convert all parquet files in a directory to csv files. The search for 
    parquet files starts searches recursively. Outputs CSV files in a similar 
    file structure to Simudyne's output structure
    
    input_dir: the directory to start searching for parquet files
    output_dir: the directory to output the CSV files
'''
def convert_all_parquet_in_dir_to_csv(input_dir, output_dir):
    tables = get_all_parquet_files(os.path.join(os.getcwd(), input_dir))
    for name,named_tables in tables.items():
        for (table, run_id) in named_tables:
            df_to_csv(name=name,df=table,output_dir=output_dir,run_id=run_id,
                      add_run_id_to_name=False,sep_csv_by_run_id=True)
    
        
def print_usage():
    print("Incorrect usage(all args are optional):")
    print("-i <input-directory> -o <output-directory>")
    print("Defaults are:")
    print("-i <None> -o <scenarioOutputCSV>")
    sys.exit()

def main(argv):
    inputDirectory = None
    outputDirectory = "scenarioOutputCSV"
    
    try:
        opts, args = getopt.getopt(argv, "i:o:",["inputDirectory=","outputDirectory="])
    except getopt.GetoptError:
        print_usage()
    
    for opt, arg in opts:
        if opt in ("-i", "--inputDirectory"):
            inputDirectory = arg
        elif opt in ("-o", "--outputDirectory"):
            outputDirectory = arg
        else:
            print_usage()

    if inputDirectory is None:
        convert_all_parquet_to_csv(outputDirectory)
    else:
        convert_all_parquet_in_dir_to_csv(inputDirectory, outputDirectory)
    
    if pos_rate:
        pos_rate_df = pd.DataFrame(dict([ (k, pd.Series(v)) for k,v in pos_rate.items() ]))
        df_to_csv(name = "positiveRate", df = pos_rate_df, output_dir = outputDirectory, sep_csv_by_run_id=False)
    if ext_infect:
        ext_infect_df = pd.DataFrame(dict([ (k, pd.Series(v)) for k,v in ext_infect.items() ]))
        df_to_csv(name = "externalInfections", df = ext_infect_df, output_dir = outputDirectory, sep_csv_by_run_id=False)
    if num_infected:
        num_infected_df = pd.DataFrame(dict([ (k, pd.Series(v)) for k,v in num_infected.items() ]))
        df_to_csv(name = "numInfected", df = num_infected_df, output_dir = outputDirectory, sep_csv_by_run_id=False)
    if num_infectious:
        num_infectious_df = pd.DataFrame(dict([ (k, pd.Series(v)) for k,v in num_infectious.items() ]))
        df_to_csv(name = "numInfectious", df = num_infectious_df, output_dir = outputDirectory, sep_csv_by_run_id=False)
    if new_infected:
        new_infected_df = pd.DataFrame(dict([ (k, pd.Series(v)) for k,v in new_infected.items() ]))
        df_to_csv(name = "numNewInfected", df = new_infected_df, output_dir = outputDirectory, sep_csv_by_run_id=False)
    if tot_given:
        tot_given_df = pd.DataFrame(dict([ (k, pd.Series(v)) for k,v in tot_given.items() ]))
        df_to_csv(name = "totTestsGiven", df = tot_given_df, output_dir = outputDirectory, sep_csv_by_run_id=False)
    if tot_returned:
        tot_returned_df = pd.DataFrame(dict([ (k, pd.Series(v)) for k,v in tot_returned.items() ]))
        df_to_csv(name = "totTestsReturned", df = tot_returned_df, output_dir = outputDirectory, sep_csv_by_run_id=False)
    if pos_given:
        pos_given_df = pd.DataFrame(dict([ (k, pd.Series(v)) for k,v in pos_given.items() ]))
        df_to_csv(name = "posTestsGiven", df = pos_given_df, output_dir = outputDirectory, sep_csv_by_run_id=False)
    if pos_returned:
        pos_returned_df = pd.DataFrame(dict([ (k, pd.Series(v)) for k,v in pos_returned.items() ]))
        df_to_csv(name = "posTestsReturned", df = pos_returned_df, output_dir = outputDirectory, sep_csv_by_run_id=False)


if __name__ == "__main__":
    main(sys.argv[1:])