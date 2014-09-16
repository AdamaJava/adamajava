#!/usr/bin/perl -w

##############################################################################
#
#  Program:  generate_bioscope_ini_files.pl
#  Author:   John V Pearson
#  Created:  2010-08-23
#
#  This script collects various inputs from the user and calls the
#  relevant modules from the QCMG::Bioscope::Ini::* group to create the
#  .ini files.  There are a large number of potential items that are
#  settable by the user and these are all collected in the
#  QCMG::Bioscope::Ini::Ini superclass.
#
#  $Id: generate_bioscope_ini_files.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );
use Term::ReadKey;

use QCMG::Bioscope::Ini::GenerateFiles;
use QCMG::DB::TrackLite;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE %PARAMS );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: generate_bioscope_ini_files.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Do *NOT* set any defaults here.  The logic for config files is to
    # let commandline options take preference so if you set a default
    # here it will be impossible to override in a config file.  For
    # thhis script, we'll use a separate defaulting routine later.

    $PARAMS{run_type}             = '';
    $PARAMS{execution_server}     = '';
    $PARAMS{ini_root_dir}         = '';
    $PARAMS{run_name}             = '';
    $PARAMS{run_date}             = '';
    $PARAMS{email}                = '';
    $PARAMS{bioscope_install_dir} = '';
    $PARAMS{pbs_queue}            = '';
    $PARAMS{pbs_exe_queue}        = '';
    $PARAMS{panasas_mount}        = '';
    $PARAMS{barcode}              = '';
    $PARAMS{physical_division}    = '';
    $PARAMS{f3_primary_dir}       = '';
    $PARAMS{f3_read_length}       = '';
    $PARAMS{f3_csfasta}           = '';
    $PARAMS{f3_qual}              = '';
    $PARAMS{f3_primary}           = '';
    $PARAMS{r3_primary_dir}       = '';
    $PARAMS{r3_read_length}       = '';
    $PARAMS{r3_csfasta}           = '';
    $PARAMS{r3_qual}              = '';
    $PARAMS{r3_primary}           = '';
    $PARAMS{f5_primary_dir}       = '';
    $PARAMS{f5_read_length}       = '';
    $PARAMS{f5_csfasta}           = '';
    $PARAMS{f5_qual}              = '';
    $PARAMS{f5_primary}           = '';
    $PARAMS{primary_lib_id}       = '';


    my @run_ids              = ();
    my $config               = '';
    my $rawdir               = '';
    my $slide                = '';
       $VERBOSE              = 0;
       $VERSION              = 0;
    my $help                 = 0;
    my $man                  = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'y|slide=s'                => \$slide,
           'z|run_id=s'               => \@run_ids,
           'c|config=s'               => \$config,
           'd|rawdir=s'               => \$rawdir,
           'r|run_name=s'             => \$PARAMS{run_name},
           't|run_type=s'             => \$PARAMS{run_type},
           's|execution_server=s'     => \$PARAMS{execution_server},
           'i|ini_root_dir=s'         => \$PARAMS{ini_root_dir},
           'a|run_date=s'             => \$PARAMS{run_date},
           'e|email=s'                => \$PARAMS{email},
           'q|pbs_queue=s'            => \$PARAMS{pbs_queue},
           'x|pbs_exe_queue=s'        => \$PARAMS{pbs_exe_queue},
           'b|bioscope_install_dir=s' => \$PARAMS{bioscope_install_dir},
           'p|panasas_mount=s'        => \$PARAMS{panasas_mount},
             'barcode=s'              => \$PARAMS{barcode},
             'physical_division=s'    => \$PARAMS{physical_division},
             'f3_primary_dir=s'       => \$PARAMS{f3_primary_dir},
             'f3_read_length=i'       => \$PARAMS{f3_read_length},
             'f3_csfasta=s'           => \$PARAMS{f3_csfasta},
             'f3_qual=s'              => \$PARAMS{f3_qual},
             'f3_primary=s'           => \$PARAMS{f3_primary},
             'r3_primary_dir=s'       => \$PARAMS{r3_primary_dir},
             'r3_read_length=i'       => \$PARAMS{r3_read_length},
             'r3_csfasta=s'           => \$PARAMS{r3_csfasta},
             'r3_qual=s'              => \$PARAMS{r3_qual},
             'r3_primary=s'           => \$PARAMS{r3_primary},
             'f5_primary_dir=s'       => \$PARAMS{f5_primary_dir},
             'f5_read_length=i'       => \$PARAMS{f5_read_length},
             'f5_csfasta=s'           => \$PARAMS{f5_csfasta},
             'f5_qual=s'              => \$PARAMS{f5_qual},
             'f5_primary=s'           => \$PARAMS{f5_primary},
             'primary_lib_id=s'       => \$PARAMS{primary_lib_id},
           'v|verbose+'               => \$VERBOSE,               # -v
           'version!'                 => \$VERSION,               # --version
           'h|help|?'                 => \$help,                  # -?
           'man|m'                    => \$man                    # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    # --slide is a special case that triggers a LIMS lookup to try to
    # identify all of the run_ids that belong to the slide
    if ($slide) {
        my $lims = QCMG::DB::TrackLite->new();
        $lims->connect;
        my $ra_rows = $lims->mapsets_from_slide_name( $slide );
        foreach my $mapset (@{ $ra_rows }) {
            push @run_ids, $mapset->[0]; 
        }
    }

    print( "\ngenerate_bioscope_ini_files.pl  v$REVISION  [" .
           localtime() . "]\n",
           "   config               $config\n",
           '   run_id(s)            ', join(',',@run_ids), "\n",
           "   rawdir               $rawdir\n",
           "   run_name             $PARAMS{run_name}\n",
           "   execution_server     $PARAMS{execution_server}\n",
           "   run_type             $PARAMS{run_type}\n",
           "   ini_root_dir         $PARAMS{ini_root_dir}\n",
           "   run_date             $PARAMS{run_date}\n",
           "   email                $PARAMS{email}\n",
           "   pbs_queue            $PARAMS{pbs_queue}\n",
           "   bioscope_install_dir $PARAMS{bioscope_install_dir}\n",
           "   panasas_mount        $PARAMS{panasas_mount}\n",
           "   barcode              $PARAMS{barcode}\n",
           "   physical_division    $PARAMS{physical_division}\n",
           "   f3_primary_dir       $PARAMS{f3_primary_dir}\n",
           "   f3_read_length       $PARAMS{f3_read_length}\n",
           "   f3_csfasta           $PARAMS{f3_csfasta}\n",
           "   f3_qual              $PARAMS{f3_qual}\n",
           "   f3_primary           $PARAMS{f3_primary}\n",
           "   r3_primary_dir       $PARAMS{r3_primary_dir}\n",
           "   r3_read_length       $PARAMS{r3_read_length}\n",
           "   r3_csfasta           $PARAMS{r3_csfasta}\n",
           "   r3_qual              $PARAMS{r3_qual}\n",
           "   r3_primary           $PARAMS{r3_primary}\n",
           "   f5_primary_dir       $PARAMS{f5_primary_dir}\n",
           "   f5_read_length       $PARAMS{f5_read_length}\n",
           "   f5_csfasta           $PARAMS{f5_csfasta}\n",
           "   f5_qual              $PARAMS{f5_qual}\n",
           "   f5_primary           $PARAMS{f5_primary}\n",
           "   primary_lib_id       $PARAMS{primary_lib_id}\n",
           "   verbose              $VERBOSE\n\n" ) if $VERBOSE;

    # If there are no run_ids, we still want to run it
    if (scalar(@run_ids) > 0) {
        foreach my $run_id (@run_ids) {
            my $ini = QCMG::Bioscope::Ini::GenerateFiles->new(
                            run_id  => $run_id,
                            config  => $config,
                            rawdir  => $rawdir,
                            %PARAMS,
                            verbose => $VERBOSE );
            $ini->generate;
        }
    }
    else {
        my $ini = QCMG::Bioscope::Ini::GenerateFiles->new(
                        config  => $config,
                        rawdir  => $rawdir,
                        %PARAMS,
                        verbose => $VERBOSE );
        $ini->generate;
    }
}


