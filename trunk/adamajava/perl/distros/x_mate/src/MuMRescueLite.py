#!/usr/bin/env python
# MuMRescueLite.py
# Copyright (c) 2009 Takehiro Hashimoto, Michiel J. L. deHoon, Geoffrey J. Faulkner
# Released under MIT licence; see LICENCE.txt
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.


import sys

################################
def retPosition(columns):
	### definitions
	chrIndex = 2
	strandIndex = 3
	startIndex = 4
	stopIndex = 5
	### main
	strand = columns[strandIndex]
	if strand == "+":
		temp = [columns[chrIndex], columns[strandIndex], int(columns[startIndex])]
		return temp
	elif strand == "-":
		temp = [columns[chrIndex], columns[strandIndex], int(columns[stopIndex])]
		return temp
	elif strand == "F":
		temp = [columns[chrIndex], "+", int(columns[startIndex])]
		return temp
	elif strand == "R":
		temp = [columns[chrIndex], "-", int(columns[stopIndex])]
		return temp
	else:
		raise Exception 

################################
def remainSingleMapper(inFile, singleMappers, inputHeaderFlag):
	### definitions
	mapPositionIndex = 1
	expressionIndex = 6
	### main loop
	f = open(inFile)
	if inputHeaderFlag:
		line = f.readline()
	for line in f:
		line = line.strip()
		columns = line.split("\t")
		mapPositions = float(columns[mapPositionIndex])
		if mapPositions == 1 and float(columns[expressionIndex]) > 0:
			chromosome, strand, position = retPosition(columns)
			chromosome_strand = "_".join([chromosome, strand])
			if not chromosome_strand in singleMappers:
				singleMappers[chromosome_strand] = {}
			if not position in singleMappers[chromosome_strand]:
				singleMappers[chromosome_strand][position] = 0
			singleMappers[chromosome_strand][position] += float(columns[expressionIndex])
	f.close()

################################
def multiMapRescue(inFile, singleMappers, window, o, inputHeaderFlag, disposeNoNearbyFlag):
	### definitions
	tagSequence = ""
	tagIndex = 0
	mapPositionIndex = 1
	### main loop
	f = open(inFile)
	if inputHeaderFlag:
		line = f.readline()
	for line in sorted(f):
		### data preparing
		line = line.strip()
		columns = line.split("\t")
		### multimap rescue by each sequence
		if tagSequence :
			if tagSequence != columns[tagIndex]:
				### submit data to output function
				printResult(mapPositions, o, tag2position, disposeNoNearbyFlag)
				### re-initialization and value set for variables
				mapPositions = int(columns[mapPositionIndex])
				tagSequence = columns[tagIndex]
				tag2position = []
				here = countCloserSingleMappedTags(line, singleMappers, window)
				tag2position.append([line, here])
			else:
				### append line to list while same tag comes in with counting closer mapped single mapped tags
				here = countCloserSingleMappedTags(line, singleMappers, window)
				tag2position.append([line, here])
		else:
			### initialization and value set for variables/list with counting closer mapped single mapped tags
			mapPositions = float(columns[mapPositionIndex])
			tagSequence = columns[tagIndex]
			tag2position = []
			here = countCloserSingleMappedTags(line, singleMappers, window)
			tag2position.append([line, here])
	### last output
	printResult(mapPositions, o, tag2position, disposeNoNearbyFlag)
	### finalize filehandle
	f.close()

################################
def printResult(mapPositions, o, tag2position, disposeNoNearbyFlag):
	### select singlemap/multimap procedure
	if mapPositions == 1:
		line, here = tag2position[0]
		weight = 1.0
		o.write("\t".join([line, str(weight)])+"\n")
	else:
		### rescue calculation
		total = countWholeSingleMappedTags(tag2position)
		for currentLine in tag2position:
			line, here = currentLine
			if total == 0:
				if disposeNoNearbyFlag == 1:
					weight = 0
				else:
					weight = 1 / float(mapPositions)
			else:
				weight = here / total
			o.write("\t".join([line, str(weight)])+"\n")

################################
def countWholeSingleMappedTags(tag2position):
	total = 0
	for currentLine in tag2position:
		line, here = currentLine
		total += here
	return total

################################
def countCloserSingleMappedTags(currentLine, singleMappers, window):
	columns = currentLine.split("\t")
	here = 0
	chromosome, strand, position = retPosition(columns)
	chromosome_strand = "_".join([chromosome, strand])
	if chromosome_strand in singleMappers:
		for k in range(position-window/2, position+window/2): 
			if k in singleMappers[chromosome_strand]:
				here += singleMappers[chromosome_strand][k]
	return here

################################
# initialization
### main::init::strings
copyright = "MuMRescueLite Copyright HASHIMOTO, Takehiro/Michiel deHoon/Geoffrey Faulkner 2008-2009"
usage = """Usage:
  MuMRescueLite.py < input file> <output file> <window>
"""
description = """What Does This Script Do?
  Sequence tags that map to multiple genomic loci (multi-mapping tags or MuMs), are routinely omitted from further analysis, leading to experimental bias and reduced coverage. MultiMapRescueLite probabilistically reincorporates multi-mapping tags into mapped short read data with acceptable computational requirements. Please check the reference articles for more details.
"""
warning_args = "Incorrect number of arguments"
help = ["-h", "-help", "--help"]
### main::init::check_arguments
if len(sys.argv) == 1:
	print copyright
	print usage
	sys.exit(0)
elif sys.argv[1] in help:
	print usage
	print description
	sys.exit(0)
elif len(sys.argv) != 4:
	print warning_args
	sys.exit(1)
else:
	### args treatment
	inFile = sys.argv[1]
	outFile = sys.argv[2]
	try:
		window = int(sys.argv[3])
	except ValueError:
		print "window should be an odd integer"
		sys.exit(1)
	try:
		f = open(inFile)
	except IOError:
		print "Failed to find input file " + inFile
		sys.exit(1)
	f.close()

################################
# main
### flags for some option 
inputHeaderFlag = 1	# default 1 to skip the header line of input file
outputHeaderFlag = 1  	# default 1 to write output header
disposeNoNearbyFlag = 1 # set this value to 1 makes no rescue for multi-mappers of no nearby single mappers; default 1 for publication version, 0 for RIKEN internal version, 
### regist single mapped tags to memory
singleMappers = {}
remainSingleMapper(inFile, singleMappers, inputHeaderFlag)
### header output
o = open(outFile, "w") 
if outputHeaderFlag:
	o.write("\t".join(["tagID", "mapPositions", "chromosome", "strand", "start", "stop", "count", "weight"]) + "\n")
### perform multi map rescue and output
multiMapRescue(inFile, singleMappers, window, o, inputHeaderFlag, disposeNoNearbyFlag)
o.close()
### finalization
sys.exit(0)
