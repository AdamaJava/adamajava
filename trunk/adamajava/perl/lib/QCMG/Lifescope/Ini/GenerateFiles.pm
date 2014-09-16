package QCMG::Lifescope::Ini::GenerateFiles; 
##############################################################################
#
#  Program:  QCMG::Lifescope::Ini::GenerateFiles.pm
#  Creator:  Lynn Fink (copying John V Pearson's modules)
#  Created:  2011-12-08
#
#  This module is the entry point for the Lifescope Ini File generation
#  system (QCMG::Lifescope::Ini::*)
#
#  $Id: GenerateFiles.pm 1406 2011-12-05 23:28:03Z l.fink $
#
##############################################################################

=pod

=head1 NAME

QCMG::Lifescope::Ini::GenerateFiles -- module for generate Lifescope .ls files

=head1 SYNOPSIS

Most common use:
	my $ls	= QCMG::Lifescope::Ini::GenerateFiles->new(
			ini_root_dir	=> $ini_root_dir,
			run_type	=> $run_type,
			slide_name	=> $slide_name,
			xsqfile		=> $xsq,
			barcode		=> $barcode
			email 		=> $email
		);

=head1 DESCRIPTION

Contains methods for parsing command line arguments describing an XSQ file and
creates the directory structure and corresponding .ls file which directs
Lifescope in mapping the readsets in the file

=head1 REQUIREMENTS
 IO::File
 Data::Dumper
 Pod::Usage
 Carp

=cut

use strict;

use IO::File;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 1406 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: GenerateFiles.pm 1406 2011-12-05 23:28:03Z l.fink $' =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#	 PUBLIC METHODS							  #
###########################################################################
=pod

=head1 METHODS

B<new()> (constructor)

 Create a new instance of this class and set user-specified parameters

 All information must be presented on the command line - the LIMS is not
  queried 

Parameters:
 	ini_root_dir
	run_type
	xsqfile
	barcode
	species (homo sapiens, human, mus musculus, mouse)
 	run_date
	lifescope_workflow_dir
	lifescope_bin_dir
	software_dir
	execution_server
	email 

Returns:
 a new instance of this class.

=cut
sub new {
	my $class = shift;

	my %params = @_;

	# List of valid user-settable parameters.  These may all appear in
	# the constructor.
	my @fields = qw(
		 	ini_root_dir
			run_type
			slide_name
			xsqfile
			barcode
		 	run_date
			lifescope_workflow_dir
			lifescope_bin_dir
			software_dir
			execution_server
			email 
			species
		);

	# translate QCMG run type tags into Lifescope run type tags
	my %runtypeequivs	= (
			'Frag'	=> 'frag',
			'LMP'	=> 'lmp',
			'PEBC'	=> 'pe',
			'PE'	=> 'pe',
		);

	# translate run type tags into Lifescope secondary mapping types
	my %mappingtypeequivs	= (
			'Frag'	=> 'fragment',
			'LMP'	=> 'pair',
			'PEBC'	=> 'pair',
			'PE'	=> 'pair',
			'frag'	=> 'fragment',
			'lmp'	=> 'pair',
			'pe'	=> 'pair'
		);

	my $self = {
			params  => {},
		 	fields  => \@fields,
			verbose => ($params{verbose} ? $params{verbose} : 0 ) 
		};

	bless $self, $class;

	# [1] Set parameters based on constructor values
	# If any of the defined params were supplied in the constructor then
	# set the value, otherwise set empty so they are defined but false.
	foreach my $field ($self->fields) {
		if (defined $params{$field}) {
			$self->{params}->{$field} = $params{$field};
		}
		else {
			$self->{params}->{$field} = '';
		}
	}

	if ($self->verbose > 1) {
		print "Parameters after reading from constructor:\n";
		$self->_dump_params;
	}

	# [5] Set defaults for some fields if still unset
	$self->set_defaults();

	# set equivalents
	$self->{runtypeequivs}		= \%runtypeequivs;
	$self->{mappingtypeequivs}	= \%mappingtypeequivs;

	return $self;
}

