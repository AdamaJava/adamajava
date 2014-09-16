package QCMG::Run::Qbasepileup;

###########################################################################
#
#  Module:   QCMG::Run::Qbasepileup
#  Creator:  John V Pearson
#  Created:  2013-01-10
#
#  Runs java executable from the Adama qbasepileup utility.
#
#  $Id: Qbasepileup.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use IO::File;

use QCMG::Util::QLog;
use QCMG::Util::Util qw( qcmg_default );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Qbasepileup.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    my $self = { verbose => ($params{verbose} ? $params{verbose} : 0), };
    bless $self, $class;

    # Set root for qbasepileup install
    $self->qbp_root( $ENV{'ADAMA_HOME'} ?
                     $ENV{'ADAMA_HOME'} :
                     '/share/software/adama-nightly' ),

    # Setup various binaries and check that they are executable
    $self->_setup_binaries;

    return $self;
}


sub _setup_binaries {
    my $self = shift;

    my %bins = ( qbasepileup_bin => 'qbasepileup' );

    foreach my $key (sort keys %bins) {
        my $bin = $bins{$key};
        $self->{$key} = $self->qbp_root .'bin/'. $bin;
        die "qbasepileup $bin binary not executable: ", $self->{$key} ,"\n"
            unless (-e $self->{$key});
    }
}


