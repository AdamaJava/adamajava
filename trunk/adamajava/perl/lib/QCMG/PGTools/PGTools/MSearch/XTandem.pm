package PGTools::MSearch::XTandem;

use strict;
use parent 'PGTools::MSearch::Base';
use IO::File;
use Data::Dumper;
use File::Spec::Functions;
use PGTools::Util; 

use PGTools::Util::Path qw/
  create_path_within_scratch
  path_within_scratch
/;


sub get_runnable {
  my $self = shift;

  my @runs = ( );

  $self->_extract_xml_files;
  $self->_prepare_default_input_xml_file;

  my $dir             = directory $self->_input_xml_path;


  die "

    The program does not exist in the said path: @{[ $self->command ]}
    Please modify the configuration file before you continue

  " unless -e $self->command;



  unless( $self->is_decoy_concatenated ) {

    $self->_prepare_taxonomy_xml_file( 
      $self->database
    );

    $self->_prepare_input_xml_file; 

    push @runs, $self->_xtandem( 
      $self->_input_xml_path
    );

    push @runs, $self->_post_process( qr/output\./, $dir, '.csv' ) 

  }

  $self->_prepare_taxonomy_xml_file( 
    $self->decoy_database,
    $self->_taxonomy_xml_path( '-d' )
  );

  $self->_prepare_input_xml_file( '-d' );

  push @runs, $self->_xtandem( 
    $self->_input_xml_path( '-d' )
  );

  push @runs, $self->_post_process( qr/output-d/, $dir, '-d.csv' );

  sub {
    print "About to process XTandem ... \n";

    for ( @runs ) {
      $_->() if ref eq 'CODE';
    }

    print "Done processing: XTandem \n";
  };

}


