package QCMG::Lifescope::Directory;

##############################################################################
#
#  Module:   QCMG::Lifescope::Directory.pm
#  Creator:  John V Pearson
#  Created:  2011-09-31
#
#  Parses files from a LifeScope-created reports directory
#
#  $Id: $
#
###########################################################################

use Moose;  # implies use strict and warnings

use IO::File;
use Data::Dumper;
use Carp qw( carp croak cluck confess );
use QCMG::Lifescope::Cht;
use QCMG::Lifescope::ChtReader;
use QCMG::Lifescope::Tbl;
use QCMG::Lifescope::TblReader;
use vars qw( $SVNID $REVISION %PATTERNS );

( $REVISION ) = '$Revision: 1021 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: LogDirectory.pm 1021 2011-08-04 03:19:58Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


BEGIN {
    # Establish package global patterns for matching .cht filenames
    my $group  = '^Group_(\d+)';
    my $strand = '((?:Positive)|(?:Negative))';
    my $chrom  = '([\w\d]+(?:\.\d+)?)';
    my $tagext = '([FR][35P2BC-]{1,4})\.cht$'; # F3, R3, F5, F5-P2, F5-BC

    $PATTERNS{alignment_length} = 
        qr/$group\.Alignment\.Length\.Distribution\.$tagext/;
    $PATTERNS{alignment_length_unique} = 
        qr/$group\.Alignment\.Length\.Distribution\.Unique\.$tagext/;
    $PATTERNS{basemismatch} = 
        qr/$group\.BaseMismatch\.Distribution\.$tagext$/;
    $PATTERNS{basemismatch_unique} = 
        qr/$group\.BaseMismatch\.Distribution\.Unique\.$tagext/;
    $PATTERNS{base_qv} = 
        qr/$group\.BaseQV\.$tagext/;
    $PATTERNS{mapping_qv} = 
        qr/$group\.MappingQV\.$tagext/;
    $PATTERNS{coverage} = 
        qr/$group\.Coverage\.cht$/;
    $PATTERNS{coverage_by_strand} = 
        qr/$group\.Coverage\.by\.Strand\.$strand\.cht$/;
    $PATTERNS{coverage_by_chrom} = 
        qr/$group\.Coverage\.by\.Chromosome\.$chrom\.cht$/;
    $PATTERNS{mismatches_by_base_qv} = 
        qr/$group\.Mismatches\.by\.BaseQV\.$tagext/;
    $PATTERNS{mismatches_by_pos_base} = 
        qr/$group\.Mismatches\.by\.Position\.BaseSpace\.$tagext/;
    $PATTERNS{mismatches_by_pos_color} = 
        qr/$group\.Mismatches\.by\.Position\.ColorSpace\.$tagext/;
    $PATTERNS{insert_range} = 
        qr/$group\.Insert\.Range\.Distribution\.cht$/;
    $PATTERNS{pairing_qv} = 
        qr/$group\.PairingQV\.cht$/;
    $PATTERNS{pairing_stats} = 
        qr/$group\.Pairing\.Stats\.cht$/;
    $PATTERNS{read_pair_type} = 
        qr/$group\.ReadPair\.Type\.cht$/;
    $PATTERNS{summary_tbl} = 
        qr/$group\-summary\.tbl$/;
}


has 'directory' => ( is => 'ro', isa => 'Str', required => 1 );
has 'files'     => ( is => 'rw', isa => 'HashRef', default => sub{{}} );
has 'chts'      => ( is => 'rw', isa => 'ArrayRef', default => sub{[]} );
has 'tbls'      => ( is => 'rw', isa => 'ArrayRef', default => sub{[]} );
has 'verbose'   => ( is => 'rw', isa => 'Int', default => 0 );


sub BUILD {
    my $self = shift;
    $self->_initialise();
}


sub _initialise {
    my $self = shift;

    my $this_dir = $self->directory();

    opendir(DIR, $this_dir) || die "Can't opendir [$this_dir]: $!";
    my @everything = readdir(DIR);
    closedir DIR;

    # Store all of the file names
    foreach my $thing (@everything) {
        if (-f "$this_dir/$thing") {
            $self->{files}->{ $thing } = 1;
        }
    }

    # Parse each .cht file into a QCMG::LifeScope::Cht object
    # and each .tbl file into a QCMG::LifeScope::Tbl object
    my $cht_reader = QCMG::Lifescope::ChtReader->new();
    my $tbl_reader = QCMG::Lifescope::TblReader->new();
    foreach my $file (sort keys %{ $self->files }) {
        if ($file =~ /^.*\.cht$/) {
            my $cht = $cht_reader->parse_file( "$this_dir/$file" );
            push @{ $self->{chts} }, $cht;
        }
        elsif ($file =~ /^.*\.tbl$/) {
            my $tbl = $tbl_reader->parse_file( "$this_dir/$file" );
            push @{ $self->{tbls} }, $tbl;
        }
    }

    return $self;
}


