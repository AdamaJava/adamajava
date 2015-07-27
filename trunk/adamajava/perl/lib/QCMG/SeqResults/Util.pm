package QCMG::SeqResults::Util;

###########################################################################
#
#  Module:   QCMG::SeqResults::Util.pm
#  Creator:  John V Pearson
#  Created:  2011-05-16
#
#  Non-OO utility functions for QCMG::SeqResults collection.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use File::Find;
use Mail::Sendmail;
use Data::Dumper;
use Exporter qw( import );

use QCMG::FileDir::Finder;
use QCMG::SamTools::Sam;

use vars qw( $SVNID $REVISION @EXPORT_OK %PATTERNS );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

@EXPORT_OK = qw( is_valid_library_name
                 is_valid_final_name
                 is_valid_mapset_name is_valid_slide_name
                 is_valid_physdiv_name is_valid_barcode_name
                 is_valid_extra_name is_valid_donor_name
                 is_valid_expt_name is_valid_type_name
                 qmail
                 check_validity_of_bam_names
                 bams_in_directory filter_bams_by_pattern
                 seqmapped_bams seqlib_bams seqfinal_bams
                 bams_that_were_merged_into_this_bam );

BEGIN {
    # SOLiD
    $PATTERNS{solid_slide_pattern} = 
        qr/^(?:S\d{4,5}|[a-z]+)_20\d{6}_[12]{1}(?:_[a-zA-Z]+)?$/;
    # HiSeq
    $PATTERNS{hiseq_slide_pattern} = 
        qr/^\d{6}_SN\d+_\d+_[A-Z0-9]+$/;
    # IonTorrent
    $PATTERNS{torrent_slide_pattern} = 
        qr/^T\d{2,5}_20\d{6}_\d+$/;
    $PATTERNS{physdiv_pattern} = 
        qr/^nopd|(?:(?:quad|octet|lane)(?:_\d+)+)$/;
    $PATTERNS{barcode_pattern} = 
        qr/^nobc|[ACGT]{1,}|IonXpress_\d+|MID_\d+|(?:(?:S|bcA|bcB)\d+_\d+)$/;
    $PATTERNS{extra_pattern} = 
        qr/^bam|MD|wt$/;
    $PATTERNS{library_pattern} = 
        qr/^Library_(?:\d{8}_[A-Z]{1})|BIA[A-Z0-9]+(?:\.bam)?$/;
    $PATTERNS{donor_pattern} = 
        qr/^(?:(?:AOCS|APGI|PPPP|COLO)_\d+)|(?:CRL_\d+_Panc_\d{2}_\d{2})$/;
    $PATTERNS{desc_pattern} = 
        qr/^[\w_]+$/;
    $PATTERNS{user_pattern} = 
        qr/^[a-z]+$/;
   
    # These were for the original seq_final BAM pattern but the new JIT 
    # naming scheme does not use them
    $PATTERNS{expt_pattern} = 
        qr/^exome|genome|mRNA$/;
    $PATTERNS{type_pattern} = 
        qr/^[ACFNMSTX]{1}[DR]{1}$/;
}


sub is_valid_mapset_name {
    my $filename = shift;

    my @sections = split /\./, $filename;
    if ( is_valid_slide_name( $sections[0] ) and
         is_valid_physdiv_name( $sections[1] ) and
         is_valid_barcode_name( $sections[2] ) ) {
        # There may or may not be an "extra" section
        if (scalar(@sections) == 3) {
            return 1;
        }
        elsif (scalar(@sections) == 4) {
            if (is_valid_extra_name($sections[3])) {
                return 1;
            }
            else {
                return 0;
            }
        }
        else {
            return 0;
        }
    }
    return 0;
}
 

sub is_valid_final_name {
    my $filename = shift;

    if ( is_valid_new_final_name( $filename ) or
         is_valid_old_final_name( $filename ) ) {
        return 1;
    }
    return 0;
}


sub is_valid_new_final_name {
    my $filename = shift;

    my @sections = split /\./, $filename;
    if ( is_valid_donor_name( $sections[0] ) and
         is_valid_desc_name( $sections[1] ) and
         is_valid_user_name( $sections[2] ) ) {
        return 1;
    }
    return 0;
}
 

sub is_valid_old_final_name {
    my $filename = shift;

    my @sections = split /\./, $filename;
    if ( is_valid_donor_name( $sections[0] ) and
         is_valid_expt_name( $sections[1] ) and
         is_valid_type_name( $sections[2] ) ) {
        return 1;
    }
    return 0;
}
 

sub is_valid_slide_name {
    my $slide = shift;
    if ( $slide =~ /$PATTERNS{hiseq_slide_pattern}/ or
         $slide =~ /$PATTERNS{torrent_slide_pattern}/ or
         $slide =~ /$PATTERNS{solid_slide_pattern}/ ) {
        return 1;
    }
    return 0;
}


sub is_valid_physdiv_name {
    my $physdiv = shift;
    if ( $physdiv =~ /$PATTERNS{physdiv_pattern}/ ) {
        return 1;
    }
    return 0;
}


sub is_valid_barcode_name {
    my $barcode = shift;
    if ( $barcode =~ /$PATTERNS{barcode_pattern}/ ) {
        return 1;
    }
    return 0;
}


sub is_valid_extra_name {
    my $extra = shift;
    if ( $extra =~ /$PATTERNS{extra_pattern}/ ) {
        return 1;
    }
    return 0;
}


sub is_valid_library_name {
    my $library = shift;
    if ( $library =~ /$PATTERNS{library_pattern}/ ) {
        return 1;
    }
    return 0;
}


sub is_valid_donor_name {
    my $donor = shift;
    if ( $donor =~ /$PATTERNS{donor_pattern}/ ) {
        return 1;
    }
    return 0;
}


