package QCMG::BaseClass::Quality;

=head1 NAME

QCMG::BaseClass::Quality

=head1 SYNOPSIS

  use QCMG::BaseClass::Quality;
  my $obj = QCMG::Quality->new(%argv);
  $obj->main;

=head1 DESCRIPTION

This module check each color base intensity for each raw tag. 
It certain number of base's color intensity lower than certain value, 
we treat it as low quality tag.

The below example list all necessary arguments:
	my $objTool = QCMG::BaseClass::Tools->new('f_log' => "/data/test.log");
        my %argv = (
               'raw_tag_length' => 10,
                'tag_lengths' => 10, 8,
                'exp_name' => "test",
                'raw_csfasta' => "/data/test.csfata",
                'raw_qual' => "/data/test.qual",
                'mask' => "1111111111",
                'output_dir' => "/data/",
                'objTool' => $objTool,
        );

=head2 Methods

=over 1

=cut

use strict;
use Object::InsideOut;
use Carp;

use constant {
    LowScore => 10,    # if base intensity lower than this number, that is low quality base
    NumPoor => 4,  # if number of base's intensity lower than this number, that is a low quality tag
};

# Attr:
#<<<
my @raw_tag_length :Field :Arg('Name' => 'raw_tag_length' , 'Mandatory' => 1);
my @tag_lengths :Field :Arg('Name' => 'tag_lengths' , 'Mandatory' => 1);
my @exp_name :Field :Arg('Name' => 'exp_name', 'Mandatory' => 1);
my @raw_csfasta :Field :Arg('Name' => 'raw_csfasta', 'Mandatory' => 1);
my @raw_qual :Field :Arg('Name' => 'raw_qual', 'Mandatory' => 1);
my @mask :Field :Arg('Name' => 'mask', 'Mandatory' => 1);
my @output_dir :Field :Arg('Name' => 'output_dir', 'Mandatory' => 1);
my @objTool :Field :Arg('Name' => 'objTool', 'Mandatory' => 1) ;

my @outputs :Field :Get(outputs);
#>>>

# methods
sub _init : Init {
    my $self = shift;

    my $lTag = $tag_lengths[$$self];
    foreach my $l (@$lTag) {
        my $f = "$output_dir[$$self]$exp_name[$$self].mers$l.csfasta";
        push @{$outputs[$$self]}, $f;
    }
}

=item * $obj->main;

Manages the processes which are required to be performed when filtering for quality.
input file : eg. "/data/test.csfasta", "/data/test.qual"
output file: "/data/test.mers10.csfata" and "/data/test.mers8.csfata"

=cut

sub main {
    my $self = shift;

    $objTool[$$self]->Log_PROCESS("checking tag quality...");
    my %FHandles = ();

    # open input files
    open(my $cs, $raw_csfasta[$$self])
      or $objTool[$$self]->Log_DIED("Can't open raw tag file: $raw_csfasta[$$self]\n");
    open(my $qual, $raw_qual[$$self])
      or $objTool[$$self]->Log_DIED("Can't open quality file: $raw_qual[$$self]\n");
    $FHandles{"CSFASTA"} = $cs;
    $FHandles{"QUAL"}    = $qual;

    # create output files and store all outputs handle into an hash table
    foreach my $l (@{$tag_lengths[$$self]}) {
        my $f = "$output_dir[$$self]$exp_name[$$self].mers$l.csfasta";
        open(my $fh, ">$f")
          or $objTool[$$self]->Log_DIED("can't create output to store quality checked tag: $f");
        $FHandles{"mers$l"} = $fh;
    }

    # create a file to store poor quality tags
    my $f = "$output_dir[$$self]$exp_name[$$self].poor.csfasta";
    open(my $poor, ">$f")
      or $objTool[$$self]->Log_DIED("can't create file: $f to store poor quality tags");
    $FHandles{"poor"} = $poor;

    # check tag qualtiy
    &check_quality($self, \%FHandles);

    # close all inputs and outputs
    foreach my $f (keys %FHandles) { close $FHandles{$f} }

    $objTool[$$self]->Log_SUCCESS("finished tag quality chek");
}


=item * check_quality(\%hashOfFileHandlesForPoorQualData)

Read each of the csfasta files (from $arg->{'QUAL'} and $arg->{'CSFASTA'}), 
and remove any poor quality reads. Write these to the file handles $hash->{'poor'}.

=cut

sub check_quality {

    my ($self, $FHandles) = @_;

    my @l_tags = sort { $a <=> $b } @{$tag_lengths[$$self]};

    my @myMask     = split(//, $mask[$$self]);
    my $fh_qual    = $FHandles->{'QUAL'};
    my $fh_csfasta = $FHandles->{'CSFASTA'};

    while (my $l_qual = <$fh_qual>) {
        my $l_sequ;
        my $tagid;

        # find a tag id in both inputs
        if ($l_qual =~ m/^>/) {
            chomp($tagid = $l_qual);
            while ($l_sequ = <$fh_csfasta>) {
                if ($l_sequ =~ m/^>/) { last }
            }
            chomp($l_sequ);
            if ($tagid ne $l_sequ) {
                $objTool[$$self]->Log_DIED(
                    "The input CSFASTA file and QUALITY file don't use same format! see tagid: $tagid");
            }

            # read next form both inputs
            chomp($l_sequ = <$fh_csfasta>);
            chomp($l_qual = <$fh_qual>);
            my @scores = split(/\s/, $l_qual);
            my $poor   = 0;
            my $start  = 0;

            # The input CSFASTA file and QUALITY file don't use same format
            foreach my $l (@l_tags) {
                for (my $s = $start ; $s < $l ; $s++) {
                    if (($myMask[$s] == 1) && ($scores[$s] < LowScore)) { $poor++ }
                }
                if ($poor > NumPoor) {

                    #  if there are more than 4 low color value during sequence range [0,shortest tag allowed length],
                    #  we treat this tag as poor quality tag
                    if ($start == 0) { my $f = $FHandles->{'poor'}; print $f "$tagid\n$l_sequ\n" }

                    #  chop tag at last tag length.
                    else {
                        my $f = $FHandles->{"mers$start"};
                        print $f "$tagid\n" . substr($l_sequ, 0, $start + 1) . "\n";
                    }
                    last;
                }
                $start = $l;
            }

            #  when the total low color value is less than NumPoor, we keep this tag's full sequence
            if ($poor <= NumPoor) {
                my $f = $FHandles->{"mers$start"};
                print $f "$tagid\n" . substr($l_sequ, 0, $start + 1) . "\n";
            }

        }

    }
}

1;
__END__

=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

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