__END__

=head1 NAME

generate_bioscope_ini_files.pl - Generate ini files for a Bioscope run


=head1 SYNOPSIS

 generate_bioscope_ini_files.pl [options]


=head1 ABSTRACT

This script will generate a directory and all the files required to
initiate a Bioscope run.  This man page is only being maintained to list
available options and all other documentation including examples and
detailed descriptions of the options has been moved to the QCMG wiki at

 http://qcmg-wiki.imb.uq.edu.au/index.php/Generate_bioscope_ini_files.pl


=head1 OPTIONS

 -y | --slide                   slide name used to lookup run_id(s) from LIMS
 -z | --run_id                  run ID for Tracklite data retrieval
 -c | --config                  config file
 -d | --rawdir                  seq_raw dirctory to be parsed
 -r | --run_name                run name
 -t | --run_type                run type (fragment, matepair etc)
 -s | --execution_server        server the INI files will be run on
 -e | --email                   used by PBS to send error and output
 -i | --ini_root_dir            directory where ini file dir will be created
 -a | --run_date                run date in YYYYMMDD format
 -q | --pbs_queue               PBS queue
 -b | --bioscope_install_dir    absolute path to $BIOSCOPEROOT
 -p | --panasas_mount           Panasas filesystem mount point
      --barcode                 barcode
      --physical_division       physical division (quad, octet, lane etc)
      --f3_primary_dir          full path to dir that contains the csfasta
      --f3_read_length          tag length - 50, 35, 60 etc
      --f3_csfasta              .csfasta file name
      --f3_qual                 .QV_qual file name
      --f3_primary              primary dir chosen by SeqTeam
      --r3_primary_dir          full path to dir that contains the csfasta
      --r3_read_length          tag length - 50, 35, 60 etc
      --r3_csfasta              .csfasta file name
      --r3_qual                 .QV_qual file name
      --r3_primary              primary dir chosen by SeqTeam
      --f5_primary_dir          full path to dir that contains the csfasta
      --f5_read_length          tag length - 50, 35, 60 etc
      --f5_csfasta              .csfasta file name
      --f5_qual                 .QV_qual file name
      --f5_primary              primary dir chosen by SeqTeam
      --primary_lib_id          primary library ID
 -v | --verbose                 print diagnostic messages
      --version                 print version number
 -? | --help                    display help
 -m | --man                     display man page


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

$Id: generate_bioscope_ini_files.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010,2011

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
