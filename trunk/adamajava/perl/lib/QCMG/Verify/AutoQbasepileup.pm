package QCMG::Verify::AutoQbasepileup;

###########################################################################
#
#  Module:   QCMG::Verify::AutoQbasepileup
#  Creator:  John V Pearson
#  Created:  2013-07-26
#
#  This a factory class for running qbasepileup against MAF files in an
#  automated fashion. Contains logic previously in qverify.pl. 
#  
#  The run method of this class takes the absolute pathname to a MAF file
#  sitting within QCMG's /mnt/seq_results directory structure and, based 
#  on the pathname, collects the information required to appropriately call
#  the java qbasepileup tool, runs qbasepileup, and puts the output and
#  log files back into the same directory as the MAF file.
#
#  $Id: AutoQbasepileup.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak carp confess );
use Data::Dumper;
use IO::File;

use QCMG::FileDir::Finder;
use QCMG::FileDir::QSnpDirRecord;
use QCMG::Run::Qbasepileup;
use QCMG::Util::QLog;
use QCMG::Verify::AutoNames;
use QCMG::QBamMaker::SeqFinalDirectory;

use vars qw( $SVNID $REVISION %BAM_CATEGORY %LIMITS );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: AutoQbasepileup.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    # Note that (unusually) we have set the default verbose level to 1
    my $self = { qbp     => undef,
                 verbose => ($params{verbose} ? $params{verbose} : 1) };

    bless $self, $class;
}


sub run {
    my $self = shift;
    my $maf  = shift;

    qlogprint "processing MAF file [$maf]\n";

    # Parse info out of the MAF pathname
    my $names = QCMG::Verify::AutoNames->new( verbose => $self->verbose );
    my $rh_names = $names->params_from_maf( $maf );

    # Always remake bamlist file so delete it if it exists
    my $bamlist = $rh_names->{variant_dir} .'/'. $rh_names->{bamlist_file};
    if (-r $bamlist) {
        qlogprint "regenerating bamlist file [$bamlist]\n";
        my $dcount = unlink $bamlist;
    }
    
    # Get the bamlist file sorted
    my $fact = QCMG::QBamMaker::SeqFinalDirectory->new( verbose => $self->verbose );
    my $coll = $fact->process_directory( $rh_names->{seq_final_dir} );

    # Make sure we only have BAMs for one donor
#qlogprint "new_collections_sorted_by_donor()\n";
    my $rh_collections = $coll->new_collections_sorted_by_donor;
#qlogprint "\@found_donors()\n";
    my @found_donors = keys %{$rh_collections};
    die('Found BAMs for multiple donors: ',join(',',@found_donors),"\n")
        if (scalar(@found_donors) > 1);

    # Get our collection and write out a bamlist
#qlogprint "\$bam_coll\n";
    my $bam_coll = $rh_collections->{ $rh_names->{project} };

    # If $bam_coll is undef, no BAM files is the probable explanation
#qlogprint "check for undef \$bam_coll\n";
    if (! defined $bam_coll) {
#qlogprint "\$bam_coll is undef so returning\n";
        warn 'No BAM files found in seq_final dir ',
             $rh_names->{seq_final_dir}, "\n";
        return undef;
    }

    qlogprint 'Collection for donor ', $rh_names->{project}, 
              ' has ', $bam_coll->record_count, " records\n";
    $bam_coll->write_bamlist_file( $bamlist );

    # Now call qbasepileup, possibly differently depending on which
    # variant call tool was used.  If qbasepileup did not run for any
    # reason, die immediately.
    if ($rh_names->{tool} eq 'qSNP') {
        # Run qbasepileup 
        my $run = QCMG::Run::Qbasepileup->new( verbose => $self->verbose );
        my $stat = $run->qbasepileup( infile => $maf );
        die "qbasepileup did not run - status: $stat\n" unless ($stat == 0);
        $self->{qbp} = $run;
    }
    elsif ($rh_names->{tool} eq 'GATK') {
        # Run qbasepileup 
        my $run = QCMG::Run::Qbasepileup->new( verbose => $self->verbose );
        my $stat = $run->qbasepileup( infile => $maf );
        die "qbasepileup did not run - status: $stat\n" unless ($stat == 0);
        $self->{qbp} = $run;
    }
    else {
        die 'Cannot currently process variant calls from ',
            $rh_names->{tool},"\n";
    }

}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


# These are simple pass-through methods

sub infile {
    my $self = shift;
    return $self->{qbp}->infile;
}

sub bamlist {
    my $self = shift;
    return $self->{qbp}->bamlist;
}

sub reference {
    my $self = shift;
    return $self->{qbp}->reference;
}

sub outfile {
    my $self = shift;
    return $self->{qbp}->outfile;
}

sub logfile {
    my $self = shift;
    return $self->{qbp}->logfile;
}
    

1;

__END__

=head1 NAME

QCMG::Verify::AutoQbasepileup - run qbasepileup on a given MAF file


=head1 SYNOPSIS

 use QCMG::Verify::AutoQbasepileup;


=head1 ABSTRACT

This a factory class for running qbasepileup against MAF files in an
automated fashion.
The run method of this class takes the absolute pathname to a MAF file
sitting within QCMG's /mnt/seq_results directory structure and based on 
the pathname, it collects all the information required to appropriately 
call the java qbasepileup tool and then runs qbasepileup, putting the 
results back into the same directory as the MAF file.


=head1 DESCRIPTION

=head2 Public Methods

=over

=item B<new()>

 my $fact = QCMG::Verify::AutoQbasepileup->new( verbose => 1 );
            
The new method takes a single optional parameter - verbose.  Because the
logging is automated, the default verbose level has been set to 1 so
that logs are more detailed.

=item B<run>
 
 $fact->run( '/absolute/path/to/my_maf.maf' );

MAF file containing the variant positions to be tested.

=item B<infile()>

=item B<bamlist()>

=item B<reference()>

=item B<outfile()>

=item B<logfile()>

These 5 methods provide access to the values used for the most recent
invocation of the underlying qbasepileup java binary.  In our case they
are all defaulted by the internal logic of this and other modules but it
can still be useful to know where the output files were written, name of
the logfile etc.

=item B<verbose>

Print progress and diagnostic messages.  Higher numbers enable 
higher levels of verbosity.

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: AutoQbasepileup.pm 4665 2014-07-24 08:54:04Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013,2014

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
