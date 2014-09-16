package QCMG::FileDir::SeqRawParser;

##############################################################################
#
#  Module:   QCMG::FileDir::SeqRawParser.pm
#  Creator:  John V Pearson
#  Created:  2010-10-12
#
#  This module pulls apart a seq_raw directory to determine key aspects
#  of the run including the barcodes (if any), the primary directories
#  and the run type (LMP, PE, Frag, etc)
#
#  $Id: SeqRawParser.pm 4662 2014-07-23 12:39:59Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Data::Dumper;
use Carp qw( carp croak confess );
use vars qw( $SVNID $REVISION );

use QCMG::FileDir::Finder;

( $REVISION ) = '$Revision: 4662 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: SeqRawParser.pm 4662 2014-07-23 12:39:59Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

# Establish class global list of valid tags
our $VALID_TAGS = { 'F3'    => [ qw( LMP Frag FragPE FragPEBC ) ],
                    'R3'    => [ qw( LMP ) ],
                    'F5-P2' => [ qw( FragPE ) ],
                    'F5-BC' => [ qw( FragPEBC ) ],
                    'BC'    => [ qw( FragBC FragPEBC ) ] };

sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { barcode_pattern => '(?:\w{3,5}_\d\d)|(?:unassigned)|(?:missing-f3)',
                 primary_pattern => 'primary\.\d+',
                 csfasta_pattern => '\.csfasta$',
                 tag_type_with_bc_pattern => '_([FRBC35]{2})_\w{3,5}_\d\d\.csfasta$',
                 tag_type_no_bc_pattern   => '_([^_]+)\.csfasta$',
                 read_type_with_barcode_pattern => '\.csfasta$',
                 barcode_dirs    => [],
                 barcodes        => {},
                 primary_dirs    => [],
                 primaries       => {},
                 csfasta_files   => [],
                 taglengths      => {},
                 dir             => '',
                 finder          => undef,
                 verbose         => ($params{verbose} ? $params{verbose} : 0)};
    bless $self, $class;

    my $finder = QCMG::FileDir::Finder->new( verbose => $self->verbose );
    $self->{finder} = $finder;

    return $self;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub barcodes {
    my $self = shift;

    my @barcodes = sort keys %{ $self->{barcodes} };
    return @barcodes;
}


sub barcode_count {
    my $self = shift;

    my @barcodes = $self->barcodes;
    return scalar(@barcodes);
}


sub process {
    my $self = shift;
    my $dir  = shift;

    # Because we may use an instance of this class as a factory, the
    # first task in processing a new directory is to wipe away the
    # values from any previously parsed directory.

    $self->{barcode_dirs}    = [];
    $self->{barcodes}        = {};
    $self->{primary_dirs}    = [];
    $self->{primaries}       = {};
    $self->{csfasta_files}   = [];
    $self->{taglengths}      = {};
    $self->{dir}             = '';

    # Strip off trailing '/' from $dir
    $dir =~ s/\/$//;
    $self->{dir} = $dir;

    $self->_find_run_name;
    $self->_extract_barcodes;
    $self->_find_primaries;
    $self->_find_csfastas;
    $self->_find_tags;
    $self->_find_tag_lengths;
    $self->_map_primaries_to_tags;
    $self->_call_run_type;
}


