package QCMG::X_Mate::RNA_Mapreads;

=head1 NAME

QCMG::X_Mate::RNA_Mapreads

=head1 SYNOPSIS

  use QCMG::X_Mate::RNA_Mapreads
  my $obj = RNA_Mapreads->new('conf' => $objConf);
  $obj->recursive;

=head1 DESCRIPTION

This module manages the runs for RNA recursive mapping using AB mapreads. 
It contains one method, 'recursive' to call the mapping, then passes 
mapped tag to a collated file and chop the nonmapped tag and map it again. 
This module require an reference of DNA_Mapreads module which contain all 
DNA mapping configuration (See detail in DNA_Mapreads module's documentation)

=head2 Methods

=over 2

=cut 

use strict;
use Object::InsideOut 'QCMG::X_Mate::DNA_Mapreads';
use File::Basename;
use Carp;

# define the constants for DNA-mate configure
use constant {};

# parameter from configure file straiteway
my @objConf : Field : Arg('Name' => 'conf', 'Mandatory' => 1);
my @objTool : Field;

# push all collated file name into this array
my @outGeno : Field : Get(genome_collated);
my @outJunc : Field : Get(junction_collated);

sub _init : Init {
    my ($self, $arg) = @_;
    my $conf = $objConf[$$self];
    $self->set(\@objTool, $objConf[$$self]->get_objTool);

    # created output file names -- collated files; one array of genome collated file and another array of genome collated file
    my $array = $objConf[$$self]->recursive_maps;
    foreach my $a (@$array) {
        my $f_geno =
          $objConf[$$self]->output_dir . $objConf[$$self]->exp_name . ".geno.$a.collated";
        push @{$outGeno[$$self]}, $f_geno;
        my $f_junc =
          $objConf[$$self]->output_dir . $objConf[$$self]->exp_name . ".junc.$a.collated";
        push @{$outJunc[$$self]}, $f_junc;
    }
}


=item * $obj->recursive;

This function call QCMG::BaseClass::Mapping, collate mapped tags and then call 
BassClass::Mapping again to map to junction library, then chop non-mapped tags recursivly

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
        my %para = (
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
            'objTool'      => $objTool[$$self],);
        my $objGenMap = QCMG::BaseClass::Mapping->new(%para);

        $objTool[$$self]->Log_PROCESS("genome mapping -- mers" . $maps[$i][0]);

        $objGenMap->mapping;
        $objTool[$$self]->Log_PROCESS("collating genome tags -- mers" . $maps[$i][0]);
        $objGenMap->collation;

        # change the collated file name mark "geno" to it
        rename($objGenMap->f_collated, $outGeno[$$self]->[$i]);

        # create junction mapping instance by passing junction mapping parameter to it
        my $ref_junc = $objConf[$$self]->junc_lib;
        $para{'genomes'}  = $ref_junc->[$i];
        $para{'tag_file'} = $objGenMap->f_nonMatch;

        my $objJuncMap = QCMG::BaseClass::Mapping->new(%para);
        $objTool[$$self]->Log_PROCESS("junction mapping -- mers" . $maps[$i][0]);
        $objJuncMap->mapping;
        $objTool[$$self]->Log_PROCESS("collating junction tags -- mers" . $maps[$i][0]);
        $objJuncMap->collation;

        # change the collated file marked "junc" to it
        rename($objJuncMap->f_collated, $outJunc[$$self]->[$i]);

        $l_last     = $maps[$i][0];
        $f_nonMatch = $objJuncMap->f_nonMatch;
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
