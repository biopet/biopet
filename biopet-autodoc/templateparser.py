#! /usr/bin/env python2

#Script to document new tools using a jinja2 template
from jinja2 import Environment, FileSystemLoader
import os
import argparse
import ast
import yaml

if __name__ == "__main__":
    
    #Block with command line options
    parser=argparse.ArgumentParser(description="")
    parser.add_argument("-t", type=str, nargs=1, help="Templatefile to be used")
    parser.add_argument("-o", type=str, nargs=1, help="output file")
    parser.add_argument("-N","--tool-name", type=str, nargs=1, help="name of the tool")
    parser.add_argument("-O", "--tool-output", type=str, nargs='?', default='')
    parser.add_argument("-R", "--tool-run", type=str, nargs='?', default='')
    parser.add_argument("-L", "--tool-option-list", type=str, nargs='?', default= '', help="Text file containing the --help output of the tool")

    #parse arguments
    arguments=parser.parse_args()
    template=arguments.t[0]
    output_file=arguments.o[0]
    tool=dict()
    tool['name']=arguments.tool_name[0]
    tool['output']=arguments.tool_output
    tool['run']=arguments.tool_run
    help_file=open(arguments.tool_option_list, 'r')
    tool['option_list']=help_file.read()   #This reads the option list file to a raw string

    # Open jinja2 template and render it with variables
    j2_env = Environment(loader=FileSystemLoader("/"))
    rendered = j2_env.get_template(template).render(tool=tool)

    # Write the template results to the output file
    file=open(output_file, "w")
    file.seek(0)
    file.write(rendered)
    file.close()