sub report {
    my $self = shift;

    my $text = '';

    $text .= 'Directory parsed:   ' . $self->{dir} . "\n";
    $text .= 'Run name:           ' . $self->{run_name} . "\n";
    $text .= 'Predicted run type: ' . $self->{run_type} . "\n";
    $text .= 'Barcodes found:     ' .
             join(',',sort(keys(%{$self->{barcodes}}))) . "\n";
    $text .= 'Tags found:         ' .
             join(',',sort(keys(%{$self->{tags}}))) . "\n";
    $text .= 'Tag lengths:        ';
	foreach my $tag (keys %{$self->{taglengths}}) {
		$text .= join ",", $tag,$self->{taglengths}->{$tag};
		$text .= " ";
	}
	$text .= "\n\n";

    $text .= "Primary-to-tag mappings:\n";
    foreach my $primary (sort(keys(%{$self->{map_primary_to_tag}}))) {
        foreach my $tag (sort(keys(%{$self->{map_primary_to_tag}->{$primary}}))) {
            $text .= "  $primary  ->  $tag  (evidence from " .
                     $self->{map_primary_to_tag}->{$primary}->{$tag} .
                     " csfasta files)\n";
        }
    }

    $text .= "\nCSFASTA files:\n";
    $text .= join( "\t", qw( Primary_Dir Barcode Path CSFASTA_size
                             CSFASTA_filename Qual_size Qual_filename ) )."\n";
    foreach my $key (sort(keys(%{$self->{parsed_files}}))) {
        my $rh_file = $self->{parsed_files}->{ $key };
        $text .= join( "\t", 
                       $rh_file->{primary_dir},
                       $rh_file->{barcode},
                       $rh_file->{path},
                       $self->size_as_string( $rh_file->{csfasta_size} ),
                       $rh_file->{csfasta_name},
                       $self->size_as_string( $rh_file->{qual_size} ),
                       $rh_file->{qual_name},
                      ) . "\n";
    }

    return $text;
}


sub _finder {
    my $self = shift;
    return $self->{finder};
}


sub _find_run_name {
    my $self = shift;

    # The assumption is that the run_name is the last subdir in $dir
    my $run_name = $self->{dir};
    # Strip off leading path elements
    $run_name =~ s/.*\///g;

    print "Run name appears to be: $run_name\n" if $self->verbose;

    $self->{run_name} = $run_name;
}


sub _extract_barcodes {
    my $self = shift;

    my $dir = $self->{dir};
    my $pattern = $self->{barcode_pattern};
    my @dirs = $self->_finder->find_directory( $dir, $pattern );
    $self->{barcode_dirs} = \@dirs;
    $self->{barcodes} = undef;
    foreach my $found_dir (@dirs) {
        if (my $bc = $self->_extract_barcode( $found_dir )) {
            $self->{barcodes}->{ $bc }++;
        }
    }

    print 'Barcodes found (' .
          join(',',sort(keys(%{$self->{barcodes}}))) . ")\n"
        if ($self->barcode_count and $self->verbose);
}

sub _extract_barcode {
    my $self   = shift;
    my $string = shift;

    my $pattern = $self->{barcode_pattern};
    if ($string =~ /.*\/($pattern)$/) {
        return $1;
    }
    else {
        return undef;
    }
}


sub _find_primaries {
    my $self = shift;

    my $dir = $self->{dir};
    my $pattern = $self->{primary_pattern};
    my @dirs = $self->_finder->find_directory( $dir, $pattern );
    $self->{primary_dirs} = \@dirs;
    $self->{primaries} = undef;
    foreach my $found_dir (@dirs) {
        $found_dir =~ /.*\/($pattern)$/;
        $self->{primaries}->{ $1 }++;
    }
}