sub as_xml {
    my $self = shift;
    my $xml = '';

    $xml .= '<LifeScopeDirectory ' .
            'dir="' . $self->directory . "\">\n";

    $xml .= "<LifeScopeChts>\n";
    foreach my $obj (@{ $self->{chts} }) {
        $xml .= $obj->as_xml;       
    }
    $xml .= "</LifeScopeChts>\n";

    $xml .= "<LifeScopeTbls>\n";
    foreach my $obj (@{ $self->{tbls} }) {
        $xml .= $obj->as_xml;       
    }
    $xml .= "</LifeScopeTbls>\n";

#    $xml .= _alignment_length_distribution( $self->{chts} );
#    $xml .= _coverage( $self->{chts} );
#    $xml .= _quality( $self->{chts} );
#    $xml .= _mismatches( $self->{chts} );
#    $xml .= _pairs( $self->{chts} );

    $xml .= "</LifeScopeDirectory>\n";
    return $xml;
}


sub _alignment_length_distribution {
    my @chtobjs = @{ shift(@_) };

    my $xml = "<Alignment_length_distributions>\n";
    foreach my $obj (@chtobjs) {
        if ($obj->file =~ /$PATTERNS{alignment_length}/) {
            my $group = $1;
            my $tag   = $2;
            $xml .= '  <Alignment_length_distribution name="' .
                    "$tag.overall\">\n";
            $xml .= $obj->as_xml;       
            $xml .= "  </Alignment_length_distribution>\n";
        }
        elsif ($obj->file =~ /$PATTERNS{alignment_length_unique}/) {
            my $group = $1;
            my $tag   = $2;
            $xml .= '  <Alignment_length_distribution name="' .
                    "$tag.unique\">\n";
            $xml .= $obj->as_xml;       
            $xml .= "  </Alignment_length_distribution>\n";
        }
    }
    $xml .= "</Alignment_length_distributions>\n";

    return $xml;
}


sub _coverage {
    my @chtobjs = @{ shift(@_) };

    my $xml = "<Coverages>\n";

    # Summaries - overall and by strand
    foreach my $obj (@chtobjs) {
        if ($obj->file =~ /$PATTERNS{coverage}/) {
            my $group = $1;
            $xml .= '<Coverage name="' . "Summary\">\n";
            $obj->trim_data_table(99);
            $xml .= $obj->as_xml;       
            $xml .= "</Coverage>\n";
        }
        elsif ($obj->file =~ /$PATTERNS{coverage_by_strand}/) {
            my $group  = $1;
            my $strand = $2;
            $xml .= '<Coverage name="' . "$strand.strand\">\n";
            $obj->trim_data_table(99);
            $xml .= $obj->as_xml;
            $xml .= "</Coverage>\n";
        }
    }

    $xml .= "<Coverages_by_chrom>\n";
    foreach my $obj (@chtobjs) {
        if ($obj->file =~ /$PATTERNS{coverage_by_chrom}/) {
            my $group = $1;
            my $chrom = $2;
            $xml .= '  <Coverage name="' . "$chrom\">\n";
            $obj->trim_data_table(99);
            #$ Mito is a special case
            $obj->bin_data_table(100) if ($chrom =~ /chrM/);
            $xml .= $obj->as_xml;
            $xml .= "  </Coverage>\n";
        }
    }
    $xml .= "</Coverages_by_chrom>\n";

    $xml .= "</Coverages>\n";

    return $xml;
}


sub _quality {
    my @chtobjs = @{ shift(@_) };

    my $xml = "<Qualities>\n";
    foreach my $obj (@chtobjs) {
        if ($obj->file =~ /$PATTERNS{base_qv}/) {
            my $group = $1;
            my $tag   = $2;
            $xml .= '  <Quality name="' . "$tag.baseQV\">\n";
            $obj->trim_data_table(99);
            $xml .= $obj->as_xml;       
            $xml .= "  </Quality>\n";
        }
        elsif ($obj->file =~ /$PATTERNS{mapping_qv}/) {
            my $group = $1;
            my $tag   = $2;
            $xml .= '  <Quality name="' . "$tag.mappingQV\">\n";
            $obj->trim_data_table(99.99);
            $xml .= $obj->as_xml;       
            $xml .= "  </Quality>\n";
        }
    }
    $xml .= "</Qualities>\n";

    return $xml;
}


sub _mismatches {
    my @chtobjs = @{ shift(@_) };

    my $xml = "<Mismatches>\n";
    foreach my $obj (@chtobjs) {
        if ($obj->file =~ /$PATTERNS{basemismatch}/) {
            my $group = $1;
            my $tag   = $2;
            $xml .= '  <Mismatch name="' . "$tag.base\">\n";
            $obj->trim_data_table(99.99);
            $xml .= $obj->as_xml;       
            $xml .= "  </Mismatch>\n";
        }
        elsif ($obj->file =~ /$PATTERNS{basemismatch_unique}/) {
            my $group = $1;
            my $tag   = $2;
            $obj->trim_data_table(99.99);
            $xml .= '  <Mismatch name="' . "$tag.base_uniq\">\n";
            $xml .= $obj->as_xml;       
            $xml .= "  </Mismatch>\n";
        }
        elsif ($obj->file =~ /$PATTERNS{mismatches_by_base_qv}/) {
            my $group = $1;
            my $tag   = $2;
            $xml .= '  <Mismatch name="' . "$tag.base_qv\">\n";
            $obj->trim_data_table(99.99);
            $xml .= $obj->as_xml;       
            $xml .= "  </Mismatch>\n";
        }
        elsif ($obj->file =~ /$PATTERNS{mismatches_by_pos_base}/) {
            my $group = $1;
            my $tag   = $2;
            $xml .= '  <Mismatch name="' . "$tag.pos_base\">\n";
            $xml .= $obj->as_xml;       
            $xml .= "  </Mismatch>\n";
        }
        elsif ($obj->file =~ /$PATTERNS{mismatches_by_pos_color}/) {
            my $group = $1;
            my $tag   = $2;
            $xml .= '  <Mismatch name="' . "$tag.pos_color\">\n";
            $xml .= $obj->as_xml;       
            $xml .= "  </Mismatch>\n";
        }
    }
    $xml .= "</Mismatches>\n";

    return $xml;
}


