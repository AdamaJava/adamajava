package QCMG::Annotate::Gff;

###########################################################################
#
#  Module:   QCMG::Annotate::Gff.pm
#  Creator:  Lynn Fink
#  Created:  2013-02-26
#
# Logic for annotating a file with features in a GFF
#
#  $Id: Gff.pm 4660 2014-07-23 12:18:43Z j.pearson $
#
###########################################################################

use strict;
#use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::IO::GffReader;
use QCMG::IO::DccSnpReader;
use QCMG::IO::DccSnpWriter;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 3242 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Gff.pm 4660 2014-07-23 12:18:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class    = shift;

    my %defaults = @_;
    $defaults{fields}	= 'feature';	# only report 3rd column ("feature") of GFF file

    # Print usage message if no arguments supplied
    #pod2usage( -exitval  => 0,
    #           -verbose  => 99,
    #           -sections => 'DESCRIPTION/Commandline parameters' )
    #    unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile   => '',
                   dcc1out  => '',
                   logfile  => '',
                   gfffile  => '',
		   fields   => $defaults{fields},
		   stranded => '',
                   verbose  => $defaults{verbose}
		);

	# allow this module to be called from within another module
	my $options = {};
	for(my $i=0; $i<=$#_; $i+=2) {
		my $this_sub = (caller(0))[0]."::".(caller(0))[3];
		$options->{$_[$i]} = $_[($i + 1)];
	}
	$params{infile}		= $options->{'infile'}	 if($options->{'infile'});
	$params{dcc1out}	= $options->{'dcc1out'}  if($options->{'dcc1out'});
	$params{logfile}	= $options->{'logfile'}  if($options->{'logfile'});
	$params{gfffile}	= $options->{'gfffile'}  if($options->{'gfffile'});
	$params{fields}		= $options->{'fields'}	 if($options->{'fields'});
	$params{stranded}	= $options->{'stranded'} if($options->{'stranded'});
	$params{verbose}	= $options->{'verbose'}	 if($options->{'verbose'});

	# if command line options are passed, override %params with them
    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'i|infile=s'         => \$params{infile},            # -i
           'o|dcc1out=s'        => \$params{dcc1out},           # -o
           'l|logfile=s'        => \$params{logfile},           # -l
	   'g|gff=s'		=> \$params{gfffile},		# -g
	   'f|fields=s'		=> \$params{fields},		# -f
	   's|stranded+'	=> \$params{stranded},		# -s
           'v|verbose+'         => \$params{verbose},           # -v
           );

    die "-i|--infile, -o|--dcc1out and -g|--gff are required\n"
       unless ($params{infile} and $params{dcc1out} and $params{gfffile});

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n" );
    qlogprint( "Run parameters:\n" );
    foreach my $key (sort keys %params) {
        qlogprint( "  $key : ", $params{$key}, "\n" );
    }


    # Create the object
    my $self = { %params };
    bless $self, $class;
}

sub execute {
    my $self = shift;

	# read GFF file and get features in a hash, key = chr; value = array of features
	my $features	= $self->get_features();

    # Because there are so many types of DCC1 file, we are going to let
    # the DccSnpReader guess the version rather than trying to specify it
    my $dcc1 = QCMG::IO::DccSnpReader->new(
                   filename => $self->{infile},
                   verbose  => $self->{verbose} );

    # get array of desired fields to put in flag in specified order
    my $fields		= $self->get_gff_field_list();
    
    # Die if the file is not a DCC1 variant or if we can't determine 
    # somatic/germline from file type
    die "input file must be a DCC1 variant but detected file type is [",
        $dcc1->version, "]\n" unless ($dcc1->dcc_type eq 'dcc1');

    # create output file in same DCC1 format
    my $dcc1out	= QCMG::IO::DccSnpWriter->new(
                   	filename	=> $self->{dcc1out},
			version		=> $dcc1->version,
                   	verbose		=> $self->{verbose}
			);

    my $count_annotated	= 0;
    my $count_total	= 0;
    while(my $rec	= $dcc1->next_record) {
		#print Dumper $rec;

		# BAD! DO PROPER CHROMOSOME CONVERSION!
		$rec->{'chromosome'}	=~ s/chr//;

		# keep copy of original flag
		my $orig_flag		= $rec->{'QCMGflag'};

		my $chrfeatures	= $features->{$rec->{'chromosome'}};

		#print STDERR "Checking record $rec->{'chromosome'} : $rec->{'chromosome_start'} - $rec->{'chromosome_end'}...\n";

		foreach my $f (@{$chrfeatures}) {
			#print STDERR "\tagainst ".$f->{'start'}." - ".$f->{'end'}."\n";
			if(
				$rec->{'chromosome_start'}	>= $f->{'start'} && $rec->{'chromosome_start'}	<= $f->{'end'} || 
				$rec->{'chromosome_end'}	>= $f->{'start'} && $rec->{'chromosome_end'}	<= $f->{'end'}    ) {


				# skip record if strand is important and strand
				# of GFF feature does not match strand of DCC1
				# record
				next if($self->{stranded} && $f->{'strand'} ne $rec->{'strand'});

				# if record is a match, add the requested GFF file fields
				# to the QCMGflag field of the DCC1 record
				# 
				# multiple fields are delimited by a ; and the
				# attribs field key/value pairs are delimited by
				# :: (the key and value are delimited by = )
				# e.g., feature,attribs -> LINE/L1;ID=3032453::Note=L1PA16::;
				my $flag;
				foreach (@${fields}) {
					if($_ eq 'attribs') {
						foreach my $att (keys %{$f->{'attribs'}}) {
							$flag	.= $att."=".$f->{'attribs'}->{$att}."::";
						}
						$flag	.= ";";
					}
					else {
						$flag	.= $f->{$_}.";";
					}
				}

				$rec->{'QCMGflag'}	.= ";".$flag;	# concatenate
				#$rec->{'QCMGflag'}	= $flag;

			}

			# don't bother parsing features that are out of range
			if($rec->{'chromosome_end'} < $f->{'start'}) {
				last;
			}
		}

		# count annotated records
		if($rec->{'QCMGflag'} ne $orig_flag) {
			$count_annotated++;
		}

		$count_total++;

		# write DCC1 record, with any edits, to output file
		$dcc1out->write_record($rec);
    }

    $dcc1out->close;

    qlogprint("Read\t\t$count_total\tDCC1 records\n");
    qlogprint("Annotated\t$count_annotated\tDCC1 records\n");

    qlogend();
}

