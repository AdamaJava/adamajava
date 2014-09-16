package PGTools::Command::TandemProcess;


use strict;
use Getopt::Long;
use PGTools::Util::TandemProcessor;
use PGTools::Util;
use parent 'PGTools::Command';

=head1 NAME

PGTools::TandemProcess

=head1 SYNOPSIS

  ./pgtools tandem_process <tandem_output_file>  <path_to_csv_file>


Converts the XML output produces by XTandem to a CSV file which we can 
conviniently use to run further processing


=cut


sub run {
  my $class   = shift; 

  my ( $ifile, $ofile ) = @ARGV; 

  debug "About to process tandem output: $ofile ";

  PGTools::Util::TandemProcessor->new(
    ifile => $ifile, 
    ofile => $ofile
  )->process;

  debug "Done proceesing: $ofile";


}

1;
__END__