sub _element {
    my $elem = shift;
    my $content = shift;
    return '<'.$elem.'>'.$content.'</'.$elem.">\n";
}



1;
__END__


=head1 NAME

QCMG::Lifescope::Directory - Parse a LifeScope outputs directory


=head1 SYNOPSIS

 use QCMG::Lifescope::Directory;

 my $dir = '/panfs/seq_raw/S8006_20110805_1_LThg19/outputs/bamstats/Group1';
 my $ls = QCMG::Lifescope::Directory->new( directory => $dir );


=head1 DESCRIPTION

This module provides an interface for reading and processing report 
files from a LifeScope outputs directory.  An outputs directory
typically contains a large number of files with a .cht extension plus
two other summary files - Group_N_statstuple.txt, Group_N-summary.tbl.
Note that all files will have a I<Group_N> prefix where Group relates to
the number of samples multiplexed in the run - non barcoded runs have
a single Group (Group_1) and barcoded runs have as many groups as there
are barcodes (1..N).

Different runtypes have different .cht files. Most files are in common
between runtypes but their count will differ depending on how many tags were
in the run.  Frag runs will have all files with a I<F3> postfix as
there is a single tag but an LMP run will have almost twice as many .cht
files as most will exist in both F3 and R3 forms.

The list below shows the files that are in common between LMP and Frag
runs.  <group> is the Group identifier and is of the form I<Group_N> and
<tag> is of the form I<F3, R3, F5> etc.

 <group>.Alignment.Length.Distribution.<tag>.cht
 <group>.Alignment.Length.Distribution.Unique.<tag>.cht
 <group>.BaseMismatch.Distribution.<tag>.cht
 <group>.BaseMismatch.Distribution.Unique.<tag>.cht
 <group>.BaseQV.<tag>.cht
 <group>.MappingQV.<tag>.cht
 <group>.Coverage.by.Chromosome.<chrom>.cht
 <group>.Coverage.by.Strand.<strand>.cht
 <group>.Coverage.cht
 <group>.Mismatches.by.BaseQV.<tag>.cht
 <group>.Mismatches.by.Position.BaseSpace.<tag>.cht
 <group>.Mismatches.by.Position.ColorSpace.<tag>.cht

Paired runs (currently just LMP) have additional .cht files:

 <group>.Insert.Range.Distribution.cht
 <group>.PairingQV.cht
 <group>.Pairing.Stats.cht
 <group>.ReadPair.Type.cht

All runtypes have 2 plain text report files:

 <group>_statstuple.txt
 <group>-summary.tbl

=head2 Report format

The layout of the XML file does not directly relate to the output format
but it is simpler if the XML elemtns occur in the XML file in the order
that they will need to be processed and output for the HTML report file.
This is the proposed tab structure of the final LifeScope report:

 Alignment length distribution
     tag.overall
     tag.unique
 Coverage
     Summary
     Strands
     By chromosome
         tag.chrom
 Quality
     tag.baseqv
     tag.mappingqv
 Mismatches
     tag.distribution
     tag.distribution_unique
     tag.baseqv
     tag.basespace_position
     tag.colorspace_position
 Pairs
     insert distribution
     pairing QV
     pairing stats
     readpair type


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $dir = '/panfs/seq_raw/S8006_20110805_1_LThg19/outputs/bamstats/Group1';
 my $ls = QCMG::Lifescope::Directory->new( directory => $dir );

A directory parameter must be supplied to this constructor.

=item B<directory()>
 
 $ls->directory();

Returns the name of the directory processed by this object.

=item B<files()>

 $ls->files();

Returns a reference to a hash of the names of all the files found in the
log directory.  Note that this reference is to the internal filename
hash so the user should not modify this hash.

=item B<chts()>

 $ls->chts();

Returns a reference to an array of QCMG::Lifescope::Cht objects
containing the parsed data from the .cht files found in the directory.

=item B<tbls()>

 $ls->tbls();

Returns a reference to an array of QCMG::Lifescope::Tbl objects
containing the parsed data from the .tbl files found in the directory.

=item B<verbose()>

 $ls->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: LogDirectory.pm 1021 2011-08-04 03:19:58Z j.pearson $


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