sub verbose {
	my $self = shift;
	return $self->{verbose};
}

################################################################################

=pod

B<run_type()>

 Set run type using Lifescope-friendly type; also set mapping type for secondary
  analysis based on run type

Parameters:
 none

Requires:
 $self->{params}

Returns:
 scalar - run type

=cut
sub run_type {
	my $self = shift;

	return undef unless defined $self->{params}->{run_type};

	$self->{params}->{run_type}	= $self->{'runtypeequivs'}->{$self->{params}->{run_type}};
	$self->{params}->{mapping_type}	= $self->{'mappingtypeequivs'}->{$self->{params}->{run_type}};

	return $self->{params}->{run_type};
}

sub fields {
	my $self = shift;
	return @{ $self->{fields} };
}

sub set_param {
	my $self  = shift;
	my $param = shift;
	my $value = shift;

	# Only set param if it's not already set
	if (! $self->{params}->{$param}) {
		$self->{params}->{$param} = $value;
	}
}

sub _dump_params {
	my $self = shift;
	foreach my $key (sort keys %{$self->{params}}) {
		printf "	%-40s %s\n", $key, $self->{params}->{$key};
	}
}

sub set_defaults {
	my $self = shift;

	# default to no barcodes (empty string)
	if (! $self->{params}->{barcode}) {
		$self->{params}->{barcode}	= 'nobc';
	}

	# associate species with reference genome
	$self->{refgenome}->{'homo sapiens'}	= 'qcmg_hg19';
	$self->{refgenome}->{'human'}		= 'qcmg_hg19';
	$self->{refgenome}->{'mus musculus'}	= 'qcmg_mm64';
	$self->{refgenome}->{'mouse'}		= 'qcmg_mm64';

	# extract project name from .xsq filename
	# S8006_20110815_1_LMP.lane_01.nobc.xsq ->
	# S8006_20110815_1_LMP.lane_01.nobc
	$self->{params}->{project_name}	= $self->{params}->{xsqfile};
	$self->{params}->{project_name}	=~ s/^(.+)\.xsq/$1/;
	my $bc	= $self->{params}->{barcode};
	$self->{params}->{project_name}	=~ s/nobc/$bc/;

	# Date - today's date if unset
	if (! $self->{params}->{run_date}) {
		my @fields = localtime;
		my $year  = $fields[5] + 1900;
		my $month = sprintf("%02d", $fields[4] + 1);
		my $day   = sprintf("%02d", $fields[3]);
		$self->{params}->{run_date} = $year . $month . $day;
	}
	
	# Execution server - barrine if unset
	if (! $self->{params}->{execution_server}) {
		$self->{params}->{execution_server} = 'barrine';
	}

	# PBS queue - workq if unset
	if (! $self->{params}->{pbs_queue}) {
		$self->{params}->{pbs_queue} = 'workq';
	}

	# default to human species; otherwise, lower-case the species and assign
	# the appropriate genome
	if(! $self->{params}->{species}) {
		$self->{params}->{species}	= 'homo sapiens';
		$self->{params}->{refgenome}	= $self->{refgenome}->{ $self->{params}->{species} };
	}
	else {
		$self->{params}->{species}	=~ y/A-Z/a-z/;
		$self->{params}->{refgenome}	= $self->{refgenome}->{ $self->{params}->{species} };
	}

	# path to Lifescope default workflows
	if (! $self->{params}->{lifescope_workflow_dir}) {
		#$self->{params}->{lifescope_workflow_dir} = '/panfs/imb/qcmg/software/LifeScope_2.5/lifescope/etc/workflows/';
		$self->{params}->{lifescope_workflow_dir} = '/share/software/Lifescope_2.5.1/lifescope/etc/workflows/';
	}

	# path to Lifescope 
	if (! $self->{params}->{lifescope_bin_dir}) {
		#$self->{params}->{lifescope_bin_dir} = '/panfs/imb/qcmg/software/LifeScope_2.5/lifescope/bin/';
		$self->{params}->{lifescope_bin_dir} = '/share/software/Lifescope_2.5.1/lifescope/bin/';
	}

	# path to our software
	if (! $self->{params}->{software_dir}) {
		#$self->{params}->{software_dir} = '/panfs/imb/qcmg/software/QCMGPerl/distros/automation/';
		$self->{params}->{software_dir} = '/share/software/QCMGPerl/distros/automation/';
	}

	# INI root dir
	if (! $self->{params}->{ini_root_dir}) {
		$self->{params}->{ini_root_dir} = '.';
	}

	# User better have set $QCMG_EMAIL or this could go poorly
	if (! $self->{params}->{email}) {
		$self->{params}->{email} = $ENV{QCMG_EMAIL};
	}

	# This should always be the last block in this method
	if ($self->verbose > 1) {
		print "Parameters after setting defaults:\n";
		$self->_dump_params;
	}
}

