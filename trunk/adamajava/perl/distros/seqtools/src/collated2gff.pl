#!/usr/bin/perl

##############################################################################
#
#  Program:  collated2gff.pl
#  Author:   John V Pearson
#  Created:  2010-02-02
#
#  Convert collated alignment output from RNA-Mate into AB GFF format.
#
#  $Id: collated2gff.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use IO::Zlib;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;

use vars qw( $CVSID $REVISION $VERBOSE $VERSION );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $CVSID ) = '$Id: collated2gff.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;



###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my @infiles         = ();
    my $qualfile        = '';
    my $outfile         = '';
    my $buffer          = 100000;
       $VERBOSE         = 0;
       $VERSION         = 0;
    my $help            = 0;
    my $man             = 0;

    my $cmdline = $0 .' '. join(' ', @ARGV);
    print $cmdline;

    # If no params then print usage
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infile=s'           => \@infiles,        # -i
           'q|qualfile=s'         => \$qualfile,       # -q
           'o|outfile=s'          => \$outfile,        # -o
           'b|buffer=s'           => \$buffer,         # -b
           'v|verbose+'           => \$VERBOSE,        # -v
           'version!'             => \$VERSION,        #
           'h|help|?'             => \$help,           # -?
           'man|m'                => \$man             # -m
           );

    # Handle calls for help, man, version
    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;
    if ($VERSION) { print "$CVSID\n"; exit }

    # Allow for ,-separated lists of infiles
    @infiles = map { split /\,/,$_ } @infiles;

    # Input file is compulsory as is quality file
    die "No input file specified" unless $infiles[0];
    die "No quality file specified" unless $qualfile;

    # Build output filename based on first input filename if no outfile supplied
    $outfile = build_outfile_from_infile( $infiles[0] ) unless $outfile;

    print "\ncollated2gff.pl  v$REVISION  [" . localtime() . "]\n",
          '   infile(s)     ', join("\n".' 'x17, @infiles), "\n",
          "   qualfile      $qualfile\n",
          "   outfile       $outfile\n",
          "   buffer        $buffer\n",
          "   verbose       $VERBOSE\n\n" if $VERBOSE;

    my $clr = CollatedFiles->new( filenames => \@infiles,
                                  verbose   => $VERBOSE );

    my $qvr = QualityFile->new( filename  => $qualfile,
                                verbose   => $VERBOSE );

    my $gfw = GffWriter->new( filename  => $outfile,
                              p_name    => 'collated2gff.pl',
                              p_cmdline => $cmdline,
                              p_version => $REVISION,
                              verbose   => $VERBOSE );

    process_files( $clr, $qvr, $gfw, $buffer );

    print "collated2gff.pl - complete: " . localtime() . "\n" if $VERBOSE;
}


sub process_files {
    my $clr    = shift;
    my $qvr    = shift;
    my $gfw    = shift;
    my $buffer = shift;

    my $ufh = undef;
    if ($PARAMS{'unpaired'}) {
        $ufh = IO::File->new( $PARAMS{unpaired}, 'w' );
        die 'Unable to open unpaied file', $PARAMS{unpaired}, "for writing: $!"
            unless defined $ufh;
    }

    # The process for each iteration of the while loop is:
    # 1. Read the current GFF, convert to SAM and add to the buffer
    # 2. Check to see if buffer already holds the mate pair.
    #    - if yes then modify both records and remove ID from
    #      unpaired hash
    #    - if no then add ID to unpaired hash
    # 3. Write out record from the bottom of buffer if beyond buffer
    #    size limit
    #    - remove ID from unpaired hash

    # To make this whole thing work, both of these data structures are
    # going to hold references to the actual scalar strings that
    # comprise a sam record.  Modifying these records in place to add
    # mate pair info without disturbing the refs is going to take some
    # pretty careful coding.

    my @buffer   = ();
    my %unpaired = ();
    my $first_rec = 1;

    while (my $sam = $gfr->next_record_as_sam) {

        # Reading the first record triggers processing of the GFF
        # headers so we are now ready to write the SAM headers
        if ($first_rec) {
            # Decide on sort order
            if ($gfr->header( 'line-order' ) eq 'fragment') {
                $swr->sort_order( 'coordinate' );
            }
            $swr->write( $swr->header . 
                         $gfr->contigs_as_sam_sq .
                         $gfr->history_as_sam_pg );
            $first_rec = 0;
        }

        if (exists $unpaired{ $sam->qname }) {
            # If mate is in buffer then modify both records to hold mate info
            # Getting hold of previous match is a tricky exercise in refs.

            # Get ref to mate object
            my $mate_sam = $unpaired{ $sam->qname };

            # Trade match locations
            $sam->mpos( $mate_sam->pos );
            $sam->mrnm( $mate_sam->rname );
            $mate_sam->mpos( $sam->pos );
            $mate_sam->mrnm( $sam->rname );

            # If aligned to same sequence then match_rname is '='
            if ($mate_sam->rname eq $sam->rname) {
                $mate_sam->mrnm( '=' );
                $sam->mrnm( '=' );
            }

            # Calculate insert size (distance between mates + read length)
            my $isize = $sam->pos - $mate_sam->pos +
                        $gfr->_headers->{'max-read-length'};
            $sam->isize( $isize );
            $mate_sam->isize( $isize );

#            print join(' ', 'Found mate pair:',
#                            $mate_sam->qname, $mate_sam->pos, $mate_sam->flag,
#                            $sam->qname, $sam->pos, $sam->flag ), "\n";

            # We need some clarification on "first" and "second" read in
            # a pair (0x40 and 0x80) - are these the first  and second
            # seen in the alignment file or F3/R3 ?

            # We need to adjust the flag fields for both reads

            my $flag1 = pack("S",$mate_sam->flag);
            my $flag2 = pack("S",$sam->flag);
            vec( $flag1, 5, 1 ) = vec( $flag2, 4, 1 );
            vec( $flag2, 5, 1 ) = vec( $flag1, 4, 1 );
            vec( $flag1, 1, 1 ) = 1;   # mate is in proper pair
            vec( $flag2, 1, 1 ) = 1;   # read is in proper pair
            vec( $flag1, 6, 1 ) = 1;   # mate is first in pair
            vec( $flag1, 7, 1 ) = 0;   # mate is not second in pair
            vec( $flag2, 6, 1 ) = 0;   # read is not first in pair
            vec( $flag2, 7, 1 ) = 1;   # read is second in pair
            $mate_sam->flag( unpack("S",$flag1) );
            $sam->flag( unpack("S",$flag2) );

#            print join(' ', '                ',
#                            $mate_sam->qname, $mate_sam->pos, $mate_sam->flag,
#                            $sam->qname, $sam->pos, $sam->flag ), "\n";


            # This mate-pair is no longer unpaired
            delete $unpaired{ $sam->qname };
        }
        else {
            # Register new record as currently unpaired
            $unpaired{ $sam->qname } = $sam;
        }
        
        # Save current (possibly modified) record to buffer
        push @buffer, $sam;

        # Write out bottom record from buffer if buffer is full
        if (scalar @buffer > $buffer) {
            my $sam_out = shift @buffer;
            
            # Work out ID of this rec and drop from unpaired hash
            if (exists $unpaired{ $sam_out->qname }) {
                delete $unpaired{ $sam_out->qname };
                print $ufh $sam_out->as_text if (defined $ufh);
            }
            
            $swr->write( $sam_out->as_text );
        }
    }

    # Write out remainder of buffer
    while (@buffer) {
        my $sam_out = shift @buffer;
        $swr->write( $sam_out->as_text );
        if (defined $ufh and exists $unpaired{ $sam_out->qname }) {
            print $ufh $sam_out->as_text;
        }
    }
}