sub is_valid_expt_name {
    my $expt = shift;
    if ( $expt =~ /$PATTERNS{expt_pattern}/ ) {
        return 1;
    }
    return 0;
}


sub is_valid_type_name {
    my $type = shift;
    if ( $type =~ /$PATTERNS{type_pattern}/ ) {
        return 1;
    }
    return 0;
}


sub is_valid_desc_name {
    my $desc = shift;
    if ( $desc =~ /$PATTERNS{desc_pattern}/ ) {
        return 1;
    }
    return 0;
}


sub is_valid_user_name {
    my $user = shift;
    if ( $user =~ /$PATTERNS{user_pattern}/ ) {
        return 1;
    }
    return 0;
}


sub qmail {
    my %params = @_;

    # Use first email address as recipient.  If additional addresses
    # were specified then format them for use with the 'Cc' option.

    my @addresses = @{$params{To}};
    my $recipient = shift @addresses;  # First address = recipient
    if ( scalar(@addresses) > 0 ) {
        $params{Cc} = join(', ', @addresses);
    }
    $params{To} = $recipient;

    sendmail( %params );
}


sub check_validity_of_bam_names{
    my @bams = @_; 

    my %valid_names   = ();
    my %invalid_names = ();
    foreach my $pathname (@bams) {
        my $bam = $pathname;
        $bam =~ s/.*\///g;  # ditch the path
        if ( is_valid_mapset_name($bam) or
             is_valid_library_name($bam) or
             is_valid_final_name($bam) ) {
            $valid_names{ $bam } = $pathname; 
        }
        else {
            $invalid_names{ $bam } = $pathname; 
        }
    }
    return \%valid_names, \%invalid_names;
}       


sub bams_in_directory {
    my $dir = shift;

    my $finder = QCMG::FileDir::Finder->new( verbose => 0 );
    my @bams   = $finder->find_file( $dir, '\.bam$' );
    return @bams;
}


sub filter_bams_by_pattern {
    my $pattern = shift;
    my @inbams  = @_;

    my @bams = ();
    foreach my $bam (@inbams) {
        next unless ($bam =~ /$pattern/);
        push @bams, $bam;
    }
    return @bams;
}


sub hash_on_bam_name {
    my @bams = @_;
    my %bams = ();
    foreach my $bampath (@bams) {
        $bampath =~ /([^\/]+)$/;  # match just BAM name
        my $bamfile = $1;
        #die "BAM $bamfile already been seen [", $bams{ $bamfile },
        #    $bampath,']' if exists $bams{ $bamfile };
        #Matt's Hacks
        if ( exists $bams{ $bamfile } ) {
            warn "BAM has already been seen [", $bams{ $bamfile },' & ',$bampath,"]\n";
        }
        else {
            $bams{ $bamfile } = $bampath;
        }
    }
    return %bams;
}


sub seqmapped_bams {
    my $dir = shift;
    # Find every BAM in a seq_mapped/ directory
    return hash_on_bam_name (
               filter_bams_by_pattern( qr{/seq_mapped/},
                   bams_in_directory( $dir ) ) );
}


sub seqlib_bams {
    my $dir = shift;
    # Find every BAM in a seq_lib/ directory
    return hash_on_bam_name (
               filter_bams_by_pattern( qr{/seq_lib/},
                   bams_in_directory( $dir ) ) );
}


sub seqfinal_bams {
    my $dir = shift;
    # Find every BAM in a seq_final/ directory
    return hash_on_bam_name (
               filter_bams_by_pattern( qr{/seq_final/},
                   bams_in_directory( $dir ) ) );
}


sub bams_that_were_merged_into_this_bam {
    my $filename = shift;

    # Take a BAM, pull the @RG lines out of the header, look for the ZC
    # field and extract the bams

    # The SamTools code has a very annoying error message that can't be
    # suppressed (from C?) so we're going to do a little bit of work
    # first in Perl to make sure it exists and is readable

    my $bam = undef;
    if (-e $filename and -r $filename) {
        $bam = QCMG::SamTools::Bam->open( $filename );
        return undef if (! defined $bam);
    }
    else {
        warn "Unable to open BAM $filename to check sub-BAMs - ".
             "file does not exist or is not readable\n";
        return undef;
    }

    my $header = $bam->header->text;  # get text of header

    my @lines = split "\n", $header;
    my @rgs = grep { /^\@RG/ } @lines;

    my @bams = ();
    foreach my $rg (@rgs) {
        if ($rg =~ /^\@RG\s+.*ZC:(?:Z:)?(?:\d+:)?([^\s]+).*$/i ) {
            push @bams, $1;
        }
        else {
           warn "No ZC clause for \@RG line in file $filename\n";
        }
    }
    
    return \@bams;
}


sub bams_that_were_merged_into_this_bam_OLD {
    my $filename = shift;

    # Take a BAM, pull the @PG lines out of the header, look for any
    # @PGs relating to qbammerge and if found, pull out all of the input
    # BAMs (-i option) for all of the merges and return arrayref.

    my $bam = QCMG::SamTools::Bam->open( $filename );
    my $header = $bam->header->text;  # get text of header

    my @lines = split "\n", $header;
    my @pgs = grep { /^\@PG\s+.*PN:qbammerge.*$/ } @lines;

    my @bams = ();
    foreach my $pg (@pgs) {
        my @fields = split "\t", $pg;
        my @cls = grep { /^CL:/ } @fields;
        foreach my $cl (@cls) {
            my @inputs = ($cl =~ /-i ([^\s\n]+)/g);
            push @bams, @inputs;
        }
    }
    
    return \@bams;
}


1;

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
