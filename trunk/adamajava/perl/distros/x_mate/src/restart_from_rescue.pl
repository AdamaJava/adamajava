#! /usr/bin/perl -w

=head1 NAME

restart_from_rescue.pl

=head1 USAGE

 perl restart_from_rescue.pl -c <configFile>

=head1 DESCRIPTION

Restart the X-MATE pipeline after mapping and before the optional multi-map
rescue stage.  This script invokes the MuMRescueLite.py package.  All 
configuration parameters for this should be specified in the configuration
file (-c configFile).

=head1 AUTHORS

=over 3

=item Qinying Xu (Christina) (q.xu@imb.uq.edu.au)

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

use strict;
use Getopt::Std;
use QCMG::BaseClass::Quality;
use QCMG::BaseClass::Tools;
use QCMG::BaseClass::WigStart;
use QCMG::BaseClass::MuMRescueLite;
use QCMG::BaseClass::Junction;
use QCMG::X_Mate::RNA_Mapreads;
use QCMG::X_Mate::DNA_Mapreads;
use QCMG::X_Mate::DNA_ISAS;
use QCMG::X_Mate::RNA_ISAS;
use QCMG::X_Mate::XConfig::mainConf;
use constant {TRUE => "true", FALSE => "false"};

my $USAGE = "usage: perl $0 -c <configFile>\n";
die $USAGE unless @ARGV;

our $opt_c;
getopt('c');
my $objConf = QCMG::X_Mate::XConfig::mainConf->new('fname' => $opt_c);

my $objTool =
  QCMG::BaseClass::Tools->new('f_log' => $objConf->output_dir . $objConf->exp_name . ".log");
$objConf->set_objTool($objTool);
$objTool->Log_PROCESS("welcome to our XMATE - Restarting from Rescue");

# creating tag file for recurisve mapping, the number of tag files are the number of recursive loop
my $f_forMap;

my $objMap;
if ($objConf->map_ISAS eq TRUE) {
    if ($objConf->map_junction eq TRUE) {
        $objMap = QCMG::X_Mate::RNA_ISAS->new('conf' => $objConf);
    }
    else { $objMap = QCMG::Xmate::DNA_ISAS->new('conf' => $objConf) }
}
else {
    if ($objConf->map_junction eq TRUE) {
        $objMap = QCMG::X_Mate::RNA_Mapreads->new('conf' => $objConf);
    }
    else { $objMap = QCMG::X_Mate::DNA_Mapreads->new('conf' => $objConf) }
}

# report junction mapping
if ($objConf->map_junction eq TRUE) {
    my $objJunc = QCMG::BaseClass::Junction->new(
        exp_name      => $objConf->exp_name,
        output_dir    => $objConf->output_dir,
        junc_index    => $objConf->junc_index,
        expect_strand => $objConf->expect_strand,
        max_mismatch  => $objConf->max_mismatch,
        objTool       => $objTool);

    $objJunc->main($objMap->junction_collated);
}

# selectiong for genome mapping
my $f_positive;
my $f_negative;
if ($objConf->run_rescue eq TRUE) {
    ($f_positive, $f_negative) = &MuMRescueLite($objConf, $objMap->genome_collated);
}
else { ($f_positive, $f_negative) = &SingleSelect($objConf, $objMap->genome_collated) }

# create wiggle plot
if ($objConf->expect_strand eq "0") { &nonStrandWig($objConf, $f_positive, $f_negative); }
else                                { &StrandWig($objConf, $f_positive, $f_negative) }

$objTool->Log_SUCCESS("All done, enjoy the data!");
exit;