sub _find_csfastas {
    my $self = shift;

    my $dir = $self->{dir};
    my $pattern = $self->{csfasta_pattern};
    my @pathnames = sort $self->_finder->find_file( $dir, $pattern );
    $self->{csfasta_files} = \@pathnames;

    # Once we have the CSFASTA files, everything else we want to know is
    # somewhere in the file pathname so the next step is regex-mania:

    foreach my $pathname (@pathnames) {
        # Initialising all of the key/value pairs here saves on
        # "uninitialised value' errors everywhere else.
        my $rh_file =  { primary_dir  => '',
                         barcode      => '',
                         path         => '',
                         csfasta_size => 0,
                         csfasta_name => '',
                         qual_size    => 0,
                         qual_name    => '' };

        my $path         = $pathname;
        my $csfasta_name = $pathname;

        # Strip off trailing file name to get path
        $path =~ s/\/[^\/]*$//g;
        $path =~ s/^ $self->{dir}\/ //x;
        $rh_file->{ path } = $path;

        # Strip off leading path elements to get filename
        $csfasta_name =~ s/.*\///g;
        $rh_file->{ csfasta_name } = $csfasta_name;

        # To get file size we're going to need stat()
        $rh_file->{ csfasta_size } = (stat( $self->{dir} .'/'. $path .'/'.
                                            $csfasta_name))[7];

        # Look for a barcode
        if ($pathname =~ /.*\/( $self->{barcode_pattern} )\//x) {
            $rh_file->{ barcode } = $1;
        }

        # Look for a primary directory
        if ($pathname =~ /.*\/( $self->{primary_pattern} )\//x) {
            $rh_file->{ primary_dir } = $1;
        }

        # Now try to find the matching QUAL file
        my $qual_name = $csfasta_name;
        if ($rh_file->{barcode}) {
            my $cs_pattern = $rh_file->{barcode} . '.csfasta';
            my $qu_pattern = 'QV_' . $rh_file->{barcode} . '.qual';
            $qual_name =~ s/$cs_pattern/$qu_pattern/;
            $rh_file->{ qual_name } = $qual_name;
            $rh_file->{ qual_size } = (stat($self->{dir} .'/'. $path .'/'.
                                            $qual_name))[7];
        }

        # Store away our CSFASTA file info hash (requires unique filename)
        $self->_add_csfasta_file( $rh_file );
    }

}


sub _add_csfasta_file {
    my $self = shift;
    my $rh_file = shift;

    # Check whether we already have a record with this primary_dir and
    # CSFASTA name in which case we die because we should never see this.

    my $unique = $rh_file->{primary_dir} .'__'.
                 $rh_file->{csfasta_name};

    if (exists $self->{parsed_files}->{ $unique } and
        defined $self->{parsed_files}->{ $unique } ) {
        #print Dumper $self->{parsed_files}->{ $unique };
        confess 'We have already seen a CSFASTA file called ',
                $rh_file->{csfasta_name},
                ' in primary directory ',
                $rh_file->{primary_dir};
    }
    else {
        $self->{parsed_files}->{ $unique } = $rh_file;
    }
}

sub _find_tag_lengths {
	my $self = shift;

    my @files = @{ $self->{csfasta_files} };

    # The tag type pattern will be different if barcodes are present
    my $pattern = $self->barcode_count ?
                  $self->{tag_type_with_bc_pattern} :
                  $self->{tag_type_no_bc_pattern};

    print "Finding tag lengths from csfasta files\n";
    print "Using tag pattern: $pattern\n";
    $self->{taglengths} = undef;
    foreach my $file (@files) {
        if ($file =~ /$pattern/) {
		my $tag = $1;

		next if($self->{taglengths}->{ $tag });

		my $cshead = `head $file`;
		$cshead =~ />.+?\n(.+?)\n/s;
		my $len = length($1) - 1;
            	$self->{taglengths}->{ $tag } = $len;
        }
        else {
            warn "warning: tag type parsing failed for CSFASTA file [$file]\n";
        }
    }

	if($self->verbose) {
		local($,) = ",";
		print 'Tag lengths ';
		print %{$self->{'taglengths'}};
		print "\n\n";
	}
}


sub _find_tags {
    my $self = shift;

    my @files = @{ $self->{csfasta_files} };

    # The tag type pattern will be different if barcodes are present
    my $pattern = $self->barcode_count ?
                  $self->{tag_type_with_bc_pattern} :
                  $self->{tag_type_no_bc_pattern};

    print "Using tag pattern: $pattern\n";
    $self->{tags} = undef;
    foreach my $file (@files) {
        if ($file =~ /$pattern/) {
            $self->{tags}->{ $1 }++;
        }
        else {
            warn "warning: tag type parsing failed for CSFASTA file [$file]\n";
        }
    }

    print 'Tags found (' . join(',',sort(keys(%{$self->{tags}}))) . ")\n\n"
        if ($self->verbose);
}


