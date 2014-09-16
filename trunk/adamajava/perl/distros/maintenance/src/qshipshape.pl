#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qshipshape.pl
#  Author:   Matthew J Anderson
#  Created:  2012-05-22
#
#  This application is for maintating harmony in seq_results.
#  I.e. renaming and relocating mapsets.
#
#  $Id: qshipshape.pl 4668 2014-07-24 10:18:42Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Pod::Usage;
use Getopt::Long;
use Data::Dumper;
#use Carp qw( carp croak confess );

use QCMG::Util::QLog;
use QCMG::SeqResults::qshipshape_checks;
#use QCMG::SeqResults::qshipshape_actions;

our ( $SVNID, $REVISION, $LOG );
our ( $DEFAULT_LOG_DIR, $DEFAULT_PROJECT_DIR ); 

( $REVISION ) = '$Revision: 4668 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qshipshape.pl 4668 2014-07-24 10:18:42Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;
	
( $DEFAULT_LOG_DIR ) 		= "/mnt/seq_results/changelogs/qshipshape";
( $DEFAULT_PROJECT_DIR ) 	= "/mnt/seq_results";


MAIN: {
    # Print usage message if no arguments supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => "COMMANDS"   ) unless (scalar @ARGV > 0);
			   

    #my $default_dir   = $ENV{'QCMG_PANFS'} . '/seq_results';
    #my $default_dir   = '/mnt/seq_results';
    #my $default_email = $ENV{'QCMG_EMAIL'};
	
	
    my $cmdline = join(' ',@ARGV);
    my $command = lc( shift @ARGV );
	
	command_help("man") unless $command;
    if ( $command =~ /version|help|man/ ) {
        command_help($command);
	}
    
    if ( $command =~ /auto|check|move|rename|replace|delete|extract|copy|quarantine/ ) {
		command_help($command) if (scalar @ARGV <= 1); 
		
		my $requirements = QCMG::SeqResults::qshipshape_checks->command_requirements_check($command);
		command_help($command) if ! $requirements;
        
		my $logging = 0;
		if ( $requirements->{opts}{logfile} ) {
	        my $logfile = "$DEFAULT_LOG_DIR/".$requirements->{opts}{logfile};
	        #print "Log File: $logfile\n";
			#qlogfile( $logfile, append => 1);
			#$logging = 1;
			#qlogbegin();
		}
		
		if ($command =~ /auto/) {
			command_auto($requirements);
		}	
		elsif ($command =~ /check/) {
			command_check($requirements);
		}
		elsif ($command =~ /move/) {
		 	command_move($requirements);
		}
		elsif ($command =~ /rename/) {
			command_rename($requirements);
		}
		elsif ($command =~ /replace/) {
			command_replace($requirements);
		}
		elsif ($command =~ /delete/) {
			command_delete($requirements);
		}
		elsif ($command =~ /extract/) {
			command_extract($requirements);
		}
		elsif ($command =~ /copy/) {
			command_copy($requirements);
		}
		elsif ($command =~ /quarantine/) {
			command_quarantine($requirements);
		}
		
		qlogend() if $logging;
			
		
		
	}else{
		command_help($command);
	}
	
	exit 1;
}


sub command_help {
    my $command = shift;
	
    if ($command eq "auto") {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => "AUTO USAGE" );
    }
    if ($command eq "check") {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => "CHECK USAGE" );
    }
    elsif ($command eq "move") {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => "MOVE USAGE" );
    }
    elsif ($command eq "rename") {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => "RENAME USAGE" );
        
    }
    elsif ($command eq "replace") {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => "REPLACE USAGE" );
        
    }
    elsif ($command eq "delete") {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => "DELETE USAGE" );
    
    }
    elsif ($command eq "extract") {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => "EXTRACT USAGE" );
        
    }
    elsif ($command eq "version") {
        print "$SVNID\n";
        
    }
    elsif ($command eq "help") {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => "SYNOPSIS|COMMANDS" );
                   
    }
    elsif ($command eq "man") {
        pod2usage(-exitstatus => 0, -verbose => 2);
    
    }
    else {
        warn "command $command is unrecognised\n\n";
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => "SYNOPSIS|COMMANDS" );
    }
    
	exit;
}

sub command_auto {
	my $requirements	= shift;
	my $options 		= $requirements->{opts};
	my $flags			= $requirements->{flags};
	
	print Dumper $requirements;
}

sub command_check {
	my $requirements	= shift;
	my $options 		= $requirements->{opts};
	my $flags			= $requirements->{flags};
	
	my $metadata = QCMG::SeqResults::qshipshape_checks->mapset_metadata($options->{original_mapset});
	my $metadata_donor_directory = QCMG::SeqResults::qshipshape_checks->metadata_donor_directory (
		$DEFAULT_PROJECT_DIR, \%$metadata 
	);
	
	if ( $options->{donor_directory} ne $metadata_donor_directory ){
		print "[ WARNING ] - Mapset $options->{original_mapset} donor directory $options->{donor_directory} does not match LIMS $metadata_donor_directory\n";
		exit 0;
	}
	print "Mapset $options->{original_mapset} appears to be in the correct donor directory $metadata_donor_directory \n";
	exit 1;
}

sub command_move {
	my $requirements	= shift;
	my $options 		= $requirements->{opts};
	my $flags			= $requirements->{flags};
	
	print Dumper $requirements;
}

sub command_rename {
	my $requirements	= shift;
	my $options 		= $requirements->{opts};
	my $flags			= $requirements->{flags};
	
	print Dumper $requirements;
}