sub nonStrandWig {
    my ($objConf, $ref_positive, $ref_negative) = @_;

    # join both positive and negative file into one, named positive temporely
    for (my $i = 0 ; $i < scalar(@$ref_positive) ; $i++) {
        my $fp = $ref_positive->[$i];
        my $fn = $ref_negative->[$i];
        open(POS, ">>$fp")
          or $objConf->objTool->Log_DIED("can't open file $fp in sub nonStrandWig");
        open(NEG, $fn) or $objConf->Log_DIED("can't open file $fn in sub nonStrandWig");
        while (my $line = <NEG>) { print POS $line }
        close(POS);
        close(NEG);
        my $tmp = "$fp.tmp";
        system("sort -n -k1 $fp > $tmp");
        unlink($fp);
        rename($tmp, $fp);
    }

    # create both wiggle plot file and start file
    my $objWig = QCMG::BaseClass::WigStart->new(
        'exp_name'   => $objConf->exp_name,
        'output_dir' => $objConf->output_dir,
        'objTool'    => $objTool);
    $objWig->main($ref_positive, "wiggle", $objConf->exp_name, $objConf->exp_name . " - unstranded");
    $objWig->main($ref_positive, "start", $objConf->exp_name, $objConf->exp_name . " - unstranded");

    # delete all temporly file
    for (my $i = 0 ; $i < scalar(@$ref_positive) ; $i++) {
        my $fp = $ref_positive->[$i];
        my $fn = $ref_negative->[$i];
        unlink($fp);
        unlink($fn);
    }
}

sub StrandWig {
    my ($objConf, $ref_positive, $ref_negative) = @_;

    my $objWig = QCMG::BaseClass::WigStart->new(
        'exp_name'   => $objConf->exp_name,
        'output_dir' => $objConf->output_dir,
        'objTool'    => $objTool);

    # positive tags
    $objWig->main($ref_positive, "wiggle", $objConf->exp_name, $objConf->exp_name . " - positive strand");
    rename($objWig->output, $objWig->output . ".positive");
    $objWig->main($ref_positive, "start", $objConf->exp_name, $objConf->exp_name . " - positive strand");
    rename($objWig->output, $objWig->output . ".positive");

    # negative tags
    $objWig->main($ref_negative, "wiggle", $objConf->exp_name, $objConf->exp_name . " - negative strand");
    rename($objWig->output, $objWig->output . ".negative");
    $objWig->main($ref_negative, "start", $objConf->exp_name, $objConf->exp_name . " - negative strand");
    rename($objWig->output, $objWig->output . ".negative");

    for (my $i = 0 ; $i < scalar(@$ref_positive) ; $i++) {
        my $fp = $ref_positive->[$i];
        my $fn = $ref_negative->[$i];
        unlink($fp);
        unlink($fn);
    }
}

sub SingleSelect {
    my ($objConf, $ref_collated) = @_;
    my $objSiM = QCMG::BaseClass::SingleSelect->new(
        'exp_name'     => $objConf->exp_name,
        'output_dir'   => $objConf->output_dir,
        'max_mismatch' => $objConf->max_mismatch,
        'chr_names'    => $objConf->chr_names,

        # must set "0" to strand, since we ignor strand for genome mapping selection
        'expect_strand' => "0",
        'objTool'       => $objConf->get_objTool,);

    # create SiM file
    $objSiM->SingleSelect($ref_collated);

    # convert SiM file formart for wigplot
    $objSiM->afSelect;

    return ($objSiM->out_Positive, $objSiM->out_Negative);
}

sub MuMRescueLite {
    my ($objConf, $ref_collated) = @_;
    my $objResc = QCMG::BaseClass::MuMRescueLite->new(
        'exp_name'       => $objConf->exp_name,
        'output_dir'     => $objConf->output_dir,
        'max_mismatch'   => $objConf->max_mismatch,
        'chr_names'      => $objConf->chr_names,
        'rescue_program' => $objConf->rescue_program,
        'rescue_window'  => $objConf->rescue_window,
        'objTool'        => $objConf->get_objTool,
        'max_hits'       => $objConf->max_hits);

    $objResc->main($ref_collated);
    return ($objResc->out_Positive, $objResc->out_Negative);
}