sub get_gff_field_list {
    my $self = shift;

    # get array of desired fields to put in QCMGflag in specified order
    my @fields		= ();
    if($self->{fields}	=~ /,/) {
    	@fields		= split /,/, $self->{fields};
    }
    else {
	@fields		= $self->{fields};
    }

   return(\@fields);

}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub get_features {
    my $self = shift;

	# read all exons from the appropriate gene model GTF into a hash ref; key =
	# transcript ID; values = attributes
	qlogprint("Reading $self->{'gfffile'}\n");

	my $f		= QCMG::IO::GffReader->new(filename => $self->{'gfffile'});
	my $count	= 0;
	my $features;

	while (my $rec = $f->next_record) {
		# BAD! DO PROPER CHROMOSOME CONVERSION!
		$rec->{'seqname'}	=~ s/chr//;

		# this needs to be unique because it is the key for each feature
		my $loc_id	= join ":",	$rec->{'seqname'},
						$rec->{'start'},
						$rec->{'end'},
						$rec->{'strand'},
						$rec->{'feature'};


		my $feature;
		$feature->{'seqname'}	= $rec->{'seqname'};
		$feature->{'start'}	= $rec->{'start'};
		$feature->{'end'}	= $rec->{'end'};
		$feature->{'strand'}	= $rec->{'strand'};
		$feature->{'source'}	= $rec->{'source'};
		$feature->{'score'}	= $rec->{'score'};
		$feature->{'frame'}	= $rec->{'frame'};
		$feature->{'matepair'}	= $rec->{'matepair'};
		$feature->{'feature'}	= $rec->{'feature'};
		$feature->{'attribs'}	= $rec->{'attribs'};

		push @{$features->{$rec->{'seqname'}}}, $feature;

		$count++;
	}

	qlogprint("read $count records\n");

	return($features);
}

1;

__END__

=head1 NAME

QCMG::Annotate::Gff - Annotate qSNP DCC1 output files with features in a GFF file


=head1 SYNOPSIS

 use QCMG::Annotate::Gff;

my %defaults = ( verbose  => 0 );
 my $qsnp = QCMG::Annotate::Gff->new( %defaults );
 $qsnp->execute;


=head1 DESCRIPTION

This module provides functionality for annotating qSNP DCC1 files.  It
includes commandline parameter processing.

=head2 Commandline parameters

 -i | --infile        Input file in DCC1 format
 -o | --dcc1out       Output file in DCC1 format
 -g | --gff           GFF file with features of interest
 -f | --fields	      Fields from GFF to report, comma-separated
 -s | --stranded      Only report GFF features if the strand matches the record
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages
      --version       print version number and exit immediately
 -h | -? | --help     print help message


=over

=item B<-i | --infile>

Mandatory. The input file is in DCC1 format (see QCMG wiki) and is a 
direct output of the qSNP java program.

=item B<-o | --dcc1out>

Mandatory. The DCC1 format output file (see QCMG wiki).

=item B<-g | --gff>

Mandatory. The input GFF feature file (see QCMG wiki).

=item B<-f | --fields>

Optional. A list of GFF fields to report for each matching record. These are
comma-separated and order matters.

Default = feature

Possible values include:
seqname
source
feature
start
end
score
strand
frame
matepair
attribs 

 Example: -f feature,seqname,start,end

=item B<-s | --stranded>

Optional. Pnly GFF features that occur on the same strand as the input
record are annotated. Strand representations must be identical - no translation
between different representation occurs.

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back


=head1 AUTHORS

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=item Lynn Fink, L<mailto:l.fink@imb.uq.edu.au>

=back


=head1 VERSION

$Id: Gff.pm 4660 2014-07-23 12:18:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

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
