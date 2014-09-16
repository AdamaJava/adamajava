package QCMG::X_Mate::DNA_Mapreads;

=head1 NAME

QCMG::X_Mate::DNA_Mapreads

=head1 SYNOPSIS

  use QCMG::X_Mate::DNA_Mapreads
  my $obj = DNA_Mapreads->new('conf' => $objConf);
  $obj->recursive;

=head1 DESCRIPTION

This module is runs recursive mapping for a genomic data set using AB mapreads.
It starts by calling the mapping, then passing the mapped tags to a collated file 
then choping the nonmapped tags, and maps again. 

=head2 Methods

=over 2

=cut

use strict;
use Object::InsideOut;
use QCMG::BaseClass::Mapping;
use QCMG::BaseClass::Tools;
use File::Basename;
use Carp;

# parameter from configure file straiteway
my @objConf : Field : Arg('Name' => 'conf', 'Mandatory' => 1);
my @objTool : Field;

# push all collated file name into this array
my @outGeno : Field : Get(genome_collated);

sub _init : Init {
    my ($self, $arg) = @_;
    my $conf = $objConf[$$self];
    $self->set(\@objTool, $objConf[$$self]->get_objTool);

    # created output file name -- collated files
    my $array = $objConf[$$self]->recursive_maps;
    foreach my $a (@$array) {
        my $f_geno =
          $objConf[$$self]->output_dir . $objConf[$$self]->exp_name . ".geno.$a.collated";
        push @{$outGeno[$$self]}, $f_geno;
    }
}


=item * $obj->recursive;

This function calls QCMG::BaseClass::Mapping, collates mapped tags and then chops non-mapped tags to
map again recursively.

=cut

sub recursive {
    my ($self, $f_forMap) = @_;

    # create a two dimention array to store recursive mapping parameters
    my @maps  = ();
    my $array = $objConf[$$self]->recursive_maps;
    foreach my $a (@$array) {
        my @s_map = split(/\./, $a);
        push @maps, [@s_map];
    }

    @maps = sort { $b->[0] <=> $a->[0] } @maps;

    my $l_last     = $maps[0][0];
    my $f_nonMatch = $f_forMap->[0];

    for (my $i = 0 ; $i < scalar(@maps) ; $i++) {

        # chop tag
        my $l_chop     = $l_last - $maps[$i][0];
        my $f_shorttag = $f_forMap->[$i];
        if ($l_chop < 0) {
            $objTool[$$self]->Log_DIED(
                "can't chop tag from $l_last to $maps[$i][0] in sub DNAMapping::recursive");
        }
        if ($l_chop > 0) {
            $objTool[$$self]->Log_PROCESS("chopping tag from mers$l_last to mers$maps[$i][0]");
            $objTool[$$self]->chop_tag($f_nonMatch, $f_shorttag, $l_chop);
        }

        # mapping &collate
        my $objMap = QCMG::BaseClass::Mapping->new(
            'genomes'      => $objConf[$$self]->genomes,
            'exp_name'     => $objConf[$$self]->exp_name,
            'output_dir'   => $objConf[$$self]->output_dir,
            'mapreads'     => $objConf[$$self]->mapreads,
            'max_hits'     => $objConf[$$self]->max_hits,
            'scratch_dir'  => $objConf[$$self]->scratch_dir,
            'space_code'   => $objConf[$$self]->space_code,
            'qsub_command' => $objConf[$$self]->qsub_command,
            'tag_file'     => $f_shorttag,
            'mask'         => $objConf[$$self]->mask,
            'tag_length'   => $maps[$i][0],
            'mismatch'     => $maps[$i][1],
            'adj_error'    => $maps[$i][2],
            'objTool'      => $objTool[$$self],

        );

        $objTool[$$self]->Log_PROCESS("mapping -- mers" . $maps[$i][0]);
        $objMap->mapping;
        $objTool[$$self]->Log_PROCESS("collating tags -- mers" . $maps[$i][0]);
        $objMap->collation;

        $f_nonMatch = $objMap->f_nonMatch;
        rename($objMap->f_collated, $outGeno[$$self]->[$i]);
        $l_last = $maps[$i][0];
    }

    $objTool[$$self]->Log_SUCCESS("recursive mapping is done, collated files are created");

}

1;
__END__

=back

=head1 AUTHORS

=over 3

=item Qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=item David Wood (d.wood@imb.uq.edu.au)

=back

=head1 COPYRIGHT

This software is copyright 2010 by the Queensland Centre for Medical
Genomics. All rights reserved.  This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes. Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection with 
the production, marketing, sale or support of any commercial product or
service or for any governmental purposes.  For commercial or governmental 
use, please contact licensing\@qcmg.org.

In any work or product derived from the use of this Software, proper 
attribution of the authors as the source of the software or data must be 
made.  The following URL should be cited:

  http://bioinformatics.qcmg.org/software/

=cut