sub _post_process {
  my ( $self, $matcher, $dir, $suffix ) = @_;

  sub {
      my ( $ofile ) = grep {
        /$matcher/
      } <$dir/*.xml>;

      run_pgtool_command sprintf( 'tandem_process %s %s', $ofile, $self->ofile( $suffix ) );
  };
}


sub _xtandem {
  my ( $self, $input_xml ) = @_;
  sub {
    print "About to run XTandem! : \n";
    run_command sprintf( '%s %s', $self->command, $input_xml ) , "xtandem";
  };
}


sub _default_input_xml_path {
  shift->path_for( 'default_input.xml' );
}

sub _prepare_default_input_xml_file {
  my $self = shift;

  $self->_write_file( 
    $self->_default_input_xml_path, 
    $self->_get_file_data( 'default_input.xml' ) 
  );
}

sub _taxonomy_xml_path {
  my ( $self, $suffix )  = @_;
  shift->path_for( 'taxonomy' . $suffix . '.xml' );
}

sub _prepare_taxonomy_xml_file {
  my ( $self, $database, $path ) = @_;

  my $taxonomy_data = $self->_get_file_data( 'taxonomy.xml' );
  my $database = $database || $self->database;

  $taxonomy_data =~ s/\{DATABASE\}/$database/g;

  $self->_write_file( 
    $path || $self->_taxonomy_xml_path,
    $taxonomy_data
  );
}

sub _input_xml_path {
  my ( $self, $suffix ) = @_;

  $self->path_for( 'input' . $suffix . '.xml' );
}


sub _output_file_path {
  my ( $self, $suffix ) = @_;

  $suffix ||= '';

  shift->path_for( 'output' . $suffix . '.xml' );
}

sub _prepare_input_xml_file {
  my $self = shift;
  my $suffix = shift || '';
  my $data = $self->_get_file_data( 'input.xml' );

  $data =~ s/\{DEFAULT_INPUT_XML\}/$self->_default_input_xml_path/ge;

  $data =~ s/\{TAXONOMY_XML\}/$self->_taxonomy_xml_path( $suffix )/ge;

  $data =~ s/\{SPECTRA_FILE\}/abs_path( $self->ifile )/ge;

  $data =~ s/\{OUTPUT_FILE\}/$self->_output_file_path( $suffix )/ge;


  $self->_write_file(
    $self->_input_xml_path( $suffix ), 
    $data
  );

}




sub _write_file {
  my ( $self, $file, $data ) = @_;

  my $fh = IO::File->new( $file, 'w' );

  $fh->print( $data );

  $fh->close;
}




sub create_input_xml_file {
  my $self = shift;

  my $input_xml = catfile( 
    $self->my_scratch_path, 
    'input.xml'
  );
}


sub _extract_xml_files {

  my $self = shift;
  my %files = ( );
  my $current_file = '';

  while( <DATA> ) {

    if( /^@@([^.]+\.xml)$/ ) {
      $files{ $1 } = '';
      $current_file = $1;
    }

    else {
      $files{ $current_file } .= $_ 
        if $current_file;
    }

  }

  $self->{files} = \%files;

}


sub _get_file_data { 
  my ( $self, $file_name ) = @_;

  $self->{files}{ $file_name };
}






1;
__DATA__
@@input.xml
<?xml version="1.0"?>
<bioml>
	<note>
	Each one of the parameters for x! tandem is entered as a labeled note node. 
	Any of the entries in the default_input.xml file can be over-ridden by
	adding a corresponding entry to this file. This file represents a minimum
	input file, with only entries for the default settings, the output file
	and the input spectra file name. 
	See the taxonomy.xml file for a description of how FASTA sequence list 
	files are linked to a taxon name.
	</note>

	<note type="input" label="list path, default parameters">{DEFAULT_INPUT_XML}</note>
	<note type="input" label="list path, taxonomy information">{TAXONOMY_XML}</note>

	<note type="input" label="protein, taxon">home.sapien</note>
	
	<note type="input" label="spectrum, path">{SPECTRA_FILE}</note>

	<note type="input" label="output, path">{OUTPUT_FILE}</note>
</bioml>


@@default_input.xml
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="tandem-input-style.xsl"?>
<bioml>
<note>list path parameters</note>
	<note type="input" label="list path, default parameters">default_input.xml</note>
		<note>This value is ignored when it is present in the default parameter
		list path.</note>
	<note type="input" label="list path, taxonomy information">taxonomy.xml</note>

<note>spectrum parameters</note>
	<note type="input" label="spectrum, fragment monoisotopic mass error">0.4</note>
	<note type="input" label="spectrum, parent monoisotopic mass error plus">100</note>
	<note type="input" label="spectrum, parent monoisotopic mass error minus">100</note>
	<note type="input" label="spectrum, parent monoisotopic mass isotope error">yes</note>
	<note type="input" label="spectrum, fragment monoisotopic mass error units">Daltons</note>
	<note>The value for this parameter may be 'Daltons' or 'ppm': all other values are ignored</note>
	<note type="input" label="spectrum, parent monoisotopic mass error units">ppm</note>
		<note>The value for this parameter may be 'Daltons' or 'ppm': all other values are ignored</note>
	<note type="input" label="spectrum, fragment mass type">monoisotopic</note>
		<note>values are monoisotopic|average </note>

<note>spectrum conditioning parameters</note>
	<note type="input" label="spectrum, dynamic range">100.0</note>
		<note>The peaks read in are normalized so that the most intense peak
		is set to the dynamic range value. All peaks with values of less that
		1, using this normalization, are not used. This normalization has the
		overall effect of setting a threshold value for peak intensities.</note>
	<note type="input" label="spectrum, total peaks">50</note> 
		<note>If this value is 0, it is ignored. If it is greater than zero (lets say 50),
		then the number of peaks in the spectrum with be limited to the 50 most intense
		peaks in the spectrum. X! tandem does not do any peak finding: it only
		limits the peaks used by this parameter, and the dynamic range parameter.</note>
	<note type="input" label="spectrum, maximum parent charge">4</note>
	<note type="input" label="spectrum, use noise suppression">yes</note>
	<note type="input" label="spectrum, minimum parent m+h">500.0</note>
	<note type="input" label="spectrum, minimum fragment mz">150.0</note>
	<note type="input" label="spectrum, minimum peaks">15</note> 
	<note type="input" label="spectrum, threads">1</note>
	<note type="input" label="spectrum, sequence batch size">1000</note>
	
<note>residue modification parameters</note>
	<note type="input" label="residue, modification mass">57.022@C</note>
		<note>The format of this parameter is m@X, where m is the modfication
		mass in Daltons and X is the appropriate residue to modify. Lists of
		modifications are separated by commas. For example, to modify M and C
		with the addition of 16.0 Daltons, the parameter line would be
		+16.0@M,+16.0@C
		Positive and negative values are allowed.
		</note>
	<note type="input" label="residue, potential modification mass"></note>
		<note>The format of this parameter is the same as the format
		for residue, modification mass (see above).</note>
	<note type="input" label="residue, potential modification motif"></note>
		<note>The format of this parameter is similar to residue, modification mass,
		with the addition of a modified PROSITE notation sequence motif specification.
		For example, a value of 80@[ST!]PX[KR] indicates a modification
		of either S or T when followed by P, and residue and the a K or an R.
		A value of 204@N!{P}[ST]{P} indicates a modification of N by 204, if it
		is NOT followed by a P, then either an S or a T, NOT followed by a P.
		Positive and negative values are allowed.
		</note>

<note>protein parameters</note>
	<note type="input" label="protein, taxon">other mammals</note>
		<note>This value is interpreted using the information in taxonomy.xml.</note>
	<note type="input" label="protein, cleavage site">[RK]|{P}</note>
		<note>this setting corresponds to the enzyme trypsin. The first characters
		in brackets represent residues N-terminal to the bond - the '|' pipe -
		and the second set of characters represent residues C-terminal to the
		bond. The characters must be in square brackets (denoting that only
		these residues are allowed for a cleavage) or french brackets (denoting
		that these residues cannot be in that position). Use UPPERCASE characters.
		To denote cleavage at any residue, use [X]|[X] and reset the 
		scoring, maximum missed cleavage site parameter (see below) to something like 50.
		</note>
	<note type="input" label="protein, modified residue mass file"></note>
	<note type="input" label="protein, cleavage C-terminal mass change">+17.002735</note>
	<note type="input" label="protein, cleavage N-terminal mass change">+1.007825</note>
	<note type="input" label="protein, N-terminal residue modification mass">0.0</note>
	<note type="input" label="protein, C-terminal residue modification mass">0.0</note>
	<note type="input" label="protein, homolog management">no</note>
		<note>if yes, an upper limit is set on the number of homologues kept for a particular spectrum</note>

<note>model refinement parameters</note>
	<note type="input" label="refine">yes</note>
	<note type="input" label="refine, modification mass"></note>
	<note type="input" label="refine, sequence path"></note>
	<note type="input" label="refine, tic percent">20</note>
	<note type="input" label="refine, spectrum synthesis">yes</note>
	<note type="input" label="refine, maximum valid expectation value">0.1</note>
	<note type="input" label="refine, potential N-terminus modifications">+42.010565@[</note>
	<note type="input" label="refine, potential C-terminus modifications"></note>
	<note type="input" label="refine, unanticipated cleavage">yes</note>
	<note type="input" label="refine, potential modification mass"></note>
	<note type="input" label="refine, point mutations">no</note>
	<note type="input" label="refine, use potential modifications for full refinement">no</note>
	<note type="input" label="refine, point mutations">no</note>
	<note type="input" label="refine, potential modification motif"></note>
	<note>The format of this parameter is similar to residue, modification mass,
		with the addition of a modified PROSITE notation sequence motif specification.
		For example, a value of 80@[ST!]PX[KR] indicates a modification
		of either S or T when followed by P, and residue and the a K or an R.
		A value of 204@N!{P}[ST]{P} indicates a modification of N by 204, if it
		is NOT followed by a P, then either an S or a T, NOT followed by a P.
		Positive and negative values are allowed.
		</note>

<note>scoring parameters</note>
	<note type="input" label="scoring, minimum ion count">4</note>
	<note type="input" label="scoring, maximum missed cleavage sites">1</note>
	<note type="input" label="scoring, x ions">no</note>
	<note type="input" label="scoring, y ions">yes</note>
	<note type="input" label="scoring, z ions">no</note>
	<note type="input" label="scoring, a ions">no</note>
	<note type="input" label="scoring, b ions">yes</note>
	<note type="input" label="scoring, c ions">no</note>
	<note type="input" label="scoring, cyclic permutation">no</note>
		<note>if yes, cyclic peptide sequence permutation is used to pad the scoring histograms</note>
	<note type="input" label="scoring, include reverse">no</note>
		<note>if yes, then reversed sequences are searched at the same time as forward sequences</note>
	<note type="input" label="scoring, cyclic permutation">no</note>
	<note type="input" label="scoring, include reverse">no</note>

<note>output parameters</note>
	<note type="input" label="output, log path"></note>
	<note type="input" label="output, message">testing 1 2 3</note>
	<note type="input" label="output, one sequence copy">no</note>
	<note type="input" label="output, sequence path"></note>
	<note type="input" label="output, path">output.xml</note>
	<note type="input" label="output, sort results by">protein</note>
		<note>values = protein|spectrum (spectrum is the default)</note>
	<note type="input" label="output, path hashing">yes</note>
		<note>values = yes|no</note>
	<note type="input" label="output, xsl path">tandem-style.xsl</note>
	<note type="input" label="output, parameters">yes</note>
		<note>values = yes|no</note>
	<note type="input" label="output, performance">yes</note>
		<note>values = yes|no</note>
	<note type="input" label="output, spectra">yes</note>
		<note>values = yes|no</note>
	<note type="input" label="output, histograms">yes</note>
		<note>values = yes|no</note>
	<note type="input" label="output, proteins">yes</note>
		<note>values = yes|no</note>
	<note type="input" label="output, sequences">yes</note>
		<note>values = yes|no</note>
	<note type="input" label="output, one sequence copy">no</note>
		<note>values = yes|no, set to yes to produce only one copy of each protein sequence in the output xml</note>
	<note type="input" label="output, results">valid</note>
		<note>values = all|valid|stochastic</note>
	<note type="input" label="output, maximum valid expectation value">0.1</note>
		<note>value is used in the valid|stochastic setting of output, results</note>
	<note type="input" label="output, histogram column width">30</note>
		<note>values any integer greater than 0. Setting this to '1' makes cutting and pasting histograms
		into spread sheet programs easier.</note>
<note type="description">ADDITIONAL EXPLANATIONS</note>
	<note type="description">Each one of the parameters for X! tandem is entered as a labeled note
			node. In the current version of X!, keep those note nodes
			on a single line.
	</note>
	<note type="description">The presence of the type 'input' is necessary if a note is to be considered
			an input parameter.
	</note>
	<note type="description">Any of the parameters that are paths to files may require alteration for a 
			particular installation. Full path names usually cause the least trouble,
			but there is no reason not to use relative path names, if that is the
			most convenient.
	</note>
	<note type="description">Any parameter values set in the 'list path, default parameters' file are
			reset by entries in the normal input file, if they are present. Otherwise,
			the default set is used.
	</note>
	<note type="description">The 'list path, taxonomy information' file must exist.
		</note>
	<note type="description">The directory containing the 'output, path' file must exist: it will not be created.
		</note>
	<note type="description">The 'output, xsl path' is optional: it is only of use if a good XSLT style sheet exists.
		</note>

</bioml>

@@taxonomy.xml
<?xml version="1.0"?>
<bioml label="x! taxon-to-file matching list">
	<taxon label="home.sapien">
		<file format="peptide" URL="{DATABASE}" />
	</taxon>
</bioml>