sub command_replace {
	my $requirements	= shift;
	my $options 		= $requirements->{opts};
	my $flags			= $requirements->{flags};
	
	print Dumper $requirements;
}

sub command_extract {
	my $requirements	= shift;
	my $options 		= $requirements->{opts};
	my $flags			= $requirements->{flags};
	
	print Dumper $requirements;
}

sub command_delete {
	my $requirements	= shift;
	my $options 		= $requirements->{opts};
	my $flags			= $requirements->{flags};
	
	print Dumper $requirements;
}

sub command_copy {
	my $requirements	= shift;
	my $options 		= $requirements->{opts};
	my $flags			= $requirements->{flags};
	
	print Dumper $requirements;
}

sub command_quarantine  {
	my $requirements	= shift;
	my $options 		= $requirements->{opts};
	my $flags			= $requirements->{flags};
	
	print Dumper $requirements;
}



__END__

=head1 NAME

qshipshape.pl - Perl script for processing seq_results directory


=head1 SYNOPSIS

 qshipshape.pl <command> [options]


=head1 ABSTRACT

This script has multiple commands, each of which is an operation against a
QCMG-style seq_results directory.  Each command has its own distinct set of
commandline options.
This man page is only being maintained to list
available commands and all other documentation including examples and
detailed descriptions of the options has been moved to the QCMG wiki at 
http://qcmg-wiki.imb.uq.edu.au/index.php/qshipshape


=head1 COMMANDS

 auto           checks the mapsets conforms to the naming convesion, and matches in Geneus. It will then check if the mapset is the correct location. If its not it will attempt to put it in the correct location.
 check          checks the mapset is in the correctly named and in correct location.
 move           move a mapset from one donor directory to an other, with the option to rename the mapset
 rename         rename a mapset
 replace        replace a mapset with a mapset from LiveArc or another directory, with the option to rename the mapset
 extract        extract a mapset from LiveArc
 delete         delete a mapset
 help           print usage message showing available commands
 man            print full man page
 version        print version number.


=head1 AUTO USAGE

=head2 EXAMPLES

  qshipshape.pl auto -log <dir> -mapped <path> 

  qshipshape.pl auto -log <dir> -donor <dir> -mapset <name>

=head2 OPTIONS

 -mapped        Path of mapped mapset from aligner
 -donor         Current donor directory
 -mapset        Original mapset name 
 -log           Log directory
 -v             Print progress and diagnostic messages
 -help          Display help


=head1 CHECK USAGE

=head2 EXAMPLES

   qshipshape.pl check -log <dir> -donor <dir> -mapset <name>

=head2 OPTIONS
  
  -donor         Current donor directory
  -mapset        Original mapset name
  -log           Log directory
  -v             Print progress and diagnostic messages
  -help          Display help


=head1 MOVE USAGE

=head2 EXAMPLES

  qshipshape.pl move -log <dir> -donor <dir> -mapset <name> -new <dir>
  
  qshipshape.pl move -log <dir> -donor <dir> -mapset <name> -new <dir> -rename <name>

=head2 OPTIONS

 -donor         Current donor directory
 -mapset        Original mapset name
 -new           New donor directory
 -rename        New mapset name
 -log           Log directory
 -v             Print progress and diagnostic messages
 -help          Display help


=head1 RENAME USAGE

=head2 EXAMPLES

 qshipshape.pl rename -log <dir> -donor <dir> -mapset <name> -rename <name>
 
=head2 OPTIONS

 -donor          Current donor directory
 -mapset         Original mapset name
 -rename         New mapset name
 -log            Log directory
 -v              Print progress and diagnostic messages
 -help           Display help


=head1 REPLACE USAGE

=head2 EXAMPLES

 qshipshape.pl replace -log <dir> -donor <dir> -mapset <name> -namespace <LiveArc> -asset <name> -rename <new name>

 qshipshape.pl replace -log <dir> -donor <dir> -mapset <name> -existingDir <dir> -existingMapset <name>

 qshipshape.pl replace -log <dir> -donor <dir> -mapset <name> -existingDir <dir> -existingMapset -rename <new name>

=head2 OPTIONS

 -donor              Current donor directory
 -mapset             Original mapset name
 -namespace          LiveArc namespace
 -asset              LiveArc asset name
 -existingDir        new_donor_directory
 -existingMapset     
 -rename             New mapset name
 -log                Log directory
 -v                  Print progress and diagnostic messages
 -help               Display help


=head1 EXTRACT USAGE

=head2 EXAMPLES

 qshipshape.pl extract -log <dir> -donor <dir> -namespace <LiveArc> -asset <name> -rename <new name>

=head2 OPTIONS

 -donor             Current donor directory
 -namespace         LiveArc namespace
 -asset             LiveArc asset name
 -rename            New mapset name
 -log               Log directory
 -v                 Print progress and diagnostic messages
 -help              Display help


=head1 DELETE USAGE

=head2 EXAMPLES

 qshipshape.pl delete  -log <dir> -donor <dir> -mapset <name>

=head2 OPTIONS

 -donor             Current donor directory
 -mapset            Original mapset name
 -log               Log directory
 -v                 Print progress and diagnostic messages
 -help              Display help



=head1 AUTHOR

=over 2

=item Matthew Anderson, L<mailto:m.anderson@imb.uq.edu.au>

=back


=head1 VERSION

$Id: qshipshape.pl 4668 2014-07-24 10:18:42Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012

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