sub qbp_root {
    my $self = shift;
    if (@_) {
        $self->{qbp_root} = shift;
        # Check that qbp_root has a trailing '/'
        $self->{qbp_root} .= '/' unless ($self->{qbp_root} =~ /\/$/);
        # We will need to reset the paths to all binaries
        $self->_setup_binaries;
    }
    return $self->{qbp_root};
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub qbasepileup {
    my $self   = shift;
    my %params = @_;

    #qlogprint( "inside qbasepileup $_\n" ) foreach (1..100);

    # Check for mandatory inputs
    die "qbasepileup() - you must specify the infile to be converted\n" unless $params{infile};
    die "qbasepileup - the infile must be specified using an absolute pathname\n"
        unless ($params{infile} =~ /^\//);
    die "qbasepileup() - currently we can only run against .maf infiles\n"
        unless ($params{infile} =~ /\.maf$/i);

    # Assuming this is a MAF file, get some defaults together
    my $names = QCMG::Verify::AutoNames->new( verbose => $self->verbose );
    my $rh_names = $names->params_from_maf( $params{infile} );

    # Get the parameters set by user or defaults
    my $bamlist   = ( exists $params{bamlist} ? $params{bamlist} :
                      $rh_names->{variant_dir} .'/'. $rh_names->{bamlist_file} );
    my $outfile   = ( exists $params{outfile} ? $params{outfile} :
                      $rh_names->{variant_dir} .'/'. $rh_names->{qbp_outfile} );
    my $logfile   = ( exists $params{logfile} ? $params{logfile} :
                      $rh_names->{variant_dir} .'/'. $rh_names->{qbp_logfile} );
    my $reference = ( exists $params{reference} ?
                      $params{reference} : qcmg_default('hs_ref_fa') );

    # Added 2013-08-03 to sync read selection for qbasepileup and qSNP
    my $filter    = 'and(flag_DuplicateRead==false, CIGAR_M>34, MD_mismatch <= 3, '.
                         'option_SM > 10, flag_ReadUnmapped==false, '.
                         'flag_NotprimaryAlignment==false, '.
                         'flag_ReadFailsVendorQuality==false)';

    my $cmdline = $self->{qbasepileup_bin} .' '.
                   "-f maf ".
                   "-s $params{infile} ".
                   "-b $bamlist ".
                   "-r $reference ".
                   "-o $outfile ".
                   "--filter '$filter' ".
                   "--log $logfile"; 

    qlogprint( "qbasepileup cmdline: $cmdline\n" ) if $self->verbose;

    # Check that we can read/write to all specified files
    die "unable to read from -g $reference\n" unless (-r $reference);
    die 'unable to read from -s '. $params{infile} ."\n" unless (-r $params{infile});
    die 'unable to read from -b '. $bamlist ."\n" unless (-r $bamlist);

    # JVP : something is wrong with these tests - disable for now
    #die "unable to write to -o $outfile\n" unless (-w $outfile);
    #die "unable to write to --log $logfile\n" unless (-w $logfile);

    if (-r $outfile) {
        # qbasepileup java will not overwrite an existing file so we
        # might as well ditch right now if we know that's the case
        warn "qbasepileup ouptut file already exists - delete to regenerate [$outfile]\n";
        # If we didn't do a run then we zero out the remembered run values
        $self->{last_run} = {};
        return 1;
    } 
    else {
        # Run it!
        if (system($cmdline) != 0) {
            # You can check all the failure possibilities by inspecting $? like this:
            if ($? == -1) {
                qlogprint "failed to execute qbasepileup: $!\n";
            }
            elsif ($? & 127) {
                qlogprint( sprintf( "qbasepileup died with signal %d, %s coredump\n",
                                   ($? & 127), ($? & 128) ? 'with' : 'without' ));
            }
            else {
                qlogprint( sprintf "qbasepileup exited with value %d\n", $? >> 8 );
            }
        }

        # Save away all of the actual values used for this run
        $self->{last_run}->{infile}    = $params{infile};
        $self->{last_run}->{bamlist}   = $bamlist;
        $self->{last_run}->{reference} = $reference;
        $self->{last_run}->{outfile}   = $outfile;
        $self->{last_run}->{logdile}   = $logfile;
    }

    return 0;
}


sub infile {
    my $self = shift;
    return (exists  $self->{last_run}->{infile} and
            defined $self->{last_run}->{infile}) ?
            $self->{last_run}->{infile} : undef;
}


sub bamlist {
    my $self = shift;
    return (exists  $self->{last_run}->{bamlist} and
            defined $self->{last_run}->{bamlist}) ?
            $self->{last_run}->{bamlist} : undef;
}


sub reference {
    my $self = shift;
    return (exists  $self->{last_run}->{reference} and
            defined $self->{last_run}->{reference}) ?
            $self->{last_run}->{reference} : undef;
}


sub outfile {
    my $self = shift;
    return (exists  $self->{last_run}->{outfile} and
            defined $self->{last_run}->{outfile}) ?
            $self->{last_run}->{outfile} : undef;
}


sub logfile {
    my $self = shift;
    return (exists  $self->{last_run}->{logfile} and
            defined $self->{last_run}->{logfile}) ?
            $self->{last_run}->{logfile} : undef;
}


1;

__END__


=head1 NAME

QCMG::Run::Qbasepileup - Module to encapsulate running Adama qbasepileup executable


=head1 SYNOPSIS

 use QCMG::Run::Qbasepileup;


=head1 DESCRIPTION

This module provides a standardised way to invoke the qbasepileup
executable from Adama.

=head1 METHODS

=over

=item B<new()>

 my $qbp = QCMG::Run::Qbasepileup->new( verbose => 1 );

This method creates a factory that can be used to execute the run
methods.  There is only 1 settable variable here - the verbose level.

=item B<qbp_root()>

 $qbp->qbp_root( '/share/software/adama-nightly/' );

This method will set the directory that will used by all methods to
locate qbasepileup executables.  The default is
'/share/software/adama-nightly/' which is the correct location for the
most current version of qbasepileup on babyjesus so you do not have to set 
this variable unless you are
on a different machine or want to use a different location, e.g. a local
install for testing a new qbasepileup version.

=item B<qbasepileup()>

 $qbp->qbasepileup( infile          => '/path/to/my_maf.maf',
                    bamlist         => '/path/to/bamlist.txt' );
 $qbp->qbasepileup( infile          => '/path/to/my_maf.maf',
                    bamlist         => '/path/to/bamlist.txt',
                    outfile         => '/path/to/my_output.txt',
                    logfile         => '/path/to/my_output.log',
                    reference       => 'my_crazy_genome.fa' );
 
This method runs the qbasepileup executable.  It takes
5 parameters but only B<infile> and B<bamlist> are mandatory and the other
3 all have defaults:

 reference - see QCMG::Util::Util::qcmg_default()
 outfile - infile + '.qbasepileup.txt'
 logfile - infile + '.qbasepileup.log'

=item B<infile()>

=item B<bamlist()>

=item B<reference()>

=item B<outfile()>

=item B<logfile()>

Because 3 of the values used to run qbasepileup can be defaulted, we
supply methods that give access to the 5 parameter values that were
actually used (defaulted or user-supplied) to run the most recent
invocation of qbasepileup.

=item B<verbose()>

 $my_verbose = $qbp->verbose;
 $qbp->verbose( 2 );

Level of verbosity of diagnostics.  Default is 0. Higher numbers
give increased verbosity.




=back 

=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: Qbasepileup.pm 4665 2014-07-24 08:54:04Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013

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