################################################################################

=pod

B<template_ls()>

 Generic .ls file template which should be able to support our usual run types;
  we change a few variables depending on the XSQ file and the run type

Parameters:
 none

Requires:
 none
Returns:
 scalar - template string

=cut
sub template_ls {
		my $self = shift;

	my $template	= qq{cd /projects
mk <project_name> 
cd <project_name>
mk <run_date>
cd <run_date>
set workflow <lifescope_workflow_dir>/genomic.mapping.<run_type>/analysis.pln
add xsq /panfs/imb/seq_raw/<slide_name>/<xsqfile>
set reference <refgenome> 
#optional - clean-up 
set task.temp.dir.delete true
set analysis.clean.temp.files true
#optional - generate unmapped read files -> can add to global ini file
set create.unmapped.bam.files true secondary/<mapping_type>.mapping.ini
set refcor.base.filter.qv 5
#optional queue setting 
#set queue.name <pbs_queue> 
set queue.name batch
set mapping.np.per.node 4
set mapping.memory 44gb
# set run status in LiveArc
#!<software_dir>lifescope_status.pl -u `whoami` -p <project_name> -a <run_date> -m init
run
#wait
# get exit status from summary.log and update LiveArc
#!<software_dir>lifescope_status.pl -u `whoami` -p <project_name> -a <run_date> -m set
exit
};

	return($template);
}

################################################################################

=pod

B<template_ls_barcode()>

 Generic .ls file template which should be able to support our usual run types;
  we change a few variables depending on the XSQ file and the run type
  - support multiple barcodes

Parameters:
 none

Requires:
 none
Returns:
 scalar - template string

=cut
sub template_ls_barcode {
		my $self = shift;

	my $template	= qq{cd /projects
mk <project_name> 
cd <project_name>
mk <run_date>
cd <run_date>
set workflow <lifescope_workflow_dir>/genomic.mapping.<run_type>/analysis.pln
add xsq /panfs/imb/seq_raw/<slide_name>/<xsqfile>:<barcodeidx>
set reference <refgenome> 
#optional - clean-up 
set task.temp.dir.delete true
set analysis.clean.temp.files true
#optional - generate unmapped read files -> can add to global ini file
set create.unmapped.bam.files true secondary/<mapping_type>.mapping.ini
set refcor.base.filter.qv 5
#optional queue setting 
#set queue.name <pbs_queue> 
set queue.name batch
set mapping.np.per.node 4
set mapping.memory 44gb
# set run status in LiveArc
#!<software_dir>lifescope_status.pl -u `whoami` -p <project_name> -a <run_date> -m init
run
#wait
# get exit status from summary.log and update LiveArc
#!<software_dir>lifescope_status.pl -u `whoami` -p <project_name> -a <run_date> -m set
exit
};

	return($template);
}

################################################################################
=pod

B<template_runpbs()>

 Generic run.pbs file template which runs Lifescope using the .ls file;
  we change a few variables depending on the XSQ file 

Parameters:
 none

