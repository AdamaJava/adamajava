package QCMG::X_Mate::DNA_ISAS;

=head1 NAME

QCMG::X_Mate::DNA_ISAS

=head1 SYNOPSIS

  use QCMG::X_Mate::DNA_ISAS
  my $obj = DNA_ISAS->new('conf' => $objConf);
  $obj->recursive;

=head1 DESCRIPTION

This module manages the runs for DNA recursive mapping using Imagenix ISAS. 
It contains one method, 'recursive' to call the mapping, then passes 
mapped tag to a collated file and chop the nonmapped tag and map it again. 

=head2 Methods

=over 2

=cut 

use strict;
use Object::InsideOut;
use QCMG::BaseClass::ISAS;
use File::Basename;
use Carp;
use constant {COLOR_SPACE => 0, BASE_SPACE => 1};

# parameter from configure file straiteway
my @objConf : Field : Arg('Name' => 'conf', 'Mandatory' => 1) : Get('objConf');
my @objTool : Field;

# push all collated file name into this array
my @outGeno : Field : Get(genome_collated);

sub _init : Init {
    my ($self, $arg) = @_;
    my $conf = $objConf[$$self];
    $self->set(\@objTool, $objConf[$$self]->get_objTool);

# created output file names -- collated files; one array of genome collated file and another array of genome collated file
    my $isas_genome = $objConf[$$self]->isas_genome;
    foreach my $a (@$isas_genome) {
        my ($l, $m) = split(',', $a->global);
        my $f = $objConf[$$self]->output_dir . $objConf[$$self]->exp_name . ".geno.mers$l.collated";
        push @{$outGeno[$$self]}, $f;
    }
}


=item * $obj->recursive;

This function call QCMG::BaseClass::Mapping, collate mapped tags and then call 
BassClass::Mapping again to map to junction library, then chops the non-mapped tags
recursively.

=cut

sub recursive {
    my ($self, $f_forMap) = @_;

    # create a two dimention array to store recursive mapping parameters
    my @maps  = ();
    my $array = $objConf[$$self]->map_lengths;
    @maps = sort { $b <=> $a } @$array;

    my $l_last     = $maps[0];
    my $f_nonMatch = $f_forMap->[0];
    $objTool[$$self]->Log_PROCESS("start recursive mapping...");
    for (my $i = 0 ; $i < scalar(@maps) ; $i++) {

        # chop tag
        my $l_chop     = $l_last - $maps[$i];
        my $f_shorttag = $f_forMap->[$i];
        if ($l_chop < 0) {
            $objTool[$$self]
              ->Log_DIED("can't chop tag from $l_last to $maps[$i] in sub DNAMapping::recursive");
        }
        if ($l_chop > 0) {
            $objTool[$$self]->Log_PROCESS("chopping tag from mers$l_last to mers$maps[$i]");
            if ($objConf[$$self]->space_code == BASE_SPACE) {
                $objTool[$$self]->chop_fastq_tag($f_nonMatch, $f_shorttag, $l_chop);
            }
            else {
                $objTool[$$self]->chop_tag($f_nonMatch, $f_shorttag, $l_chop);
            }
            $objTool[$$self]->Log_SUCCESS("chopped tag from mers$l_last to mers$maps[$i]");
        }

        my $genome = $objConf[$$self]->isas_genome->[$i];
        my $objMap = QCMG::BaseClass::ISAS->new(
            'exp_name'   => $objConf[$$self]->exp_name,
            'output_dir' => $objConf[$$self]->output_dir,
            'isas'       => $objConf[$$self]->isas,
            'database'   => $genome->database,
            'chr'        => $genome->chr,
            'mode'       => $genome->mode,
            'verbose'    => $genome->verbose,
            'limit'      => $genome->limit,
            'filter'     => $genome->filter,
            'global'     => $genome->global,
            'file'       => $f_shorttag,
            'chrRename'  => $genome->IndexRename,
            'objTool'    => $objConf[$$self]->get_objTool,
            'space_code' => $objConf[$$self]->space_code);

        $objMap->mapping;
        if ($objConf[$$self]->space_code == BASE_SPACE) {
            $objMap->collation_bases;
        }
        elsif ($objConf[$$self]->space_code == COLOR_SPACE) {
            $objMap->collation_colors;
        }
        $f_nonMatch = $objMap->f_nonMatch;
        rename($objMap->f_collated, $outGeno[$$self]->[$i]);
        $l_last = $maps[$i];

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

