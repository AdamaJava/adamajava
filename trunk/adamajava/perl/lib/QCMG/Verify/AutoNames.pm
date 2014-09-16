package QCMG::Verify::AutoNames;

###########################################################################
#
#  Module:   QCMG::Verify::AutoNames
#  Creator:  John V Pearson
#  Created:  2013-07-29
#
#  This is a factory class for generating parameters required for
#  automated running of processes associated with verification.  It has
#  methods that take a variety of inputs and produce hashes of outputs.
#  The advantage of abstracting this code out into a separate module is
#  that it can easily be shared between multiple other classes.
#
#  $Id: AutoQbasepileup.pm 4151 2013-07-29 06:06:23Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak carp confess );
use Data::Dumper;
use IO::File;

use QCMG::Run::Qbasepileup;
use QCMG::Util::QLog;
use QCMG::QBamMaker::SeqFinalDirectory;

use vars qw( $SVNID $REVISION %BAM_CATEGORY %LIMITS );

( $REVISION ) = '$Revision: 4151 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: AutoQbasepileup.pm 4151 2013-07-29 06:06:23Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    # Set defaults allowing for user override
    my $self = { verbose => ($params{verbose} ? $params{verbose} : 0) };

    bless $self, $class;
}


sub params_from_maf {
    my $self = shift;
    my $maf  = shift;

    qlogprint "parsing information from name of MAF file [$maf]\n";

    # Parse info out of the MAF pathname
    my $parent_proj_dir = '';
    my $project         = '';
    my $tool            = '';
    my $uuid            = '';
    my $maf_file        = '';
    if ($maf =~ /^(.*)\/([^\/]+)\/variants\/([^\/]+)\/([^\/]+)\/(.*maf)$/) {
        $parent_proj_dir = $1;
        $project         = $2;
        $tool            = $3;
        $uuid            = $4;
        $maf_file        = $5;
    }
    else {
        qlogprint "params_from_maf(): maf file [$maf] does not match expected pattern\n";
        return {};
    }

    my $seq_final_dir  = "$parent_proj_dir/$project/seq_final";
    my $variant_dir    = "$parent_proj_dir/$project/variants/$tool/$uuid";
    my $bamlist_file   = $project .'.bamlist.txt';
    my $qbp_outfile    = $maf_file .'.qbasepileup.txt';
    my $qbp_logfile    = $maf_file .'.qbasepileup.log';
    my $maf_verif_file = $maf_file;
    $maf_verif_file =~ s/\.maf$/\.qverified\.maf/;

    return { project         => $project,
             parent_proj_dir => $parent_proj_dir,
             tool            => $tool,
             uuid            => $uuid,
             maf_file        => $maf_file,
             seq_final_dir   => $seq_final_dir,
             variant_dir     => $variant_dir,
             bamlist_file    => $bamlist_file,
             qbp_outfile     => $qbp_outfile,
             qbp_logfile     => $qbp_logfile,
             maf_verif_file  => $maf_verif_file };
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


1;

__END__

=head1 NAME

QCMG::Verify::AutoNames - predict run params from pathnames


=head1 SYNOPSIS

 use QCMG::Verify::AutoNames;


=head1 ABSTRACT

This is a factory class for generating parameters required for
automated running of processes associated with verification.  It has
methods that take a variety of inputs and produce hashes of outputs.
The advantage of abstracting this code out into a separate module is
that it can easily be shared between multiple other classes.


=head1 DESCRIPTION

=head2 Public Methods

=over

=item B<new()>

 my $fact = QCMG::Verify::AutoNames->new( verbose => 1 );
            
The new method takes a single optional parameter - verbose.  Because the
logging is automated, the default verbose level has been set to 1 so
that logs are more detailed.

=item B<params_from_maf()>
 
 $mymaf = '/absolute/path/panc/APGI_0001/variants/qSNP/123456/my_maf.maf';
 $fact->params_from_maf( $mymaf );

 # Would return a hashref that looks like:

 { project         => APGI_0001
   parent_proj_dir => /absolute/path/panc/,
   seq_final_dir   => /absolute/path/panc/APGI_0001/seq_final,
   variant_dir     => /absolute/path/panc/APGI_0001/variants/qSNP/123456,
   tool            => qSNP,
   uuid            => 123456,
   bamlist_file    => APGI_0001.bamlist.txt,
   maf_file        => my_maf.maf
   qbp_outfile     => my_maf.maf.qbasepileup.txt,
   qbp_logfile     => my_maf.maf.qbasepileup.log,
   maf_verif_file  => my_maf.qverified.maf,
 }

This method takes an absolute pathname to a MAF file and predicts a heap
of useful parameters and filenames from the MAF.  The intent is that
this is a one-stop-shop for setting these names so if every other QCMG
module or script uses this method to predict MAF-related file names and
directories, then we only ever have to change these in one place if
change is needed.

=item B<verbose>

Print progress and diagnostic messages.  Higher numbers enable 
higher levels of verbosity.

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: AutoQbasepileup.pm 4151 2013-07-29 06:06:23Z j.pearson $


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
