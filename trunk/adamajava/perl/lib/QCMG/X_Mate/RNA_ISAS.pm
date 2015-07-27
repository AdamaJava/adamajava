package QCMG::X_Mate::RNA_ISAS;

=head1 NAME

QCMG::X_Mate::RNA_ISAS

=head1 SYNOPSIS

  use QCMG::X_Mate::RNA_ISAS
  my $obj = RNA_ISAS->new('conf' => $objConf);
  $obj->recursive;

=head1 DESCRIPTION

This module manages the runs for RNA recursive mapping using Imagenix ISAS. 
It contains one method, 'recursive' to call the mapping, then passes 
mapped tag to a collated file and chop the nonmapped tag and map it again. 
This module requires a reference of DNA_ISAS module which contains all 
DNA mapping configuration (See detail in DNA_ISAS module's documentation)

=head2 Methods

=over 2

=cut 

use strict;
use Object::InsideOut 'QCMG::X_Mate::DNA_ISAS';
use QCMG::BaseClass::ISAS;
use File::Basename;
use Carp;
use constant {COLOR_SPACE => 0, BASE_SPACE => 1};

# push all collated file name into this array
my @outJunc : Field : Get(junction_collated);

sub _init : Init {
    my ($self, $arg) = @_;

    # 	$self->set(\@objConf, $self->DNA_ISAS::objConf);
    my $objConf = $self->QCMG::X_Mate::DNA_ISAS::objConf;

    # created output file names -- collated files; one array of genome collated file and another array of genome collated file
    my $isas_genome = $objConf->isas_genome;

    foreach my $a (@$isas_genome) {
        my ($l, $m) = split(',', $a->global);
        my $f = $objConf->output_dir . $objConf->exp_name . ".junc.mers$l.collated";
        push @{$outJunc[$$self]}, $f;
    }
}



=item * $obj->recursive;

This function call QCMG::BaseClass::Mapping, collate mapped tags and then call 
BassClass::Mapping again to map to junction library, then chops the non-mapped tags
recursively.

=cut

sub recursive {
    my ($self, $f_forMap) = @_;

    my $objConf = $self->QCMG::X_Mate::DNA_ISAS::objConf;
    my $objTool = $objConf->get_objTool;

    # create a two dimention array to store recursive mapping parameters
    my @maps  = ();
    my $array = $objConf->map_lengths;
    @maps = sort { $b <=> $a } @$array;

    my $l_last     = $maps[0];
    my $f_nonMatch = $f_forMap->[0];
    $objTool->Log_PROCESS("start mapping...");

    for (my $i = 0 ; $i < scalar(@maps) ; $i++) {

        # chop tag
        my $l_chop     = $l_last - $maps[$i];
        my $f_shorttag = $f_forMap->[$i];
        if ($l_chop < 0) {
            $objTool->Log_DIED(
                "can't chop tag from $l_last to $maps[$i] in sub DNAMapping::recursive");
        }
        if ($l_chop > 0) {
            $objTool->Log_PROCESS("chopping tag from mers$l_last to mers$maps[$i]");
            if ($objConf->space_code == BASE_SPACE) {
                $objTool->chop_fastq_tag($f_nonMatch, $f_shorttag, $l_chop);
            }
            else {
                $objTool->chop_tag($f_nonMatch, $f_shorttag, $l_chop);
            }

            # deleted the nonMatch file as it is not the file for mapping
            unlink($f_nonMatch);
        }

        # genome mapping
        my $genome = $objConf->isas_genome->[$i];
        my %para   = (
            'exp_name'   => $objConf->exp_name,
            'output_dir' => $objConf->output_dir,
            'isas'       => $objConf->isas,
            'database'   => $genome->database,
            'chr'        => $genome->chr,
            'mode'       => $genome->mode,
            'verbose'    => $genome->verbose,
            'limit'      => $genome->limit,
            'filter'     => $genome->filter,
            'global'     => $genome->global,
            'file'       => $f_shorttag,
            'chrRename'  => $genome->IndexRename,
            'objTool'    => $objConf->get_objTool,
            'space_code' => $objConf->space_code);
        my $objMap = QCMG::BaseClass::ISAS->new(%para);

        $objTool->Log_PROCESS("genome mapping -- mers" . $objMap->global);

        $objMap->mapping;

        $objTool->Log_PROCESS("collating genome mapping -- mers" . $objMap->global);

        if ($objConf->space_code == BASE_SPACE) {
            $objMap->collation_bases;
        }
        elsif ($objConf->space_code == COLOR_SPACE) {
            $objMap->collation_colors;
        }

        rename($objMap->f_collated, $self->QCMG::X_Mate::DNA_ISAS::genome_collated->[$i]);

        # junction mapping
        $para{'database'} = $objConf->isas_junc->[$i]->database;
        $para{'chr'}      = $objConf->isas_junc->[$i]->chr;
        $para{'file'}     = $objMap->f_nonMatch;

        $para{'chrRename'} = $objConf->isas_junc->[$i]->IndexRename;
        $objMap = QCMG::BaseClass::ISAS->new(%para);

        $objTool->Log_PROCESS("junction mapping -- mers" . $objMap->global);

        $objMap->mapping;

        $objTool->Log_PROCESS("collating junction tags -- mers" . $objMap->global);

        if ($objConf->space_code == BASE_SPACE) {
            $objMap->collation_bases;
        }
        elsif ($objConf->space_code == COLOR_SPACE) {
            $objMap->collation_colors;
        }

        rename($objMap->f_collated, $outJunc[$$self]->[$i]);
        $l_last     = $maps[$i];
        $f_nonMatch = $objMap->f_nonMatch;

    }
    $objTool->Log_SUCCESS("recursive mapping is done, collated files are created");
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