Requires:
 none

Returns:
 scalar - template string

=cut
sub template_runpbs {
	my $self = shift;

	# set:
	# <pbs_queue>
	# <slide_name>
	# <run_date>
	# <lifescope_bin_dir>

	my $template	= <<'RUNPBS';
#!/bin/bash
#PBS -N ls_run_pbs
#PBS -q <pbs_queue>
#PBS -A sf-QCMG
#PBS -S /bin/bash
#PBS -r n
#PBS -j oe
#PBS -l walltime=124:00:00
#PBS -l select=1:ncpus=1:mem=4gb
#PBS -m ae
#PBS -M QCMG-InfoTeam@imb.uq.edu.au

ssh b10b10 "nohup <lifescope_bin_dir>/lscope.sh -u qcmg -e ~/lscope.passwd < /panfs/imb/seq_raw/<slide_name>/<ls_file> &"

RUNPBS

	return($template);
}

################################################################################
=pod

B<template_ls_command()>

 Generic command to input .ls file to lifescope server

Parameters:
 none

Requires:
 none

Returns:
 scalar - template string

=cut
sub template_ls_command {
	my $self = shift;

	my $template	= qq{ssh b10b10 "nohup <lifescope_bin_dir>/lscope.sh -u qcmg -e ~/lscope.passwd < /panfs/imb/seq_raw/<slide_name>/<ls_file> &" \n\n};

	return($template);
}

################################################################################

=pod

B<check_params()>

 Make that necessary params are set or die

Parameters:
 none

Requires:
 $self->{params}

Returns:
 void

=cut
sub check_params {
	my $self	= shift @_;

	if(	! $self->{params}->{run_date} ||
		! $self->{params}->{run_type} ||
		! $self->{params}->{mapping_type} ||
		! $self->{params}->{pbs_queue} ||
		! $self->{params}->{xsqfile} ||
		! $self->{params}->{slide_name} ||
		! $self->{params}->{project_name} ||
		! $self->{params}->{lifescope_bin_dir} ||
		! $self->{params}->{lifescope_workflow_dir} ||
		! $self->{params}->{species} ||
		! $self->{params}->{refgenome}
	) {

		print STDERR "Missing parameter(s):\n";
		$self->_dump_params();
		exit(1);
	}
}

################################################################################
=pod

B<generate_ls()>

 Read the generic template .ls string and replace the variables with information
  specific to the XSQ file; create a string with the text of the file to create

Parameters:
 none

Requires:
 $self->{params}

Returns:
 ref to array of ls filenames

=cut
sub generate_ls {
	my $self	= shift @_;

	my $params	= $self->{'params'};

	my $run_type	= $self->run_type();

	my $ls;

	my @files	= ();

	$self->check_params();

	$self->{params}->{xsqfile}	=~ /(lane\_\d)/;
	my $lane	= $1;

	if($self->{params}->{'barcode'} ne 'nobc') {
		# if barcode, just get the non-zero-padded number
		# (1,2,...16) and prepend with a colon
		# add xsq file.xsq:1
		# barcode    = 'bcB16_01'
		#                      -
		# barcodeidx = '1'
		/(\d+)$/;
		my $bc		= $1;
		$bc		=~ s/^0//;
	
		$self->{params}->{'barcodeidx'}	= $bc;

		# .ls filename (pathless)
		my $fname	= $self->ls_filename();
		$self->{params}->{ls_file} 	= $fname;

		$ls	= $self->template_ls_barcode();
		foreach (keys %{$params}) {
			$ls	=~ s/<$_>/$params->{$_}/g;
		}

		$self->write_file($fname, $ls);
	}
	else {
		$ls	= $self->template_ls();
		foreach (keys %{$params}) {
			$ls	=~ s/<$_>/$params->{$_}/g;
		}

		# generate and set filename for .ls file -> needed for run.pbs
		$self->{params}->{ls_file} 	= $self->ls_filename();
		$self->write_file($self->{params}->{ls_file}, $ls);
	}

	return($self->{params}->{ls_file})
}