sub build_outfile_from_qualfile {
    my $infile = shift;  # input filename

    # Does the filename contain an extension.
    if ($infile =~ m/([[:alnum:]]+)\.([[:alnum:]]+)$/) {
        my $name = $1;
        my $extension = $2;

        # An extension of '.qual' would be replaced, others are left alone
        if ($extension =~ /qual/i) {
            return $name . '.' . 'gff';
        }

    }

    # In all other cases, simply append '.gff' to input filename
    return $infile . '.' . 'gff';
}



###########################################################################
#                          CONVENIENCE CLASSES                            #
###########################################################################

###########################################################################
#
#  Module:   GffReader
#  Creator:  John V Pearson
#  Created:  2010-01-26
#
#  Reads GFF v2 files output by AB SOLiD pipeline.
#
###########################################################################


package GffReader;

use Data::Dumper;

sub new {
    my $class = shift;
    my %params = @_;

    die "GffReader:new() requires a filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => $params{filename},
                 version         => '2',
                 headers         => {},
                 valid_headers   => {},
                 record_ctr      => 0,
                 matepair        => ($params{matepair} ?
                                     $params{matepair} : 0),
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # GFF headers specified by AB come in 2 flavours: unique and multiple.
    # We also force all header tags to be lc to avoid complications.

    my @valid_unique_headers   = qw( solid-gff-version gff-version
                                     source-version date time type
                                     color-code primer-base
                                     max-num-mismatches max-read-length
                                     line-order hdr );
    my @valid_multiple_headers = qw( history contig );
    $self->{valid_headers} = {}; 
    $self->{valid_headers}->{ $_ } = 1 foreach @valid_unique_headers;
    $self->{valid_headers}->{ $_ } = 2 foreach @valid_multiple_headers;

    # If there a zipname, we use it in preference to filename.  We only
    # process one so if both are specified, the zipname wins.

    if ( $params{zipname} ) {
        my $fh = IO::Zlib->new( $params{zipname}, 'r' );
        die 'Unable to open ', $params{zipname}, "for reading: $!"
            unless defined $fh;
        $self->filename( $params{zipname} );
        $self->filehandle( $fh );
    }
    elsif ( $params{filename} ) {
        my $fh = IO::File->new( $params{filename}, 'r' );
        die 'Unable to open ', $params{filename}, "for reading: $!"
            unless defined $fh;
        $self->filename( $params{filename} );
        $self->filehandle( $fh );
    }

    return $self;
}


sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}