sub _map_primaries_to_tags {
    my $self = shift;

    # The easiest way to do this is to take each CSFASTA file and
    # do pattern matches to look for the primaries and tags

    my $finder  = QCMG::FileDir::Finder->new();

    my %maps = ();
    foreach my $csfasta (@{ $self->{csfasta_files} }) {
        foreach my $primary (sort keys %{ $self->{primaries} }) {
            foreach my $tag (sort keys %{ $self->{tags} }) {
                # The pattern match is a bit tricky
                if ( ($csfasta =~ /_$tag[\._]/) and
                     ($csfasta =~ /\/$primary\//) ) {
                     $maps{ $primary }->{ $tag }++;
                }
            }
        }
    }

    $self->{map_primary_to_tag} = \%maps;
}


sub _call_run_type {
    my $self = shift;

    # If this was a barcode run then we should have found dirs matching
    # the barcode pattern and we should have found a BC tag.  If we
    # found either of these by themselves then we are in trouble.

    $self->{run_type} = '';

    if ($self->barcode_count and exists $self->{tags}->{'BC'}) {
        if (exists $self->{tags}->{'F3'} and exists $self->{tags}->{'F5-BC'}) {
            $self->{run_type} = 'FragPEBC';
        }
        elsif (exists $self->{tags}->{'F3'}) {
            $self->{run_type} = 'FragBC';
        }
        else {
            warn "Looks like a barcode run but we couldn't decipher the tags";
        }
    }
    elsif ($self->barcode_count) {
       warn "We found mixed evidence for barcodes - barcode dirs but no BC tag"
    }
    elsif (exists $self->{tags}->{'BC'}) {
       warn "We found mixed evidence for barcodes - BC tag but no barcode dirs"
    }
    else {
        if (exists $self->{tags}->{'F3'} and exists $self->{tags}->{'F5-P2'}) {
            $self->{run_type} = 'FragPE';
        }
        if (exists $self->{tags}->{'F3'} and exists $self->{tags}->{'R3'}) {
            $self->{run_type} = 'LMP';
        }
        elsif (exists $self->{tags}->{'F3'}) {
            $self->{run_type} = 'Frag';
        }
        else {
            my @tags = keys %{ $self->{tags} };
            warn 'Looks like a non-barcode run but we couldn\'t decipher ',
                'the tags [ ', join(',',@tags), " ]\n";
        }
    }
}


sub size_as_string {
    my $self = shift;
    my $size = shift || 0;
    my $stmp = ( $size > 1024**3 ) ? sprintf( "%6.1f", $size / 1024**3).'G' :
               ( $size > 1024**2 ) ? sprintf( "%6.1f", $size / 1024**2).'M' :
               ( $size > 1024**1 ) ? sprintf( "%6.1f", $size / 1024**1).'K' :
                                     sprintf( "%6.0f",   $size ).'B' ;
    return $stmp;
}




1;
__END__


=head1 NAME

QCMG::FileDir::SeqRawParser - Perl module for parsing seq_raw directories


=head1 SYNOPSIS

 use QCMG::FileDir::SeqRawParser;


=head1 DESCRIPTION

This module pulls apart a seq_raw directory to determine key aspects
of the run including the barcodes (if any), the primary directories
and the run type (LMP, PE, Frag, etc)


=head1 PUBLIC METHODS

=over

=item B<new()>

 $raw = QCMG::FileDir::SeqRawParser->new( verbose => 0 );

There is currently only one valid parameter that can be supplied to the
constructor - verbose.

=item B<process()>

 $raw->process( '/panfs/seq_raw/S0449_20110204_2_LMP' );

This method triggers the parsing of a seq_raw directory.

=item B<barcodes()>

 @barcodes = $raw->process;

Returns a list of barcodes found.

=item B<barcode_count()>

 $bc_count = $raw->barcode_count;

Returns a list of barcodes found.

=item B<verbose()>

 $blm->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: SeqRawParser.pm 4662 2014-07-23 12:39:59Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013-2014

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
