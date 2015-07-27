package QCMG::Bioscope::Ini::GenerateFiles;

##############################################################################
#
#  Program:  QCMG::Bioscope::Ini::GenerateFiles.pm
#  Creator:  John V Pearson
#  Created:  2011-04-07
#
#  This module is the entry point for the Bioscope Ini File generation
#  system (QCMG::Bioscope::Ini::*)
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );

use DBI;

use QCMG::Bioscope::Ini::Fragment;
use QCMG::Bioscope::Ini::MatePair;
use QCMG::Bioscope::Ini::PairedEndBC;
use QCMG::DB::TrackLite;
use QCMG::FileDir::SeqRawParser;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#                             PUBLIC METHODS                              #
###########################################################################

sub new {
    my $class = shift;
    my %params = @_;

    # List of valid user-settable parameters.  These may all appear in
    # the constructor.
    my @fields = qw( execution_server ini_root_dir run_type run_name
                     run_date bioscope_install_dir pbs_queue email 
                     pbs_exe_queue panasas_mount barcode physical_division
                     config run_id rawdir
			primary_lib_id
                     f3_primary_dir f3_read_length f3_csfasta f3_qual f3_primary
                     r3_primary_dir r3_read_length r3_csfasta r3_qual r3_primary
                     f5_primary_dir f5_read_length f5_csfasta f5_qual f5_primary );

    # Table of equivalencies between Tracklite => Bioscope
    my %equivs = ('run_name'                   => 'run_name',
                  'barcode'                    => 'barcode',
                  'individual_sample_location' => 'physical_division',
                  'f3_fullpath'                => 'f3_primary_dir',
                  'f3_reads'                   => 'f3_primary',
                  'f3_tag_length'              => 'f3_read_length',
                  'f3_csfasta'                 => 'f3_csfasta',
                  'r3_fullpath'                => 'r3_primary_dir',
                  'r3_reads'                   => 'r3_primary',
                  'r3_tag_length'              => 'r3_read_length',
                  'r3_csfasta'                 => 'r3_csfasta',
                  'f5_fullpath'                => 'f5_primary_dir',
                  'f5_reads'                   => 'f5_primary',
                  'f5_tag_length'              => 'f5_read_length',
                  'f5_csfasta'                 => 'f5_csfasta',
		  'primary_lib_id'	       => 'primary_lib_id'
		);
 
    my $self = { params  => {},
                 fields  => \@fields,
                 equivs  => \%equivs,
                 verbose => ($params{verbose} ? $params{verbose} : 0 ) };
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

    # [2] Set parameters based on config file
    # If config file is found then read and set the params.
    if (defined $params{config} and $params{config}) {
       $self->parse_config_file( $params{config} );
    }

    # [3] Set based on run_id
    # If a run_id is found then read values from TrackLite.
    if (defined $params{run_id} and $params{run_id}) {
        $self->parse_tracklite_values( $params{run_id} );
    }

    # [4] Parse a seq_raw directory looking for primaries, barcodes etc
    if (defined $params{rawdir} and $params{rawdir}) {
        $self->parse_raw_dir( $params{rawdir} );
    }

    # [5] Set defaults for some fields if still unset
    $self->set_defaults();

    return $self;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub run_type {
    my $self = shift;
    return undef unless defined $self->{params}->{run_type};
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


sub parse_config_file {
    my $self     = shift;
    my $filename = shift;

    my $fh = IO::File->new( $filename, 'r' );
    die 'Unable to open config file [', $filename, "] for reading: $!"
        unless defined $fh;

    # Read all the options
    my %options = ();
    while (my $line = $fh->getline) {
        chomp $line;
        $line =~ s/\s+$//;        # trim trailing spaces
        next if ($line =~ /^#/);  # skip comments
        next unless $line;        # skip blank lines

        if ($line =~ /^([\w_\d]+)\s*=\s*(.*)$/) {
            $options{ $1 } = $2;
        }
        else {
            die "Unparseable line in config file: [$line]";
        }
    }

    # Constructor options should override the config file options but
    # since the constructor options have been processed prior to reading
    # the config file, we are forced to make sure that a value has not
    # previously been set before we can set it here.

    foreach my $field ($self->fields) {
        next unless exists $options{$field};  # skip if not set in config
        $self->set_param( $field, $options{$field} );
    }

    if ($self->verbose > 1) {
        print "Parameters after reading from config file:\n";
        $self->_dump_params;
    }
}

sub _dump_params {
    my $self = shift;
    foreach my $key (sort keys %{$self->{params}}) {
        printf "    %-40s %s\n", $key, $self->{params}->{$key};
    }
}


# Pull apart the seq_raw run directory
sub parse_raw_dir {
    my $self = shift;
    my $dir  = shift;

    my $raw = QCMG::FileDir::SeqRawParser->new();
    $raw->process( $dir );
    print $raw->report;

    if ($self->verbose > 1) {
        print "Parameters after parsing the seq_raw directory:\n";
        $self->_dump_params;
    }
}


# Prod-n-poke tracklite values into %PARAMS
sub parse_tracklite_values {
    my $self   = shift;
    my $run_id = shift;

    sub cs2q {
        my $qual = shift;
        return undef unless $qual;
        $qual =~ s/\.csfasta/_QV.qual/;
        return $qual;
    }

    my $db = QCMG::DB::TrackLite->new;
    $db->connect;

    my $rh_fields = $db->by_run_sample_number( $run_id ); 

    # Flatten the hash of hashes into a simple hash
    my %dbvals = ();
    foreach my $key (sort keys %{$rh_fields}) {
        $dbvals{ $key } = $rh_fields->{$key}->{value};
    }

    # Diagnostic dump of values retrieved from Tracklite
    if ($self->verbose > 2) {
        print "Values read from Tracklite:\n";
        foreach my $key (sort keys %dbvals) {
            printf "    %-40s", $key;
            printf "%s\n", (defined $dbvals{$key} ? $dbvals{$key} : 'undef');
        }
    }

    # Here is where we need to use our table of equivalencies - for each
    # Bioscope parameter, see if the tracklite equivalent was set
    foreach my $field (keys %{ $self->{equivs} }) {
        next unless defined $dbvals{$field};  # skip if not set
        $self->set_param( $self->{equivs}->{$field}, $dbvals{$field} );
    }

    # More complex cases - N.B. the F5 check has to come before R3 since
    # PE barcoded runs have the barcode primary dir in the R3 slot
    if (defined $dbvals{f3_csfasta}) {
       $self->set_param( 'f3_qual', cs2q( $dbvals{f3_csfasta} ) );
       if (defined $dbvals{f5_csfasta}) {
           $self->set_param( 'run_type', 'pairedendBC' );
           $self->set_param( 'f5_qual', cs2q( $dbvals{f5_csfasta} ) );
       }
       elsif (defined $dbvals{r3_csfasta}) {
           $self->set_param( 'run_type', 'matepair' );
           $self->set_param( 'r3_qual', cs2q( $dbvals{r3_csfasta} ) );
       }
       else {
           $self->set_param( 'run_type', 'fragment' );
       }
    }

    # Guess the run_type if it's not already set
    if (! $self->{params}->{run_type}) {

        # Now pick the run_type, coping with barcode/no-barcode
        if ($self->{params}->{barcode} ne 'nobc') {
            $self->{params}->{run_type} =
                   $dbvals{run_type} eq 'Frag' ? 'fragmentBC' :
                   $dbvals{run_type} eq 'PE'   ? 'pairedendBC' : '';
        }
        else {
            $self->{params}->{run_type} =
                   $dbvals{run_type} eq 'Frag' ? 'fragment' :
                   $dbvals{run_type} eq 'LMP'  ? 'matepair' :
                   $dbvals{run_type} eq 'PE'   ? 'pairedend' : '';
        }
    }

    if ($self->verbose > 1) {
        print "Parameters after reading from tracklite LIMS:\n";
        $self->_dump_params;
    }
}


sub set_defaults {
    my $self = shift;

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

    # INI root dir
    if (! $self->{params}->{ini_root_dir}) {
        $self->{params}->{ini_root_dir} = '.';
    }

    # barcode - nobc if unset
    if (! $self->{params}->{barcode}) {
        $self->{params}->{barcode} = 'nobc';
    }

    # physical_division - nopd if unset
    if (! $self->{params}->{physical_division}) {
        $self->{params}->{physical_division} = 'nopd';
    }

    # primary_lib_id
    if (! $self->{params}->{primary_lib_id}) {
       	$self->{params}->{primary_lib_id} = 'unknown';
    }

	#print STDERR "Setting library: ".$self->{params}->{primary_lib_id}."\n";

    # User better have set $QCMG_EMAIL or this could go poorly
    if (! $self->{params}->{email}) {
        $self->{params}->{email} = $ENV{QCMG_EMAIL};
    }

    # Guess any missing F3 params
    if ($self->{params}->{f3_primary}) {
        # Primary directory
        if (! $self->{params}->{f3_primary_dir} ) {
            $self->{params}->{f3_primary_dir} =
               _guess_primary_dir( $self->{params}->{barcode},
                                   $self->{params}->{f3_primary} );
        }
        # CSFASTA file
        if (! $self->{params}->{f3_csfasta} ) {
            $self->{params}->{f3_csfasta} =
               _guess_csfasta( $self->{params}->{barcode},
                               $self->{params}->{run_name}, 'F3' );
        }
        # QUAL file
        if (! $self->{params}->{f3_qual} ) {
            $self->{params}->{f3_qual} =
               _guess_qual( $self->{params}->{barcode},
                            $self->{params}->{run_name}, 'F3' );
        }
    }

    # Guess any missing R3 params
    if ($self->{params}->{r3_primary}) {
        # Primary directory
        if (! $self->{params}->{r3_primary_dir} ) {
            $self->{params}->{r3_primary_dir} =
               _guess_primary_dir( $self->{params}->{barcode},
                                   $self->{params}->{r3_primary} );
        }
        # CSFASTA file
        if (! $self->{params}->{r3_csfasta} ) {
            $self->{params}->{r3_csfasta} =
               _guess_csfasta( $self->{params}->{barcode},
                               $self->{params}->{run_name}, 'R3' );
        }
        # QUAL file
        if (! $self->{params}->{r3_qual} ) {
            $self->{params}->{r3_qual} =
               _guess_qual( $self->{params}->{barcode},
                            $self->{params}->{run_name}, 'R3' );
        }
    }

    # Guess any missing F5 params
    if ($self->{params}->{f5_primary}) {
        # pairedend and pairedendBC have different F5 tag names
        my $tag = 'F5-P2';
        $tag = 'F5-BC' if ($self->run_type =~ /pairedendBC/);

        # Primary directory
        if (! $self->{params}->{f5_primary_dir} ) {
            $self->{params}->{f5_primary_dir} =
               _guess_primary_dir( $self->{params}->{barcode},
                                   $self->{params}->{f5_primary} );
        }
        # CSFASTA file
        if (! $self->{params}->{f5_csfasta} ) {
            $self->{params}->{f5_csfasta} =
               _guess_csfasta( $self->{params}->{barcode},
                               $self->{params}->{run_name}, $tag );
        }
        # QUAL file
        if (! $self->{params}->{f5_qual} ) {
            $self->{params}->{f5_qual} =
               _guess_qual( $self->{params}->{barcode},
                            $self->{params}->{run_name}, $tag );
        }
    }


    # This should always be the last block in this method
    if ($self->verbose > 1) {
        print "Parameters after setting defaults:\n";
        $self->_dump_params;
    }
}


sub _guess_primary_dir {
    my $barcode     = shift;
    my $primary     = shift;

    # Barcodes look different!
    if ($barcode ne 'nobc') {
        return "bcSample1/results.F1B1/libraries/$barcode/$primary/reads";
    }
    else {
        #return "Sample1/results.F1B1/libraries/$primary/reads";
        return "Sample1/results.F1B1/$primary/reads";
    }
}


sub _guess_csfasta {
    my $barcode     = shift;
    my $run_name    = shift;
    my $tag         = shift;

    # Barcodes look different!
    if ($barcode ne 'nobc') {
        return $run_name . '_bcSample1_' . $tag . '_' . $barcode . '.csfasta';
    }
    else {
        return $run_name . '_Sample1_' . $tag . '.csfasta';
    }
}


sub _guess_qual {
    my $barcode     = shift;
    my $run_name    = shift;
    my $tag         = shift;

    # Barcodes look different!
    if ($barcode ne 'nobc') {
        return $run_name . '_bcSample1_' . $tag . '_QV_' . $barcode . '.qual';
    }
    else {
        return $run_name . '_Sample1_' . $tag . '_QV.qual';
    }
}


sub generate {
    my $self = shift;

	print STDERR "LIBRARY: (lib) ".$self->{params}->{'primary_lib_id'}."\n";

    # Do it!
    my $ini;
    if ($self->run_type =~ /fragment/i) {
        $ini = QCMG::Bioscope::Ini::Fragment->new( %{ $self->{params} },
                                                   verbose => $self->verbose );
    }
    elsif ($self->run_type =~ /matepair/i) {
        $ini = QCMG::Bioscope::Ini::MatePair->new( %{ $self->{params} },
                                                   verbose => $self->verbose );
    }
    elsif ($self->run_type =~ /pairedendBC/i) {
        $ini = QCMG::Bioscope::Ini::PairedEndBC->new( %{ $self->{params} },
                                                      verbose => $self->verbose );
    }
    else {
        die 'The run_type [',$self->run_type,"] is not currently handled.\n";
    }
}




__END__

=head1 NAME

QCMG::Bioscope::Ini::GenerateFiles - generate Bioscope INI files


=head1 SYNOPSIS

 use QCMG::Bioscope::Ini::GenerateFiles;
 my $ini = QCMG::Bioscope::Ini::GenerateFiles->new( %params );
 $ini->generate;


=head1 DESCRIPTION

This module is the top-level interface for the QCMG system for
generating Bioscope INI files.  The Bioscope INI files contain hundreds
of name=value pairs but only a handful are user settable.  There are 5
ways in which values can be determined for the user-settable Bioscope
parameters:

 1. In the constructor for this class
 2. From a config file
 3. From the QCMG LIMS (currently Tracklite)
 4. By parsing a seq_raw directory
 5. Default

If a parameter is set in multiple places then values form steps with
lower numbers take precedence so if a parameter was set in the
constructor method then tha value would take precedence over any values
for the same parameter in the config file or LIMS.

In practical terms, step 4 will only work well as the primary
information source for runs without barcode, lanes or gaskets.  This is
because all of the multiplexed run types will include multiple mapsets
in a single directory so without at least a run_id, the parser for step
4 will be unable to work out which of the multiplexed mapsets the INI
files are required for.

=head1 PUBLIC METHODS

=over

=item B<new()>

 execution_server
 ini_root_dir
 run_type
 run_name
 run_date
 bioscope_install_dir
 pbs_queue 
 pbs_exe_queue
 panasas_mount

 config
 run_id

If this is specified then the F3/R3/F5 parameters that would normally
be set in the config file are instead retrieved directly from Tracklite.
Note that other options such as B<-s>, B<-i>, B<-a>, and B<-e> will
still need to be set from the commandline as they relate to aspects of
mapping that are not captured in Tracklite.

=head2 Constructor values

=item config

A text config file that contains user-settable options.  These options
may incude some that are also settable from the command line but because
there are so many bioscope options, most are only handled via the config
file.  If a parameter is set in the config file and also by a
commandline parameter, the commandline parameter value will be the one
that is used.  This allows the user to have a config file but override
select values by supplying a commandline parameter.

=item run_type

Currently valid run_type values are fragment, matepair and pairedendBC.

=item run_name

Run name from icgc_runs log, typically looks like:
I<S0014_20090108_1_MC58>.

=item execution_server

Currently valid execution_server values are barrine, qcmg-clustermk2.
Defaults to barrine since barrine is the primary target for all mapping
jobs.

=item run_date

Should be an 8 digit string of the form YYYYMMDD, for example
I<20110217>.  Defaults to today's date.

=item verbose

An integer indicating the level of detail of progress and diagnostic
messages.  Higher numbers give more detailed output.

=head2 Config File Example

This example is for an LMP run on barrine:

 execution_server = barrine

 run_type       = matepair
 ini_root_dir   = /panfs/imb/home/uqqxu1/devel/QCMGProduction/bioscope
 run_name       = S0039_20100802_2_LMP
 run_date       = 20110311

 f3_read_length = 50
 f3_csfasta     = S0039_20100802_2_LMP_Sample1_F3.csfasta
 f3_qual        = S0039_20100802_2_LMP_Sample1_F3_QV.qual
 f3_primary_dir = Sample1/results.F1B1/primary.20100814200322206/reads

 r3_read_length = 50
 r3_csfasta     = S0039_20100802_2_LMP_Sample1_R3.csfasta
 r3_qual        = S0039_20100802_2_LMP_Sample1_R3_QV.qual
 r3_primary_dir = Sample1/results.F1B1/primary.20100807071130290/reads

Setting B<execution_server> to barrine or qcmg-clustermk2 will also
set sensible default values for parameters B<panasas_mount>, 
B<pbs_queue>, B<pbs_exe_queue> B<bioscope_install_dir>.  You should only
set these secondary values in cases where you wish to override the defaults.


=head1 SEE ALSO

=over 2

=item perl

=item QCMG::Bioscope::Ini::IniFile
=item QCMG::Bioscope::Ini::Parameter
=item QCMG::Bioscope::Ini::ParameterCollection

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


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
