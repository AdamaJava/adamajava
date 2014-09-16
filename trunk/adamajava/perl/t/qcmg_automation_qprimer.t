###########################################################################
#
#  File:     qcmg_automation_qprimer.t
#  Creator:  Lynn Fink
#  Created:  2011-10-19
#
#  $Id: qcmg_automation_qprimer.t 1294 2011-10-19 06:41:11Z l.fink $
#
###########################################################################

use Test::More tests => 16;
BEGIN { use_ok('QCMG::Automation::QPrimer') };

use_ok('QCMG::SamTools::Sam');

my $SEQ	=
'GTTCTAGGGCCTATGAGGGCCACATGGAAAGATGGTCTGGGGTATTAAGATTACAGGCCAGAGGCCAACTGTCTAGCATCAAAAAGCAGCTCTCCAACTTGCTGGCTGTGAGGTCTTTCTGGAATCTCAATTTGCTCATCTATCAGAAGAGTAATAAGTCCTACTTGGAGGGATGTTTGAAGATTCAATATTTGTAAAACACTGGGAACAGTGTCTGGCACAGGGGTAAGTGCCAAGTGTACTGTAAAACAAGTAAGTACACACACAAAAAATTCTCATCATGTCACTCTCCTGTTCAGAATCCACCAATGGCTGCCCACCTGACACCCATGGGCCTCCCGGCACGGAGCCTGGCTGTCCTCTGATATTTACCCATGCTCTTCCTGTCCCATGGCACTGTTCCTTTGGCCACGACACCCCCACCACTGGGCTCTAGTGCCACTGCTCCCTGAAACGTTCTGCTGGGAGTTCATGCCTTTAACGACTGCCTGCCCCACCCCC';
my $TARGET	= '248,5';

my $SEQ2 =
'NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN';
my $TARGET2	= '199,5';

# test with arbitrary SNP position
my $qp = QCMG::Automation::QPrimer->new(POS	=> 'chr1:2342398-2342398',
					GENOME	=> '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa'
					);

ok( $qp->pos()		eq 'chr1:2342398-2342398', 'Original SNP position');
ok( $qp->rs()		eq 'chr1', 'Reference sequence of SNP');
ok( $qp->coord()	== 2342398, 'Genomic coordinate of SNP');
ok( $qp->p3targetlen()	== 5, 'Default length of genome that cannot be in a primer (5)');
ok( $qp->p3halflength()	== 250, 'Half default window size of genome around SNP (250)');
ok( $qp->p3target()	== 248, 'Default shift size of window (248)');

ok( $qp->num_primer_pairs_to_get()	== 200, 'Default number of primers to get from primer3 (200)');
ok( -d $qp->path_blast(), 'Path to BLAST executable exists?');
ok( -d $qp->path_primer3(), 'Path to primer3 executable exists?');
ok( -e $qp->path_dbsnp(), 'dbSNP file exists?');

# try trivial case, where SNP is not near ends of chromosome
my ($seq, $target)	= $qp->get_genomic_seq();
ok( $seq->[0] eq $SEQ, "Genomic sequence around SNP matches expected sequence");
ok( $target->[0] eq $TARGET, "primer3 target");

# try harder case where SNP is too close to chromosome ends
my $qp = QCMG::Automation::QPrimer->new(POS	=> 'chr1:200-200',
					GENOME	=> '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa'
					);
my ($seq, $target)	= $qp->get_genomic_seq();
ok( $seq->[0] eq $SEQ2, "Genomic sequence around SNP matches expected sequence in edge case");
ok( $target->[0] eq $TARGET2, "primer3 target for edge case");