################################################################################
=pod

B<generate_runpbs()>

 Read the generic template run.pbs string and replace the variables with information
  specific to the XSQ file; create a string with the text of the file to create

Parameters:
 none

Requires:
 $self->{params}

Returns:
 scalar - text of run.pbs file

=cut
sub generate_runpbs {
	my $self	= shift @_;

	my $params	= $self->{'params'};

	$self->check_params();

	my $runpbs	= $self->template_runpbs();
	foreach (keys %{$params}) {
		$runpbs	=~ s/<$_>/$params->{$_}/g;
	}

	my $fname = $self->runpbs_filename();
	$self->write_file($fname, $runpbs);

	return($fname)
}

################################################################################
=pod

B<ls_filename()>

 Generate pathless filename for .ls file

Parameters:

Requires:
 $self->{params}->{project_name}

Returns:
 scalar - name .ls file (no path)

=cut
sub ls_filename {
	my $self	= shift @_;

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

=cut

	my $params	= $self->{'params'};

	# build pathless .ls filename
	$self->{params}->{'ls_file'}	= $self->{params}->{xsqfile};
	$self->{params}->{'ls_file'}	=~ s/\.xsq//;
	$self->{params}->{'ls_file'}	.= ".ls";

	# add barcode, if one exists
	if($params->{'barcode'} =~ /\w+/ && $params->{'barcode'} ne 'nobc') {
		$self->{params}->{'ls_file'}	=~ s/nobc/$params->{'barcode'}/;
	}

	return($self->{params}->{'ls_file'})
=cut

=cut

	my $ls_file	= join ".", $options->{'SLIDE'}, $options->{'LANE'}, $options->{'BARCODE'}, "ls";

	return($ls_file);
=cut
	my $ls_file	= join ".", $self->{params}->{project_name}, "ls";
	return($ls_file);
}

################################################################################
=pod

B<runpbs_filename()>

 Generate pathless filename for run.pbs file

Parameters:

Requires:
 $self->{params}->{project_name}

Returns:
 scalar - name run.pbs file (no path)

=cut
sub runpbs_filename {
	my $self	= shift @_;
=cut

	my $params	= $self->{'params'};

	# build pathless run.pbs filename
	$self->{params}->{'runpbs_file'}	= $self->{params}->{xsqfile};
	$self->{params}->{'runpbs_file'}	=~ s/\.xsq//;
	#$self->{params}->{'runpbs_file'}	= $self->{params}->{'ls_file'};
	#$self->{params}->{'runpbs_file'}	=~ s/\.ls//;
	$self->{params}->{'runpbs_file'}	.= ".run.pbs";

	return($self->{params}->{'runpbs_file'})
=cut

=cut
        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $rp_file	= join ".", $options->{'SLIDE'}, $options->{'LANE'}, $options->{'BARCODE'}, "run.pbs";

	return($rp_file);
=cut
	my $rp_file	= join ".", $self->{params}->{project_name}, "run.pbs";
	return($rp_file);
}

################################################################################
=pod

B<write_file()>

 Writes the .ls file in the proper directory; will overwrite an existing file

Parameters:
 filename
 file conents

Requires:
 $self->{params}

Returns:
 void

=cut
sub write_file {
	my $self	= shift @_;
	my $filename	= shift @_;
	my $fc		= shift @_;

	$filename	= $self->{params}->{'ini_root_dir'}."/".$filename;

	# Now we are ready to write out the file
	my $fh = IO::File->new( $filename, 'w' );
	die 'Unable to open file [', $filename, "] for writing: $!" unless defined $fh;
	$fh->print( $fc );
	$fh->close;

	return();
}

1;

__END__

=head1 AUTHOR

=over 2

=item Lynn Fink/John Pearson, L<mailto:l.fink@imb.uq.edu.au>

=back


=head1 VERSION

$Id: GenerateFiles.pm 1406 2011-12-05 23:28:03Z l.fink $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