sub filehandle {
    my $self = shift;
    return $self->{filehandle} = shift if @_;
    return $self->{filehandle};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub matepair {
    my $self = shift;
    return $self->{matepair};
}

sub cmdline {
    my $self = shift;
    return $self->{cmdline};
}


sub _valid_headers {
    my $self = shift;
    return $self->{valid_headers};
}

sub _headers {
    my $self = shift;
    return $self->{headers};
}

sub header {
    my $self   = shift;
    my $attrib = shift;

    if (exists  $self->{headers}->{$attrib} and
        defined $self->{headers}->{$attrib}) {
        return $self->{headers}->{$attrib};
    }
    else {
        return undef;
    }
}


sub _incr_record_ctr {
    my $self = shift;
    return $self->{record_ctr}++;
}


sub next_record {
    my $self = shift;

    # Read lines, checking for and processing any headers
    # and only return once we have a record

    while (1) {
        my $line = $self->filehandle->getline();
        # Catch EOF
        return undef if (! defined $line);
        if ($line =~ /^##/) {
            $self->process_gff2_header_line( $line );
        }
        else {
            if ($self->verbose) {
                # Print progress messages for every 1M records
                $self->_incr_record_ctr;
                print( $self->{record_ctr}, ' GFF records processed: ',
                       localtime().'', "\n" )
                    if $self->{record_ctr} % 100000 == 0;
            }
            my $gff = Gff->new( $line );
            $gff->matepair( $self->matepair );
            print $gff->debug if ($self->verbose > 1);
            return $gff;
        }
    }
}


sub process_gff2_header_line {
    my $self = shift;
    my $line = shift;

    chomp $line;
    $line =~ s/^##//;

    my ($tag, $value) = split ' ', $line, 2;
    $tag = lc( $tag );

    die "Unknown GFF2 header [$tag]"
        unless (exists $self->_valid_headers->{$tag} and
                defined $self->_valid_headers->{$tag} );

    # Headers can be classed as unique or multiple
    if ($self->_valid_headers->{$tag} == 1) {
        $self->{headers}->{$tag} = $value;
    }
    if ($self->_valid_headers->{$tag} == 2) {
        push @{ $self->{headers}->{$tag} }, $value;
    }
}


sub history_as_sam_pg {
    my $self = shift;

    my $pg ='';
    
    # Return empty string if no history present
    return $pg unless ( exists $self->{headers}->{history} and
                        scalar( @{ $self->{headers}->{history} } ) > 0 );

    foreach (@{ $self->{headers}->{history} }) {
       my $cmdline = $_;
       $cmdline =~ /(.*?) /;
       my $ptmp = $1;
       my @ptmps = split /\//, $ptmp;
       my $program = pop @ptmps;
       $pg .= "\@PG\t" . 'ID:' . $program . "\t" .
                       'CL:' . $cmdline . "\n";
    }

    return $pg;
}


sub contigs_as_sam_sq {
    my $self = shift;

    my $sq ='';

    # Return empty string if no contigs present
    return $sq unless ( exists $self->{headers}->{contig} and
                        scalar( @{ $self->{headers}->{contig} } ) > 0 );

    foreach (@{ $self->{headers}->{contig} }) {
       my ($id, $defline) = split /\s/, $_, 2;
       $sq .= "\@SQ\t" . 'SN:' . 'seq_' . $id . "\t" .
                         'UR:' . $defline . "\n";
    }

    return $sq;
}


sub next_record_as_sam {
    my $self = shift;

    my $gff = $self->next_record;
    return undef unless defined $gff;
    my $sam = Sam->new( gff => $gff );
    return $sam;
}



###########################################################################
#
#  Module:   SamWriter
#  Creator:  John V Pearson
#  Created:  2010-01-22
#
#  Creates v0.1.2 SAM file (1000 Genomes) to hold sequencing read
#  alignment records.
#
###########################################################################


package SamWriter;

use Data::Dumper;

use Data::Dumper;

sub new {
    my $class = shift;
    my %params = @_;

    die "SamWriter:new() requires a filename parameter" 
        unless (exists $params{filename} and defined $params{filename});

    my $self = { filename        => $params{filename},
                 sam_version     => '0.1.2',
                 sort_order      => 'unsorted',
                 group_order     => 'none',
                 p_name          => ($params{p_name} ?
                                     $params{p_name} : ''),
                 p_cmdline       => ($params{p_cmdline} ?
                                     $params{p_cmdline} : ''),
                 p_version       => ($params{p_version} ?
                                     $params{p_version} : 0),
                 description     => ($params{description} ?
                                     $params{description} : 0),
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # Open file and make sure it is writable
    my $fh = IO::File->new( $params{filename}, 'w' );
    die 'Unable to open ', $params{filename}, "for writing: $!"
        unless defined $fh;

    $self->filehandle( $fh );
    return $self;
}


sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}

sub filehandle {
    my $self = shift;
    return $self->{filehandle} = shift if @_;
    return $self->{filehandle};
}

sub sam_version {
    my $self = shift;
    return $self->{sam_version};
}

sub p_name {
    my $self = shift;
    return $self->{p_name};
}

sub p_cmdline {
    my $self = shift;
    return $self->{p_cmdline};
}

sub p_version {
    my $self = shift;
    return $self->{p_version};
}

sub group_order {
    my $self = shift;
    return $self->{group_order} = shift if @_;
    return $self->{group_order};
}


sub sort_order {
    my $self = shift;

    if (@_) {
        my $order = lc( shift );
        if ($order =~ /unsorted/i or
            $order =~ /queryname/i or
            $order =~ /coordinate/i ) {
            $self->{sort_order} = lc( $order );
        }
        else {
            die "Invalid value [$order] supplied to sort_order(). ",
                "Valid values are: unsorted, queryname, coordinate\n";
        }
    }

    return $self->{sort_order};
}


sub header {
    my $self = shift;

    my $header = '@HD' ."\t" . 'VN:' . $self->sam_version . "\t" .
                               'SO:' . $self->sort_order . "\t" .
                               'GO:' . $self->group_order . "\n";
    $header .=  '@PG' . "\t" . $self->program . "\n";

    return $header;
}


sub program {
    my $self = shift;

    my $program = 'ID:' . $self->p_name . "\t" .
                  'VN:' . $self->p_version  . "\t" .
                  'CL:' . $self->p_cmdline;
    return $program;
}


sub write {
    my $self = shift;
    my $line = shift;

    $self->filehandle->print( $line );
}


###########################################################################
#
#  Module:   Gff
#  Creator:  John V Pearson
#  Created:  2010-02-12
#
#  Data container for a GFF v2 record.
#
###########################################################################


package Gff;

use Data::Dumper;
use Memoize;

sub new {
    my $class = shift;
    my $line  = shift;
    
    chomp $line;
    my @fields = split "\t", $line;
    warn 'Saw ', scalar(@fields), " fields, should have been 9 [$line]\n"
        if (scalar(@fields) < 9);

    # Parse out attributes field because we are going to need this info
    my @attributes = split(';',$fields[8]);
    my %attributes = ();
    foreach (@attributes) {
        my ($tag,$value) = split '=', $_;
        $attributes{ $tag } = $value; 
    }

    my $self = { seqname  => $fields[0],
                 source   => $fields[1],
                 feature  => $fields[2],
                 start    => $fields[3],
                 end      => $fields[4],
                 score    => $fields[5],
                 strand   => $fields[6],
                 frame    => $fields[7],   
                 attribs  => \%attributes,
                 matepair => 0 };

    bless $self, $class;
}


sub seqname {
    my $self = shift;
    return $self->{seqname};
}

sub source {
    my $self = shift;
    return $self->{source};
}

sub feature {
    my $self = shift;
    return $self->{feature};
}

sub start {
    my $self = shift;
    return $self->{start};
}

sub end {
    my $self = shift;
    return $self->{end};
}

sub score {
    my $self = shift;
    return $self->{score};
}

sub strand {
    my $self = shift;
    return $self->{strand};
}

sub frame {
    my $self = shift;
    return $self->{frame};
}

sub attrib {
    my $self = shift;
    my $type = shift;
    return $self->{attribs}->{$type} = shift if @_;
    return $self->{attribs}->{$type};
}

sub matepair {
    my $self = shift;
    return $self->{matepair} = shift if @_;
    return $self->{matepair};
}


# This routine is not OO and it's being memoized for extra speed
sub _cb2b {
    my $base = uc( shift );
    my $col  = shift;
    my %transform = ( A => { 0 => 'A', 1 => 'C', 2 => 'G', 3 => 'T' },
                      C => { 1 => 'A', 0 => 'C', 3 => 'G', 2 => 'T' },
                      G => { 2 => 'A', 3 => 'C', 0 => 'G', 1 => 'T' },
                      T => { 3 => 'A', 2 => 'C', 1 => 'G', 0 => 'T' } );

    return $transform{$base}->{$col};
}
memoize('_cb2b');

# This routine is not OO and it's being memoized for extra speed
sub _bb2c {
    my $base1 = uc( shift );
    my $base2 = uc( shift );
    my %transform = ( A => { A => '0', C => '1', G => '2', T => '3' },
                      C => { A => '1', C => '0', G => '3', T => '2' },
                      G => { A => '2', C => '3', G => '0', T => '1' },
                      T => { A => '3', C => '2', G => '1', T => '0' } );

    return $transform{$base1}->{$base2};
}
memoize('_bb2c');


sub c2b_seq {
    my $self = shift;
    my $cstr = shift;

    return undef unless defined $cstr and $cstr;

    my @colors = split //, $cstr;
    my $base   = shift @colors;
    my $bases  = '';
    foreach my $color (@colors) {
        $bases .= $base;
        $base = _cb2b( $base, $color );
    }
    $bases .= $base;

    return $bases;
}


sub revcomp {
    my $self = shift;
    my $seq = shift;
    my $revc = join('', reverse( split(//,$seq) ));
    $revc =~ tr/ACGTacgt/TGCAtgca/;
    return $revc;
}


sub color_original {
    my $self = shift;
    return $self->attrib('g');
}


sub color_original_to_base {
    my $self = shift;
    return $self->c2b_seq( $self->color_original );
}


sub color_fixed {
    my $self = shift;

    return $self->attrib('g') unless $self->attrib('r');

    my @cols = split //, $self->attrib('g');
    my @fixes = split /\,/, $self->attrib('r');
    foreach my $fix (@fixes) {
        my ($loc,$col) = split /_/, $fix;
        $cols[$loc-1] = $col;
    }
    return join('',@cols);
}


sub color_fixed_to_base {
    my $self = shift;
    return $self->c2b_seq( $self->color_fixed );
}


sub _min {
    my @vals = @_;
    my $min = $vals[0];
    foreach my $val (@vals) {
        next unless defined $val;  # beyond limits of color quality array
        $min = $val if $val < $min; 
    }
    return $min;
}

sub _max {
    my @vals = @_;
    my $max = $vals[0];
    foreach my $val (@vals) {
        next unless defined $val;  # beyond limits of color quality array
        $max = $val if $val > $max; 
    }
    return $max;
}


sub color_quality_to_base_quality {
    my $self = shift;

    # This is a pain-in-the-ass.  We need to take the data from GFF
    # attibutes "g", "s" and "q" and apply the rules outlined in the
    # SOLiD_QC_to_BC.pdf document in order to calculate base qualities.

    my @colors = split //,   $self->attrib('g');
    my @qc     = split /\,/, $self->attrib('q');
    my @tmps   = ();
    if (defined $self->attrib('s')) {
        @tmps  = split /\,/, $self->attrib('s');
    }

    my @s      = map { ' ' } @colors;  # initialize to all blanks
    foreach my $tmp (@tmps) {
       my $type = substr( $tmp, 0 ,1 );
       my $locn = substr( $tmp, 1 );
       $s[ $locn ] = $type;
    }

    # First thing we need to do is recover the first color which
    # annoyingly is not supplied in the "g" attribute.  The primer base
    # and the first color are used to reduce the first color to a base
    # but luckily the full list of color qualities is given in "q" so we
    # just have to work out whether the read is F3 or R3 and based on
    # the primer base (T/G) we can work out what the color must have
    # been to give us the base shown at the start of "g".

    my $col1 = shift @colors;
    my $pcol = ( $self->seqname =~ /F3$/ ) ? 'T' : 'G';
    unshift @colors, _bb2c( $pcol, $col1 );

    # Apply the rules from the SOLiD_QC_to_BC.pdf document.

    # Default case: set base qualities assuming everything is normal.
    # Note that this formula requires the i+1 color quality for each
    # base quality which obviously does not exist for the final base so
    # to avoid warnings we need to treat the last base as a sepcial case.
    my @qtmp = ();
    foreach my $i (0..($#colors-1)) {
        $qtmp[ $i ] = $qc[ $i ] + $qc[ $i+1 ];
    }
    $qtmp[ $#colors ] = $qc[ $#colors ];

    my @qb = @qtmp;
    {
        # This next loop has almost limitless ways to reach beyond the
        # limits of the actual array of colors so in almost every case
        # it throws enough warnings to drown a fish.  I have no idea how
        # to modify the ABI rules in cases where the rules as stated
        # reach beyond the bounds of the actual colors so I will do the
        # dumbest but most pragmatic thing - disable warnings! 

        no warnings;

        foreach my $i (0..$#colors) {
       
            # Case 1: (grAy) isolated mismatch (sequencing error) 
            if ($s[$i] eq 'a') {
               $qb[ $i-1 ] = _min( $qc[ $i-1 ], $qc[ $i ] );
               $qb[ $i ]   = _min( $qc[ $i ], $qc[ $i+1 ] );
            }

            # Case 2: (g: Green) valid adjacent mismatch (one base change)
            elsif ($s[$i] eq 'g' and $s[$i+1] eq 'g') {
               $qb[ $i-1 ] = $qc[ $i-1 ] + $qc[ $i ];
               $qb[ $i ]   = $qc[ $i ]   + $qc[ $i+1 ];
               $qb[ $i+1 ] = $qc[ $i+1 ] + $qc[ $i+2 ];
            }

            # Case 3: (y: Yellow) call consistent with isolated two-base change
            elsif ($s[$i-1] eq 'y' and $s[$i] eq 'y' and $s[$i+1] eq 'y') {
               $qb[ $i-2 ] = _min( $qc[ $i-2 ], $qc[ $i-1 ], $qc[ $i ] );
               $qb[ $i-1 ] = $qb[ $i-2 ];
               $qb[ $i ]   = _min( $qc[ $i-1 ], $qc[ $i ], $qc[ $i+1 ] );
               $qb[ $i+1 ] = $qb[ $i ];
            }

            # Case 4: (r: Red)
            # color call consistent with isolated three-base change
            elsif ($s[$i-1] eq 'r' and $s[$i] eq 'r' and
                   $s[$i+1] eq 'r' and $s[$i+2] eq 'r' ) {
               $qb[ $i-1 ] = _min( $qc[ $i-2 ], $qc[ $i-1 ], $qc[ $i ],
                                   $qb[ $i+1 ], $qc[ $i+2 ], $qc[ $i+3 ] );
               $qb[ $i ]   = $qb[ $i-1 ];
               $qb[ $i+1 ] = $qb[ $i-1 ];
               $qb[ $i+2 ] = $qb[ $i-1 ];
               $qb[ $i+3 ] = $qb[ $i-1 ];
            }

            # Case 5: (b = Blue) invalid adjacent mismatch (other mismatches)
            elsif ($s[$i] eq 'b') {
               $qb[ $i-1 ] = 1;
               $qb[ $i ]   = 1;
            }
        }

    }  # warnings should be on again after this scope is closed out
    
    # Add 33 to all qualities and ASCII-ize (ceiling at 64)
    my @final_quals = map { ($_ > 30) ? '@' : chr(33+$_) } @qb;

#    print '  colors: ', join( "   ", @colors ), "\n",
#          '  qc:     ', join( "  ",  @qc ), "\n",
#          '  s:      ', join( "   ", @s ), "\n",
#          '  qtmp:   ', join( "  ",  @qtmp ), "\n",
#          '  qb:     ', join( "  ",  @qb ), "\n",
#          '  ascii:  ', join( "   ", @final_quals ), "\n\n";

    return join('',@final_quals);
}


sub debug {
    my $self = shift;

    my $output = $self->color_original .
                 "\toriginal color space\n" .
                 $self->color_original_to_base .
                 "\tbase space from original color\n" .
                 $self->attrib('b') .
                 "\tAB base space\n";
    if ($self->strand eq '-') {
        $output .= $self->revcomp($self->color_original_to_base) .
                   "\trevcomp base space from original color space\n";
        $output .= $self->revcomp($self->attrib('b')) .
                   "\trevcomp AB base space\n";
    }

    if ($self->attrib('r')) {
        $output .= $self->color_fixed .
                   "\tfixed color space [" . $self->attrib('r') . "]\n" .
                   $self->color_fixed_to_base .
                   "\tbase space from fixed color space\n";
        if ($self->strand eq '-') {
        $output .= $self->revcomp($self->color_fixed_to_base) .
                   "\trevcomp base space from fixed color space\n";
        }
    }

    return $self->seqname .
           "  [" . $self->attrib('i') .':'.
                   $self->start . $self->strand . "]\n" .
           $output . "\n";
}


###########################################################################
#
#  Module:   Sam
#  Creator:  John V Pearson
#  Created:  2010-02-12
#
#  Data container for a SAM v0.1.2 record.
#
###########################################################################


package Sam;

use Data::Dumper;
use Memoize;

sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { qname   => '',
                 flag    => unpack("S", pack("B*",'0'x16)),  # 16 bits zerod
                 rname   => '',
                 pos     => 0,
                 mapq    => 255,
                 cigar   => '',
                 mrnm    => '',
                 mpos    => 0,
                 isize   => 0,
                 seq     => '',
                 qual    => '',
                 tags    => {} };

    bless $self, $class;


    # If GFF record passed in, populate SAM record
    $self->from_gff( $params{gff} ) if $params{gff};

    return $self;
}


sub qname {
    my $self = shift;
    return $self->{qname} = shift if @_;
    return $self->{qname};
}

sub flag {
    my $self = shift;
    return $self->{flag} = shift if @_;
    return $self->{flag};
}

sub rname {
    my $self = shift;
    return $self->{rname} = shift if @_;
    return $self->{rname};
}

sub pos {
    my $self = shift;
    return $self->{pos} = shift if @_;
    return $self->{pos};
}

sub mapq {
    my $self = shift;
    return $self->{mapq} = shift if @_;
    return $self->{mapq};
}

sub cigar {
    my $self = shift;
    return $self->{cigar} = shift if @_;
    return $self->{cigar};
}

sub mrnm {
    my $self = shift;
    return $self->{mrnm} = shift if @_;
    return $self->{mrnm};
}

sub mpos {
    my $self = shift;
    return $self->{mpos} = shift if @_;
    return $self->{mpos};
}

sub isize {
    my $self = shift;
    return $self->{isize} = shift if @_;
    return $self->{isize};
}

sub seq {
    my $self = shift;
    return $self->{seq} = shift if @_;
    return $self->{seq};
}

sub qual {
    my $self = shift;
    return $self->{qual} = shift if @_;
    return $self->{qual};
}

sub tag {
    my $self = shift;
    my $type = shift;
    return $self->{tags}->{$type} = shift if @_;
    return $self->{tags}->{$type};
}


sub tags {
    my $self = shift;
    return $self->{tags};
}


sub tags_as_array {
    my $self = shift;
    return values %{ $self->{tags} };
}


# This routine is not OO and it's being memoized for extra speed
sub _bb2c {
    my $base1 = uc( shift );
    my $base2 = uc( shift );
    my %transform = ( A => { A => '0', C => '1', G => '2', T => '3' },
                      C => { A => '1', C => '0', G => '3', T => '2' },
                      G => { A => '2', C => '3', G => '0', T => '1' },
                      T => { A => '3', C => '2', G => '1', T => '0' } );

    return $transform{$base1}->{$base2};
}
memoize('_bb2c');


sub from_gff {
    my $self = shift;
    my $gff  = shift;

    # Process ID including removal of trailing _F3 / _R3 from IDs
    my $qname = $gff->seqname;
    $qname =~ s/_[FR]3$//;
    $self->qname( $qname );

    #print Dumper $self->flag, pack("S", $self->flag);

    # Set appropriate bits in flag
    my $flag = pack("S", $self->flag);
    vec( $flag, 0, 1 ) = 1 if ($gff->matepair);      # mate pair library
    vec( $flag, 4, 1 ) = 1 if ($gff->strand eq '-'); # match on - strand
    vec( $flag, 6, 1 ) = 1;                          # first read
    $self->flag( unpack("S",$flag) );

    #print Dumper $self->flag, unpack("S",$flag);

    # In the GFF sequences are only identified by number
    $self->rname( 'seq_' . $gff->attrib('i') );

    my $bquals = $gff->color_quality_to_base_quality;
    if ($gff->strand eq '-') {
       my @bquals = reverse split( //, $bquals );
       $bquals = join( '', @bquals );
    }
    $self->qual( $bquals );

    $self->pos( $gff->start );
    $self->cigar( length( $gff->attrib('b') ) .'M' );

    # Set sequence and work out MD descriptor as follows:
    # 1. $self->seq = AB "believed" sequence (GFF b= attrib), revcomp if
    #    match is on - strand
    # 2. To get reference, fix original color based on r= attrib,
    #    convert to base space and revcomp if match is on - strand
    # 3. Compare sequences from 1. and 2. and code delta in MD format

    if ($gff->strand eq '-') {
        $self->seq( uc( $gff->revcomp( $gff->attrib('b') ) ) );
        my $refr_seq = $gff->revcomp( $gff->color_fixed_to_base );
        $self->tag( 'MD', 'MD:Z:'. $self->MD( $self->seq, $refr_seq ) );
    }
    else {
        $self->seq( uc( $gff->attrib('b') ) );
        my $refr_seq = $gff->color_fixed_to_base;
        $self->tag( 'MD', 'MD:Z:'. $self->MD( $self->seq, $refr_seq ) );
    }

    # Add 33 to all qualities and ASCII-ize
    my @cquals = map { chr(33+$_) }
                 split(',',$gff->attrib('q'));

    $self->tag( 'AS', 'AS:i:' . int($gff->score) );
    $self->tag( 'CQ', 'CQ:Z:' . join('',@cquals) );

    # The color string needs some work because the SAM format wants the
    # primer base (T for F3, for R3) plus the full 35 colors but the gff
    # "g" attribute is the first base of the sequence plus 34 colors so
    # we need to recover the first color before converting to SAM.

    my @colors = split //, $gff->attrib('g');
    my $base1 = ( $gff->seqname =~ /F3$/ ) ? 'T' : 'G';
    my $base2 = shift @colors;
    unshift @colors, $base1, _bb2c( $base1, $base2 );
    $self->tag( 'CS', 'CS:Z:' . join('',@colors) );

    #print Dumper $gff, $self; exit;
}


sub MD {
    my $self = shift;
    my $seq  = shift;
    my $ref  = shift;

    my $md_str = '';
    my $ctr = 0;

    # Do a base-by-base compare on the read and reference sequences

    foreach my $i (0..(length($seq)-1)) {
        if (uc(substr( $seq, $i, 1 )) eq uc(substr( $ref, $i, 1 ))) {
            $ctr++;
        }
        else {
            if ($ctr > 0) {
                $md_str .= $ctr;
                $ctr = 0;
            }
            $md_str .= substr( $ref, $i, 1 );
        }
    }
    if ($ctr > 0) {
        $md_str .= $ctr;
    }

    return $md_str;
}


sub as_text {
    my $self = shift;

    return join( "\t", $self->qname,
                       $self->flag,
                       $self->rname,
                       $self->pos,
                       $self->mapq,
                       $self->cigar,
                       $self->mrnm,
                       $self->mpos,
                       $self->isize,
                       $self->seq,
                       $self->qual,
                       $self->tags_as_array )."\n";
}



###########################################################################
#
#  Module:   CollatedFile
#  Creator:  John V Pearson
#  Created:  2010-02-13
#
#  Reads a collated as created by RNA-Mate or X-Mate.  A collated file
#  is very similar to the .ma matches file created by the ABI
#  corona-lite/mapreads pipelein except that the match location is
#  listed in the defline in a slightly different way.  Both files are
#  very similar to a multiple sequence FASTA file, i.e. the matches
#  appear as multiepl sequence records where each sequence has a defline
#  followed by the sequence matched.
#
###########################################################################

package CollatedFile;

use Data::Dumper;

sub new {
    my $class = shift;
    my %params = @_;

    confess "CollatedFile->new() requires a file parameter" 
        unless (exists $params{file} and defined $params{file});

    my $self  = { file         => $params{file},
                  verbose      => ($params{verbose} ? $params{verbose} : 0),
                  _defline     => '',
                  _recctr      => 0,
                  CreationTime => localtime().'',
                  Version      => $VERSION, 
                };

    bless $self, $class;
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub file {
    my $self = shift;
    return $self->{file};
}

sub sequences {
    my $self = shift;
    return @{ $self->{sequences} };
}

sub creation_time {
    my $self = shift;
    return $self->{'CreationTime'};
}

sub creator_module_version {
    my $self = shift;
    return $self->{'Version'};
}


sub parse_file {
    my $self = shift;

    my $fh = IO::File->new( $self->file, 'r' );
    confess 'Unable to open FASTA file [', $self->file, "] for reading: $!"
        unless defined $fh;

    # Process FASTA string in case it contains multiple sequences
    print 'processing FASTA file [', $self->file, "]\n"
        if $self->verbose;

    my @seqs    = ();
    my $defline = '';
    my $seq     = '';

    while (my $line = $fh->getline) {
        chomp $line;
        $line =~ s/\s+$//;        # trim trailing spaces
        next if ($line =~ /^#/);  # skip comments
        next unless $line;        # skip blank lines

        if ($line =~ /^>/) {
            if ($defline) {
                push @seqs, Bio::TGen::Util::Sequence->new(
                                defline  => $defline,
                                sequence => $seq,
                                verbose  => $self->verbose );
            }
            $defline = $line;
            $seq     = '';
        }
        else {
            $seq .= $line;
        }
    }

}


sub parse_file {
    my $self = shift;

    my $fh = IO::File->new( $self->file, 'r' );
    confess 'Unable to open FASTA file [', $self->file, "] for reading: $!"
        unless defined $fh;

    # Process FASTA string in case it contains multiple sequences
    print 'processing FASTA file [', $self->file, "]\n"
        if $self->verbose;

    my @seqs    = ();
    my $defline = '';
    my $seq     = '';

    while (my $line = $fh->getline) {
        chomp $line;
        $line =~ s/\s+$//;        # trim trailing spaces
        next if ($line =~ /^#/);  # skip comments
        next unless $line;        # skip blank lines

        if ($line =~ /^>/) {
            if ($defline) {
                push @seqs, Bio::TGen::Util::Sequence->new(
                                defline  => $defline,
                                sequence => $seq,
                                verbose  => $self->verbose );
            }
            $defline = $line;
            $seq     = '';
        }
        else {
            $seq .= $line;
        }
    }

    # Catch last sequence
    if ($defline) {
        push @seqs, Bio::TGen::Util::Sequence->new(
                        defline  => $defline,
                        sequence => $seq,
                        verbose  => $self->verbose );
    }

    $self->{sequences} = \@seqs;
}

sub write_seqs {
    my $self = shift;
    foreach my $seq ($self->sequences) {
       $seq->write_FASTA_file;
    }
}

1;
__END__


=head1 NAME

Bio::TGen::Util::FASTA - FASTA file IO


=head1 SYNOPSIS

 use Bio::TGen::Util::FASTA;


=head1 DESCRIPTION

This module provides an interface for reading and writing FASTA files.


=head1 AUTHORS

John Pearson L<mailto:bioinfresearch@tgen.org>


=head1 VERSION

$Id: collated2gff.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

This module is copyright 2008 by The Translational Genomics Research
Institute.  All rights reserved.  This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes.  Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection
with the production, marketing, sale or support of any commercial
product or service.  For commercial use, please contact
licensing@tgen.org.  By installing this Software you are agreeing to
the terms of the LICENSE file distributed with this software.

In any work or product derived from the use of this Software, proper
attribution of the authors as the source of the software or data must
be made.  The following URL should be cited:

L<http://bioinformatics.tgen.org/brunit/>

=cut


###########################################################################


__END__

=head1 NAME

collated2gff.pl - Generate ABI GFF from RNA-Mate collated alignment files


=head1 SYNOPSIS

 collated2gff.pl [options]


=head1 ABSTRACT

Take collated-format alignment output files from RNA-Mate and mate each
aligned read with the matching quality values from the original AB qual
file and write the record out in ABI GFF-format files.

=head1 OPTIONS

 -i | --infile        filename for collated input file
 -q | --qualfile      filename for quality file
 -o | --outfile       filename for GFF output
 -b | --buffer        buffer size (records)
 -v | --verbose       print progress and diagnostic messages
      --version       print version and exit immediately
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<collated2gff.pl> was designed to support the Australian ICGC
sequencing project.  It will take 1 or more alignment files in
collated-format as output by RNA-Mate, and match each read in the
collated file(s) with the corresponding read quality string from the
original AB qual file.  The output should be in GFF format.

This script makes a number of assumptions: (1) each collated file is
sorted in read-id order; (2) the qual file contains a superset of the
reads from all the ocllated files, i.e. the expectation is that the
collated files were generated by aligning the .csfasta file that
corrsponds to the .qual file so reads that did not map or mapped more
than once will not appear in the collated file but no read that was not
in the original .csfasta file can appear in a collated file.

=head2 Input format

A collated file looks very much like a .ma file from mapreada, i.e. a
modified FASTA format where the defline carries additional information.
In this case the defline is a tab-separated list of read ID, number of
matches, and one or more map locations where each location is in the
format chromosome.location.mismatch_count.

=head2 Output format

The GFF input format read by this script is a tab-separated text file
with unix-style line endings and the following fields of which the last
two are optional:

     Fieldname      Example value
 1.  seqname        1231_644_1328_F3
 2.  source         solid
 3.  feature        read
 4.  start          97
 5.  end            121
 6.  score          13.5
 7.  strand         -
 8.  frame          .
 9.  [attributes]   b=TAGGGTTAGGGTTGGGTTAGGGTTA;
                    c=AAA;
                    g=T320010320010100103000103;
                    i=1;
                    p=1.000;
                    q=23,28,27,20,17,12,24,16,20,8,13,26,28,2
                      4,13,13,27,14,19,4,23,16,19,9,14;
                    r=20_2;
                    s=a20;
                    u=0,1
 10. [comments]

=head2 Output format

The output format is SAM v0.1.2.


=head2 Commandline Options

=over

=item B<-i | --infile>

Name of AB GFF alignment input file.
Either -i or -z must be specified or the script will exit immediately.

=item B<-z | --zipinfile>

Name of gzipped AB GFF alignment input file.
Either -i or -z must be specified or the script will exit immediately.

=item B<-o | --outfile>

Name of SAM file to write to.  If no filename is specified, the program
will construct one based on the name of the GFF input file.  If the GFF
file ends in .txt or .sam (case insensitive) then the output file is the
same as the input but with the .sam extention in place of the original
extension.  In all other cases, .sam is simply appended to the input
filename.

=item B<-u | --unpaired>

Optional SAM-format file of records that are unmated.

=item B<-b | --buffer>

Number of lines to be kept in buffer while looking for mate-pairs.
Default value is 100000.  With GFF files containing hundreds of millions
of alignments, we can't afford to load it all into memory in order to
spot mate pairs so we'll keep a buffer and if the second read from a
mate-pair is within -b alignments of the first read, then both reads
will be modified prior to being written out.

=item B<-p | --matepair>

Should be specified if the GFF file is from a mate-pair library.  This
is hard to tell from the GFF itself so the user has to specify.  If the
flag is not set, the library is assumed to be fragment.

=item B<--version>

Print the program version number and exit immediately.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.  These
messages will be printed to STDOUT.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back

=head2 Example

The following usage example will convert a gzipped GFF file to SAM.

  gff2sam.pl -v -z SUSAN_20080709_1_Baylor_2_Frag.gff.gz

B<N.B.> The spaces between the options (B<-z>, B<-o> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:jpearson@tgen.org>

=back


=head1 VERSION

$Id: collated2gff.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010

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
