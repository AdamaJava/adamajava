#! /python22/Lib

import string,sys,os,math,commands,re,random,cmd, time

def rescue(input, output, window, chromo):

	chromosomes = {}
	currentChromosomes = {}

	passKey = {}
	f = open(input)
	thisline = f.readline()
	while (thisline):
		data = string.split(thisline)
		ID = data[0]
		chromosome = data[2]
		if (chromosome == chromo):
			passKey[ID] = "Y"
		thisline = f.readline()
	f.close()

	f = open(input)
	thisline = f.readline()
	while (thisline):
		data = string.split(thisline)
		ID = data[0]
		try:
			passKey[ID]
			chromosome = data[2]
			start = data[3]
			strand = data[5]
			currentChromosomes[chromosome+strand+start] = "Y"
		except:
			pass
		thisline = f.readline()
	f.close()
	
	already = {}
	key = {}
	IDs = []
	f = open(input)
	thisline = f.readline()
	while (thisline):
		data = string.split(thisline)
		call = "N"
		chromosome = data[2]
		start = int(data[3])
		strand = data[5]
		for k in range(start-window/2,start+window/2):
			try:
				currentChromosomes[chromosome+strand+str(k)]
				call = "Y"
			except:
				pass
		if (call == "Y"):
			ID = data[0]
			try:
				key[ID].append(data[2]+"_"+data[3]+"_"+data[4]+"_"+data[5])
			except:
				key[ID] = [data[2]+"_"+data[3]+"_"+data[4]+"_"+data[5]]
				IDs.append(ID)
			try:
				already[data[2]+"_"+data[3]+"_"+data[4]+"_"+data[5]].append(ID)
			except:
				already[data[2]+"_"+data[3]+"_"+data[4]+"_"+data[5]] = [ID]
		thisline = f.readline()
	f.close()

	for i in range(len(IDs)):
		key[IDs[i]].sort()

	conversion = {}
	overall = {}
	for i in range(len(IDs)):
		try:
			overall[IDs[i]]
		except:
			another = {}
			others = []
			positions = key[IDs[i]]
			for j in range(len(positions)):
				terms = already[positions[j]]
				for k in range(len(terms)):
					try:
						another[terms[k]]
					except:
						another[terms[k]] = "Y"
						others.append(terms[k])
			for j in range(len(others)):
				if (key[others[j]] == key[IDs[i]]):
					overall[others[j]] = "Y"
					try:
						conversion[IDs[i]].append(others[j])
					except:
						conversion[IDs[i]] = [others[j]]

	overall = {}
	key = {}
	already = {}

	IDs = []
	info = {}
	f = open(input)
	thisline = f.readline()
	while (thisline):
		data = string.split(thisline)
		ID = data[0]
		chromosome = data[2]
		start = int(data[3])
		strand = data[5]
		move = "N"
		try:
			conversion[ID]
			move = "Y"
		except:
			pass
		if (move == "Y"):
			try:
				chromosomes[chromosome+strand+str(start)].append(ID)
			except:
				chromosomes[chromosome+strand+str(start)] = [ID]
			try:
				info[ID].append(string.strip(thisline))
			except:
				info[ID] = [string.strip(thisline)]
				IDs.append(ID)
		thisline = f.readline()
	f.close()

	o = open(output, "w") 
	for i in range(len(IDs)):
		outTerms = conversion[IDs[i]]
		ID = IDs[i]
		positions = info[ID]
		if (len(positions) == 1):
			thisline = positions[0]
			data = string.split(thisline, "\t")
			data[-1] = string.strip(data[-1])
			if (data[-1] == "SiM"):
				for z in range(len(outTerms)):
					outString = outTerms[z]
					for r in range(1,len(data)):
						outString = outString + "\t" + data[r]
					if (data[2] == chromo):
						o.write(outString+"\t1.0\t"+str(float(data[1]))+"\n")
		else:
			totalUnique = 0
			combinations = {}
			secondCombinations = {}
			otherIDs = []
			terms = []
			otherIDs = []
			for j in range(len(positions)):
				thisline = positions[j]
				data = string.split(thisline)
				start = int(data[3])
				for k in range(start-window/2,start+window/2):
					terms = []
					try:
						terms = chromosomes[data[2]+data[5]+str(k)]
					except:
						pass
					for z in range(len(terms)):
						try:
							combinations[terms[z]] = combinations[terms[z]] + 1
						except:
							combinations[terms[z]] = 1
							otherIDs.append(terms[z])
			for k in range(len(otherIDs)):
				count = combinations[otherIDs[k]]
				if (count == 1):
					totalUnique = totalUnique + len(conversion[otherIDs[k]])

			if (totalUnique > 0):
				total = 0
				for j in range(len(positions)):
					thisline = positions[j]
					data = string.split(thisline, "\t")
					data[-1] = string.strip(data[-1])
					start = int(data[3])
					currentUnique = 0
					for k in range(start-window/2,start+window/2):
						terms = []
						try:
							terms = chromosomes[data[2]+data[5]+str(k)]
						except:
							pass	
						for z in range(len(terms)):
							count = combinations[terms[z]]
							if (count == 1):
								currentUnique = currentUnique + len(conversion[terms[z]])

					coefficient = currentUnique/float(totalUnique)
					for z in range(len(outTerms)):
						outString = outTerms[z]
						for r in range(1,len(data)):
							outString = outString + "\t" + data[r]
						if (data[2] == chromo):
							o.write(outString+"\t"+str(float(coefficient))+"\t"+str(coefficient*float(data[1]))+"\n")
			else:
				coefficient = 0
				for j in range(len(positions)):
					thisline = positions[j]
					data = string.split(thisline, "\t")
					data[-1] = string.strip(data[-1])
					for z in range(len(outTerms)):
						outString = outTerms[z]
						for r in range(1,len(data)):
							outString = outString + "\t" + data[r]
						if (data[2] == chromo):
							o.write(outString+"\t"+str(float(coefficient))+"\t"+str(coefficient*float(string.split(thisline)[1]))+"\n")
	o.close()

rescue(sys.argv[1], sys.argv[2], int(sys.argv[3]), sys.argv[4])
sys.exit()

